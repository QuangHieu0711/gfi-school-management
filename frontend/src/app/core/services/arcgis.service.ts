import { computed, EventEmitter, Injectable, signal } from '@angular/core';
import { EsriMapConfig } from '../../features/map/esri-map/map/map';
import Map from '@arcgis/core/Map';
import MapView from '@arcgis/core/views/MapView';
import Extent from "@arcgis/core/geometry/Extent";
import { GraphicUtil } from '../classes/graphic-util';
import { SymbolDefault } from '../classes/symbol';
import { ToolConfig } from '../../features/map/esri-map/basic-tools/basic-tools';
import { DrawSketch } from '../classes/draw';
import IdentityManager from '@arcgis/core/identity/IdentityManager';
import esriConfig from '@arcgis/core/config';

export const FieldTypeNumber = [
  "small-integer",
  "integer",
  "big-integer",
  "single",
  "double",
  "long",
  "oid"
];

export const FieldTypeString = [
  "string",
  "guid",
  "global-id",
  "xml"
];

export const FieldTypeDate = [
  "date",
  "date-only",
  "time-only",
  "timestamp-offset"
]

export const FieldTypeOther = [
  "geometry",
  "blob",
  "raster"
]

@Injectable({
  providedIn: 'root',
})
export class ArcgisService {

  private _esriMap?: Map;
  get esriMap(): Map | undefined {
    return this._esriMap;
  }

  set esriMap(map: Map | undefined) {
    this._esriMap = map;
    // if (map) {
    //   this.graphicUtil = new GraphicUtil(map);
    //   this.symbol = new SymbolDefault();
    // }
  }

  private _esriMapView?: MapView;
  get esriMapView(): MapView | undefined {
    return this._esriMapView;
  }

  set esriMapView(view: MapView | undefined) {
    this._esriMapView = view;
    if (view?.map) {
      this.graphicUtil = new GraphicUtil(view?.map);
      this.symbol = new SymbolDefault();
      this.draw = new DrawSketch(view);
    }
  }
  urlProxy = '';

  validUrlIdentify: string[] = [];
  bookmarkInit = signal(false);
  editFeatureLayers = signal<any[]>([]);

  graphicUtil?: GraphicUtil;
  symbol?: SymbolDefault;
  draw?: DrawSketch;

  private flashGraphic = signal<__esri.Graphic | nullish>(null);

  homeExtent = new Extent({
    xmin: 11771397.598321695,
    ymin: 2388539.391863944,
    xmax: 11789245.644426256,
    ymax: 2400377.5766195925,
    spatialReference: {
      wkid: 3857
    }
  });

  bsToolEmit: EventEmitter<ToolConfig> = new EventEmitter();
  mapLoading = signal(false);

  /** Tổng bản ghi hiện tại trong FeatureTable (nếu có ngữ cảnh như search-features) */
  tablePanelTotal = signal<number | null>(null);


  /** Số đối tượng được chọn sau truy vấn (editor); null = không hiển thị */
  selectedFeaturesCount = signal<number | null>(null);

  /** Hiển thị panel bảng dữ liệu (TableList) phía dưới bản đồ */
  tablePanelVisible = signal(false);
  /** Layer/sublayer được chọn để xem bảng (nếu cần dùng sau này) */
  tablePanelLayer = signal<__esri.Layer | __esri.Sublayer | null>(null);

  private readonly TOKEN_KEY = 'arcgis_token';
  private readonly TOKEN_EXPIRES_KEY = 'arcgis_token_expires';
  private readonly PORTAL_ROOT_KEY = 'arcgis_portal_root';
  private readonly SERVER_ROOT_KEY = 'arcgis_server_root';
  private readonly CREDENTIALS_KEY = 'arcgis_credentials';

  /**
   * Đăng nhập Portal bằng username/password. Token + hạn lưu sessionStorage để F5 kiểm tra, không lưu user/pass.
   */
  public async loginWithUserPass(portalUrl: string, username: string, password: string): Promise<void> {
    await this.registerArcGISDomain(username, password, portalUrl);
    // Lưu credentials (base64 encoded) để tự động đăng nhập lại khi token hết hạn
    this.saveCredentials(portalUrl, username, password);
  }

  /**
   * Lưu credentials (base64 encoded) vào sessionStorage
   */
  private saveCredentials(portalUrl: string, username: string, password: string): void {
    try {
      const cred = btoa(`${username}|${password}|${portalUrl}`);
      sessionStorage.setItem(this.CREDENTIALS_KEY, cred);
    } catch {
      // ignore
    }
  }

  /**
   * Khôi phục credentials từ sessionStorage
   */
  private getCredentials(): { username: string; password: string; portalUrl: string } | null {
    try {
      const cred = sessionStorage.getItem(this.CREDENTIALS_KEY);
      if (!cred) return null;
      const [username, password, portalUrl] = atob(cred).split('|');
      return { username, password, portalUrl };
    } catch {
      return null;
    }
  }

  /**
   * Tự động đăng nhập lại nếu token hết hạn hoặc không có
   */
  public async autoLoginIfNeeded(portalUrl: string): Promise<boolean> {
    // Nếu token còn hạn, không cần đăng nhập lại
    if (this.tryRestoreToken(portalUrl)) {
      return true;
    }

    // Thử khôi phục credentials và đăng nhập lại
    const cred = this.getCredentials();
    if (!cred) {
      return false;
    }

    try {
      await this.loginWithUserPass(cred.portalUrl, cred.username, cred.password);
      return true;
    } catch (err) {
      console.warn('Auto-login failed', err);
      return false;
    }
  }

  /**
   * Kiểm tra token đã lưu: nếu còn hạn thì đăng ký lại với IdentityManager và trả về true (bỏ qua form login).
   * Nếu hết hạn hoặc không có token thì trả về false.
   */
  public tryRestoreToken(portalUrl: string): boolean {
    const portalRoot = portalUrl.replace(/\/+$/, '');
    const storedPortal = sessionStorage.getItem(this.PORTAL_ROOT_KEY);
    if (storedPortal !== portalRoot) {
      return false;
    }
    const token = sessionStorage.getItem(this.TOKEN_KEY);
    const expiresStr = sessionStorage.getItem(this.TOKEN_EXPIRES_KEY);
    const serverRoot = sessionStorage.getItem(this.SERVER_ROOT_KEY);
    if (!token || !expiresStr || !serverRoot) {
      return false;
    }
    const expiresAt = Number(expiresStr);
    // Còn hạn (dự phòng 60 giây trước khi hết hạn)
    if (expiresAt <= Date.now() + 60_000) {
      this.clearStoredToken();
      return false;
    }
    IdentityManager.registerToken({
      server: portalRoot,
      token,
      expires: expiresAt
    });
    IdentityManager.registerToken({
      server: serverRoot,
      token,
      expires: expiresAt
    });
    esriConfig.request.interceptors?.push({
      urls: serverRoot,
      before: (params) => {
        const opts: any = params.requestOptions || {};
        opts.query = {
          ...(opts.query || {}),
          token
        };

        const q: any = opts.query;
        const webMapJson = q?.Web_Map_as_JSON;
        if (typeof webMapJson === 'string') {
          try {
            const def = JSON.parse(webMapJson);
            this.appendTokenToWebMapUrls(def, serverRoot, token);
            q.Web_Map_as_JSON = JSON.stringify(def);
          } catch {
            // ignore parse errors
          }
        }

        params.requestOptions = opts;
      }
    });
    esriConfig.portalUrl = portalRoot;
    return true;
  }

  private clearStoredToken(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.TOKEN_EXPIRES_KEY);
    sessionStorage.removeItem(this.PORTAL_ROOT_KEY);
    sessionStorage.removeItem(this.SERVER_ROOT_KEY);
  }

  /**
   * Đăng xuất khỏi Portal: xóa token đã lưu và credentials trong IdentityManager.
   */
  public logout(): void {
    try {
      IdentityManager.destroyCredentials();
    } catch {
      // ignore
    }
    this.clearStoredToken();
  }

  /**
   * Thiết lập portalUrl cho ứng dụng sau khi đã đăng nhập.
   * Map component gọi khi init; credential đã có từ loginWithUserPass.
   */
  public registerArcGISService(config: EsriMapConfig): void {
    const portalUrl = config.portalUrl.replace(/\/+$/, '');
    esriConfig.portalUrl = portalUrl;
  }

  /** Hàm đăng ký sử dụng các dịch vụ riêng tư của ArcGIS, quản lý bởi Identity Manager */
  private async registerArcGISDomain(
    username: string,
    password: string,
    portalUrl: string
  ): Promise<void> {

    const portalRoot = portalUrl.replace(/\/+$/, ""); // bỏ dấu /
    const portalSharing = `${portalRoot}/sharing/rest/generateToken`;
    const serverRoot = portalRoot.replace(/\/portal$/, "/server");

    const body = new URLSearchParams({
      username,
      password,
      client: "referer",
      referer: location.origin,
      expiration: "240", // phút
      f: "json"
    });

    const response = await fetch(portalSharing, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded"
      },
      body
    });

    const result = await response.json();

    if (!result.token || !result.expires) {
      const message = result?.error?.message || "GenerateToken failed";
      throw new Error(message);
    }

    const expiresAt = result.expires;

    // Lưu token + hạn vào sessionStorage để F5 kiểm tra (không lưu user/pass)
    sessionStorage.setItem(this.TOKEN_KEY, result.token);
    sessionStorage.setItem(this.TOKEN_EXPIRES_KEY, String(expiresAt));
    sessionStorage.setItem(this.PORTAL_ROOT_KEY, portalRoot);
    sessionStorage.setItem(this.SERVER_ROOT_KEY, serverRoot);

    IdentityManager.registerToken({
      server: portalRoot,
      token: result.token,
      expires: expiresAt
    });

    IdentityManager.registerToken({
      server: serverRoot,
      token: result.token,
      expires: expiresAt
    });

    esriConfig.request.interceptors?.push({
      urls: serverRoot,
      before: (params) => {
        const opts: any = params.requestOptions || {};
        opts.query = {
          ...(opts.query || {}),
          token: result.token
        };

        const q: any = opts.query;
        const webMapJson = q?.Web_Map_as_JSON;
        if (typeof webMapJson === 'string') {
          try {
            const def = JSON.parse(webMapJson);
            this.appendTokenToWebMapUrls(def, serverRoot, result.token);
            q.Web_Map_as_JSON = JSON.stringify(def);
          } catch {
            // ignore parse errors
          }
        }

        params.requestOptions = opts;
      }
    });

    esriConfig.portalUrl = portalRoot;
  }

  private timer: any = null;
  public flashGeometry(
    geometry: __esri.Geometry,
    blinkCount = 4,
    duration = 3000
  ) {

    this.clearPreviousFlash();

    let symbol: any = this.symbol?.flashPointSymbol;

    if (geometry.type === 'polyline') {
      symbol = this.symbol?.flashLineSymbol;
    } else if (geometry.type === 'polygon') {
      symbol = this.symbol?.flashPolygonSymbol;
    }

    // 2. Tạo graphic highlight
    const graphic = this.graphicUtil?.create(geometry, symbol);
    if (!graphic) {
      return;
    }
    this.flashGraphic.set(graphic);
    // 3. Blink
    const interval = duration / (blinkCount * 2);
    let visible = false;
    let count = 0;

    this.timer = setInterval(() => {
      if (visible) {
        this.graphicUtil?.remove(graphic);
      } else {
        this.graphicUtil?.add(graphic);
      }

      visible = !visible;
      count++;

      if (count >= blinkCount * 2) {
        clearInterval(this.timer);
        this.graphicUtil?.remove(graphic);
      }
    }, interval);
  }

  private clearPreviousFlash() {
    let graphic = this.flashGraphic();
    if (!graphic) {
      return;
    }
    if (this.timer) {
      clearInterval(this.timer);
      this.graphicUtil?.remove(graphic);
      this.flashGraphic.set(null);
    }
  }

  /**
   * Gắn thêm token vào các URL dịch vụ trong Web_Map_as_JSON (baseMap + operationalLayers)
   * để Print GPServer có thể truy cập được các dịch vụ private.
   */
  private appendTokenToWebMapUrls(def: any, serverRoot: string, token: string) {
    if (!def || !serverRoot || !token) {
      return;
    }

    const addToken = (url: string): string => {
      if (!url || typeof url !== 'string') {
        return url;
      }
      if (!url.startsWith(serverRoot)) {
        return url;
      }
      if (url.includes('token=')) {
        return url;
      }
      const sep = url.includes('?') ? '&' : '?';
      return `${url}${sep}token=${encodeURIComponent(token)}`;
    };

    // baseMap layers
    if (def.baseMap && Array.isArray(def.baseMap.baseMapLayers)) {
      def.baseMap.baseMapLayers.forEach((ly: any) => {
        if (ly && typeof ly.url === 'string') {
          ly.url = addToken(ly.url);
        }
      });
    }

    // operational layers
    if (Array.isArray(def.operationalLayers)) {
      def.operationalLayers.forEach((ly: any) => {
        if (!ly) return;
        if (typeof ly.url === 'string') {
          ly.url = addToken(ly.url);
        }
        if (Array.isArray(ly.layers)) {
          ly.layers.forEach((sub: any) => {
            if (sub && typeof sub.url === 'string') {
              sub.url = addToken(sub.url);
            }
          });
        }
      });
    }
  }
}
