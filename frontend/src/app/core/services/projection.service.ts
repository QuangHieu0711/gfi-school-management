import { effect, Injectable, signal } from '@angular/core';
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";

@Injectable({
    providedIn: 'root',
})
export class ProjectionService {
    private initialized = signal(false);
    private initEffect = effect(async () => {
        if (this.initialized()) return;

        this.initialized.set(true);
        await projectOperator.load();
    }, { allowSignalWrites: true });

    transform(geometry: any, outSPWkid: number) {
        let val = null;
        try {
            let sp = new SpatialReference({ wkid: outSPWkid });
            val = projectOperator.execute(geometry, sp) as __esri.Extent;
        } catch (error) {
            // console.error(error);
        }
        return val;

    }
}
