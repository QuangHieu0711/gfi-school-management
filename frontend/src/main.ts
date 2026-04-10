import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

import 'bootstrap';

// import { setAssetPath } from '@arcgis/map-components';
// import esriConfig from '@arcgis/core/config';

// import { defineCustomElements } from '@esri/calcite-components/dist/loader';

const base = document.querySelector('base')?.getAttribute('href') ?? '/';

/**
 * ArcGIS map-components CSS - Disabled, not needed
 */
// const arcgisStyles = document.createElement('link');
// arcgisStyles.rel = 'stylesheet';
// document.head.appendChild(arcgisStyles);

/**
 * Map-components assets path - Disabled
 */
// setAssetPath(`${base}assets/arcgis`);

/**
 * ArcGIS JS SDK assets path - Disabled
 */
// esriConfig.assetsPath = `${base}assets/esri`;

/**
 * Calcite components - Disabled
 */
// defineCustomElements(window);

/**
 * Bootstrap Angular
 */
bootstrapApplication(AppComponent, appConfig).catch((err) =>
  console.error(err)
);
