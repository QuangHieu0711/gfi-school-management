
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";
import Point from "@arcgis/core/geometry/Point.js";

import Graphic from "@arcgis/core/Graphic.js";
import Map from "@arcgis/core/Map";
import SketchViewModel from "@arcgis/core/widgets/Sketch/SketchViewModel";
import GraphicsLayer from "@arcgis/core/layers/GraphicsLayer";
import MapView from "@arcgis/core/views/MapView";
import { EventEmitter } from "@angular/core";


export class DrawSketch {

    drawFinish: EventEmitter<any> = new EventEmitter();
    /** Emit khi hoàn thành chỉnh sửa geometry (update) một graphic */
    updateFinish: EventEmitter<__esri.Graphic> = new EventEmitter();

    private sketchVM?: SketchViewModel;
    private sketchLayer?: GraphicsLayer;

    constructor(
        private view: MapView
    ) {
        let sketchLayer: any = view.map?.allLayers.find((fil: any) => fil.id === 'SketchLayer' || fil.id === 'graphicLayerCore');
        if (!(sketchLayer instanceof GraphicsLayer)) {
            sketchLayer = new GraphicsLayer({
                id: 'SketchLayer',
                listMode: "hide"
            });
            view.map?.add(sketchLayer);
        }
        this.sketchLayer = sketchLayer;

        const sketchVM = new SketchViewModel({
            view,
            layer: sketchLayer,
            updateOnGraphicClick: false,
        });

        sketchVM.on("create", (event) => {
            if (event.state === 'start') {
                this.sketchVM?.removeAllGraphics();
            }
            if (event.state === "complete") {
                const graphicToEmit = this.resolveGraphicForComplete(event);
                this.sketchVM?.create(this._type);
                this.drawFinish.emit(graphicToEmit);
            }
        });

        sketchVM.on("update", (event: __esri.SketchViewModelUpdateEvent) => {
            if (event.state === "complete" && event.graphics?.length) {
                this.updateFinish.emit(event.graphics[0]);
            }
        });

        this.sketchVM = sketchVM;
    }

    /** Thêm graphic vào sketch layer và bắt đầu chế độ sửa geometry (kéo đỉnh, di chuyển...). Kết thúc thì emit updateFinish. */
    startUpdate(graphic: __esri.Graphic): void {
        if (!this.sketchLayer || !this.sketchVM) return;
        this.sketchLayer.add(graphic);
        this.sketchVM.update(graphic);
    }

    /**
     * Polygon: click đơn (1 đỉnh) => Point; kéo thả nhiều đỉnh => Polygon.
     * Rectangle/Circle: click đơn (không kéo / diện tích 0 / bán kính 0) => Point; kéo thả => Rectangle/Circle.
     */
    private resolveGraphicForComplete(event: __esri.SketchViewModelCreateEvent): __esri.Graphic {
        const graphic = event.graphic;
        if (!graphic?.geometry) return graphic;

        const geom = graphic.geometry;
        const type = this._type;

        if (type === "polygon") {
            const poly = geom as __esri.Polygon;
            if (poly.type !== "polygon" || !poly.rings?.length) return graphic;
            const ring = poly.rings[0];
            if (ring.length <= 2) {
                this.sketchVM?.removeAllGraphics();
                const coord = ring[0];
                return new Graphic({
                    geometry: new Point({
                        x: coord[0],
                        y: coord[1],
                        spatialReference: poly.spatialReference
                    })
                });
            }
            return graphic;
        }

        if (type === "rectangle") {
            const poly = geom as __esri.Polygon;
            const ext = poly.extent;
            if (!ext || ext.width <= 0 || ext.height <= 0) {
                this.sketchVM?.removeAllGraphics();
                const ring = poly.rings?.[0];
                const coord = ring?.[0] ?? [ext?.xmin ?? 0, ext?.ymin ?? 0];
                return new Graphic({
                    geometry: new Point({
                        x: coord[0],
                        y: coord[1],
                        spatialReference: poly.spatialReference
                    })
                });
            }
            return graphic;
        }

        if ((this._type as string) === "circle") {
            const circle = geom as __esri.Circle;
            if (!circle.center) return graphic;
            const radius = circle.radius ?? 0;
            if (radius <= 0) {
                this.sketchVM?.removeAllGraphics();
                return new Graphic({
                    geometry: new Point({
                        x: circle.center.x,
                        y: circle.center.y,
                        spatialReference: circle.spatialReference
                    })
                });
            }
            return graphic;
        }

        return graphic;
    }

    private _type: any = null;
    create(type: | "point"
        | "multipoint"
        | "polyline"
        | "polygon"
        | "rectangle"
        | "circle"
        | "mesh"
        | "freehandPolygon"
        | "freehandPolyline"
        | "text") {
        this._type = type;
        this.sketchVM?.create(type);
    }

    cancel() {
        this.sketchVM?.cancel();
    }

    clearAllGraphics() {
        this.sketchVM?.removeAllGraphics();
    }

  /** Hoàn thành thao tác vẽ/sửa hiện tại giống như double-click/Enter. */
  complete() {
      (this.sketchVM as any)?.complete?.();
  }
}
