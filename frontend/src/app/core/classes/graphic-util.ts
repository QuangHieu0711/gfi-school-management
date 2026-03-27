
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";
import Point from "@arcgis/core/geometry/Point.js";

import GraphicsLayer from "@arcgis/core/layers/GraphicsLayer.js";
import Graphic from "@arcgis/core/Graphic.js";
import Map from "@arcgis/core/Map";


export class GraphicUtil {
    /** Graphic layer name. */
    private graphicLayerName = 'graphicLayerCore';

    private graphicLayer: GraphicsLayer;


    constructor(
        private map: Map
    ) {
        const graphicLayer = new GraphicsLayer({ id: this.graphicLayerName, listMode: 'hide' });
        this.map.add(graphicLayer);
        this.graphicLayer = graphicLayer;
    }

    /** Đưa graphicLayer lên top */
    ensureTopLayer() {
        if (!this.map.layers.includes(this.graphicLayer)) {
            this.map.add(this.graphicLayer);
        } else {
            this.map.layers.reorder(this.graphicLayer, this.map.layers.length - 1);
        }
    }

    create(geometry: any, symbol?: any, attribute?: any, infoTemplate?: any) {
        return new Graphic({
            geometry,
            symbol,
            attributes: attribute,
            popupTemplate: infoTemplate
        });
    }

    createPoint(x: number, y: number, sp: number) {
        return new Point({
            x,
            y,
            spatialReference: new SpatialReference({ wkid: sp })
        });
    }

    createGraphicLayer(graphicLayerName: string, index?: any) {
        let graphicLayer: GraphicsLayer = (this.map.findLayerById(graphicLayerName) as GraphicsLayer);
        if (!graphicLayer) {
            const obj: any = { id: graphicLayerName }
            if (index) {
                obj['index'] = index;
            }
            graphicLayer = new GraphicsLayer(obj);
            this.map.add(graphicLayer);
        }
        return graphicLayer;
    }

    /**
     * Adds a graphic.
     * @param graphic esri/graphic class.
     * @param graphicLayerName Name of graphic layer.
     */
    add(graphic: any, graphicLayerName: string = this.graphicLayerName): any {
        const graphicLayer = this.createGraphicLayer(graphicLayerName);
        return graphicLayer ? graphicLayer.add(graphic) : null;
    }

    /**
     * Removes a graphic.
     * @param graphic esri/graphic class.
     * @param graphicLayerName Name of graphic layer.
     */
    remove(graphic: Graphic, graphicLayerName: string = this.graphicLayerName) {
        const graphicLayer: GraphicsLayer = (this.map.findLayerById(graphicLayerName) as GraphicsLayer);
        if (graphicLayer != null) {
            graphicLayer.remove(graphic);
        }
    }

    /**
     * Clears all graphics in `graphicLayerCore` layer.
     * @param graphicLayerName Name of graphic layer.
     */
    clear(graphicLayerName: string = this.graphicLayerName) {
        const graphicLayer: GraphicsLayer = (this.map.findLayerById(graphicLayerName) as GraphicsLayer);
        if (graphicLayer !== undefined) {
            graphicLayer.removeAll();
        }
    }
}
