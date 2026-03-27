import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

import 'bootstrap';

import { setAssetPath } from '@arcgis/map-components';
import esriConfig from '@arcgis/core/config';

import { defineCustomElements } from '@esri/calcite-components/dist/loader';

const base = document.querySelector('base')?.getAttribute('href') ?? '/';

/**
 * ArcGIS map-components CSS
 */
const arcgisStyles = document.createElement('link');
arcgisStyles.rel = 'stylesheet';
arcgisStyles.href = `${base}assets/arcgis/main.css`;
document.head.appendChild(arcgisStyles);

/**
 * Map-components assets path
 */
setAssetPath(`${base}assets/arcgis`);

/**
 * ArcGIS JS SDK assets path
 */
esriConfig.assetsPath = `${base}assets/esri`;

/**
 * Calcite components
 */
defineCustomElements(window);

/**
 * Bootstrap Angular
 */
bootstrapApplication(AppComponent, appConfig).catch((err) =>
  console.error(err)
);
