import { EndpointTree } from '@model/menu.model';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

const memo = new Map<string, string>();

/**
 * Resolves a path from a nested EndpointTree.
 * Always strict. Throws if any segment is missing or final value is not a string.
 * @param path Array of keys to resolve (e.g., ['DESIGN_SYSTEM', 'THEME', 'COLOR'])
 * @returns Resolved string path
 */
export const resolvePath = (path: readonly string[]): string => {
  // 1. Guard: path must be non-empty
  if (!Array.isArray(path) || path.length === 0) throw new Error('resolvePath: Path must be a non-empty string array.');

  const key = path.join('.');

  // 2. Memoization check
  if (memo.has(key)) return memo.get(key)!;

  // 3. Traverse with guard-first pattern
  let current: string | EndpointTree = NAVIGATOR_ENDPOINT;
  const segments: string[] = ['/'];
  for (const segment of path) {
    if (typeof current !== 'object' || current === null || !(segment in current))
      throw new Error(`resolvePath: Segment "${segment}" not found in path "${key}".`);

    current = current[segment];

    if (typeof current === 'object' && 'BASE_PATH' in current && typeof current['BASE_PATH'] === 'string')
      segments.push(current['BASE_PATH']);
  }

  // 4. Final guard: At the end of traversal, if current is a string, it is the leaf path (e.g., 'color')
  if (typeof current === 'string') segments.push(current);

  // 5. Cache and return
  const resolved = segments.filter(Boolean).join('/');
  memo.set(key, resolved);

  return resolved;
};

/**
 * Navigates to a URL resolved from a dot-path array using the EndpointTree.
 * @param path The path array to resolve, e.g., ['DESIGN_SYSTEM', 'THEME', 'COLOR']
 * @param extras Optional router navigation extras
 */
export const navigateTo = (path: readonly string[], extras?: Parameters<Router['navigate']>[1]): void => {
  const router = inject(Router);
  router.navigate([resolvePath(path)], extras);
};

/**
 * Creates a `UrlTree` from a resolved path array.
 * @param path The path array to resolve into a route, e.g., ['USER', 'LIST']
 * @param extras Optional extras for `createUrlTree`
 * @returns UrlTree object representing the resolved route
 */
export const createResolvedUrl = (path: readonly string[], extras?: Parameters<Router['createUrlTree']>[1]) => {
  const router = inject(Router);
  return router.createUrlTree([resolvePath(path)], extras);
};
