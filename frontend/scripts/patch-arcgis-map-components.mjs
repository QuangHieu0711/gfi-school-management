import fs from 'node:fs';
import path from 'node:path';

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

function writeText(filePath, content) {
  ensureDir(path.dirname(filePath));
  fs.writeFileSync(filePath, content, 'utf8');
}

function patchRuntimeImport({ runtimePath }) {
  const original = readText(runtimePath);
  const patched = original.replace(/^import\s+["']\.\.\/cdn\/main\.css["'];\s*\r?\n/m, '');

  if (patched === original) {
    return { changed: false };
  }

  writeText(runtimePath, patched);
  return { changed: true };
}

function prepareCss({ sourceCssPath, outCssPath }) {
  const original = readText(sourceCssPath);

  // The upstream CSS expects assets next to itself at ./assets/**.
  // We serve it from /assets/arcgis/main.css and copy the upstream assets folder to /assets/arcgis/**,
  // so rewrite url(./assets/...) -> url(./...)
  const patched = original.replace(/url\(\.\/assets\//g, 'url(./');

  writeText(outCssPath, patched);
  return { changed: patched !== original };
}

function main() {
  const projectRoot = process.cwd();
  const packageRoot = path.join(projectRoot, 'node_modules', '@arcgis', 'map-components');

  const runtimePath = path.join(packageRoot, 'dist', 'chunks', 'runtime.js');
  const sourceCssPath = path.join(packageRoot, 'dist', 'cdn', 'main.css');
  const outCssPath = path.join(projectRoot, 'public', 'assets', 'arcgis', 'main.css');

  if (!fs.existsSync(runtimePath)) {
    throw new Error(`Missing file: ${runtimePath}`);
  }
  if (!fs.existsSync(sourceCssPath)) {
    throw new Error(`Missing file: ${sourceCssPath}`);
  }

  const runtimeResult = patchRuntimeImport({ runtimePath });
  const cssResult = prepareCss({ sourceCssPath, outCssPath });

  const summary = [
    runtimeResult.changed ? 'patched runtime.js (removed main.css import)' : 'runtime.js already patched',
    cssResult.changed ? 'wrote patched main.css to public/assets/arcgis/main.css' : 'wrote main.css (no changes needed)',
  ].join('; ');
}

main();
