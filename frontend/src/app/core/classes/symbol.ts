
import PictureMarkerSymbol from "@arcgis/core/symbols/PictureMarkerSymbol.js";
import SimpleMarkerSymbol from "@arcgis/core/symbols/SimpleMarkerSymbol.js";
import SimpleLineSymbol from "@arcgis/core/symbols/SimpleLineSymbol.js";
import SimpleFillSymbol from "@arcgis/core/symbols/SimpleFillSymbol.js";
import TextSymbol from "@arcgis/core/symbols/TextSymbol.js";
import Color from "@arcgis/core/Color.js";


export class SymbolDefault {
    // Symbol cho các điểm, đường, vùng.
    currentLocation = new PictureMarkerSymbol({
        url: 'web_pin_current.png',
        height: 24,
        width: 24,
        xoffset: 0,
        yoffset: 12
    });

    flashPointSymbol = new SimpleMarkerSymbol({
        angle: 0,
        color: new Color([47, 172, 244, 1]),
        outline: new SimpleLineSymbol({
            cap: "round",
            color: new Color([43, 173, 238, 1]),
            join: "round",
            miterLimit: 1,
            style: "solid",
            width: 1
        }),
        path: "undefined",
        size: 12,
        style: "circle",
        xoffset: 0,
        yoffset: 0
    });

    flashLineSymbol = {
        type: "simple-line",
        color: [47, 172, 244, 1],
        width: 4
    };

    flashPolygonSymbol = {
        type: "simple-fill",
        color: [255, 255, 0, 0.25],
        outline: {
            color: [47, 172, 244, 1],
            width: 2
        }
    };
}
