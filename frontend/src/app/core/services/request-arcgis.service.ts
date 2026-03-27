import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { combineLatest, Observable, of, Subject, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

// Module of ARCGIS CORE
import esriConfig from '@arcgis/core/config';
import esriRequest from '@arcgis/core/request';

export interface DeleteResponse {
  success: boolean;
  data: Array<any>;
  message: string;
}

export interface UpdateResponse {
  success: boolean;
  message: string;
  features?: any;
}

export interface InsertResponse {
  features: any;
  total: number;
  success: boolean;
  message: string;
}

export interface SearchResponse {
  features: Array<any>;
  total: number;
  success: boolean;
  message: string;
}

export interface QdataParams {
  url: string;
  params: any; // params cần gửi, có dạng object, ví dụ { name: 'mr.a', age: 41, sex: 'male'}
  proxy?: string; // đường dẫn của proxy, ví dụ: https://coretech:1313. chỉ có proxy khi khác domain, nếu không chỉ định proxy thì lấy mặc định từ appservice
  method?: 'GET' | 'POST' | 'DELETE';
  type?: string;
  contentType?: string; // content type trong header
  responseType?: string; // kiểu dữ liệu trả về, bao gồm: 'arraybuffer' | 'blob' | 'json' | 'text'
  token?: any;
}

export interface OdataParams {
  url: string;
  select?: Array<string>;
  where?: any; // Quy ước: where = [] => lấy toàn bộ dữ liệu, where = null => không tìm dữ liệu
  logic?: string;
  orderBy?: Array<string>;
  groupBy?: Array<string>; // tạm thời chưa dùng
  pageSize?: number;
  startRecord?: number;
  data?: any;
  primaryKey?: string;
  and?: any; // cấu trúc giống where
  or?: any; // cấu trúc giống where
  geometry?: any; // Đồ họa thêm vào trong bảng
  distinct?: boolean; // Có query distinct hay không (true = có)
  returnGeometry?: boolean; // Có trả về geometry hay không
  spatialRelationship?: string; // kiểu relationship của geometry
  token?: string;
  notCount?: boolean;
  inSR?: any; // for ArcGIS service
  outSR?: any; // for ArcGIS Service
}

export interface IdParams {
  url: string;
  id: any;
}

// Service arcgis sử dụng api 4.x
@Injectable({
  providedIn: 'root',
})
export class RequestArcGISService {
  urlProxy: string | nullish = null;

  constructor() {
    setTimeout(() => {
      this.testDecode();
    }, 5000);
  }

  /** type: 'search' || 'insert' || 'update' || 'delete' */
  private esriRequest(
    url: string,
    params: any = {},
    type: string = ''
  ): Observable<any> {
    const subject: Subject<any> = new Subject();
    let newUrl = url;
    let method: any = 'post'; // mặc định phương thức gọi là post

    switch (type) {
      case 'search':
        newUrl += '/query';
        method = 'auto';
        break;
      case 'insert':
        newUrl += '/addFeatures';
        break;
      case 'update':
        newUrl += '/updateFeatures';
        method = 'post';
        break;
      case 'delete':
        newUrl += '/deleteFeatures';
        break;
      default:
        break;
    }

    if (this.urlProxy) {
      esriConfig.request.proxyUrl = this.urlProxy;
    }
    esriRequest(newUrl, {
      query: params,
      responseType: 'json',
      useProxy: true,
      method,
    }).then(
      (response) => subject.next(response),
      (error) => subject.next(error)
    );

    return subject.asObservable();
  }

  queryCustom(q: QdataParams): Observable<any> {
    const url = q.url;
    const _params = {
      f: 'json',
      returnGeometry: true,
      spatialRel: 'esriSpatialRelEnvelopeIntersects',
    };

    return this.esriRequest(url, _params, 'search');
  }

  search(p: OdataParams) {
    const where = this.decodeSql(p.where);
    const outFields = p.select ? [p.select] : ['*'];
    const _params: any = {
      f: 'json',
      outFields,
      returnGeometry: true,
      spatialRel: 'esriSpatialRelEnvelopeIntersects',
      where,
    };

    if (p.geometry) {
      _params['geometry'] = JSON.stringify(p.geometry);
    }

    const _paramsTotal = {
      ..._params,
      returnCountOnly: true,
    };

    // Nếu truyền pageNumber và pageSize thì => lazyload, còn không sẽ load toàn bộ
    if (p.startRecord) {
      _params['resultOffset'] = p.startRecord;
    }
    if (p.pageSize) {
      _params['resultRecordCount'] = p.pageSize;
    }

    const list = [];
    const url = `${p.url}`;

    list.push(this.esriRequest(url, _params, 'search'));
    list.push(this.esriRequest(url, _paramsTotal, 'search'));
    return combineLatest(list).pipe(
      map((res: any) => {
        const result: SearchResponse = {
          features: [],
          total: 0,
          success: true,
          message: 'Search Success',
        };

        if (res[0].message === 'Timeout exceeded') {
          result.success = false;
          result.message = 'Request timeout';
        }

        if (res[0] && res[0].data.features) {
          const geometryType = res[0].data.geometryType;
          const wkid = res[0].data.spatialReference ?? 102100;

          res[0].data.features.forEach((item: any) => {
            if (item.geometry) {
              item.geometry.spatialReference = wkid;
              item.geometry.geometryType = geometryType;
              item.attributes.__geometry = item.geometry;
            }

            result.features.push(item.attributes);
          });

          result.total = res[1] ? res[1].data.count : 0;
        } else {
          result.success = false;
          result.message = 'Request error';
        }
        return result;
      }),
      catchError(this.handleSearchError)
    );
  }

  private handleSearchError(error: HttpErrorResponse) {
    const rq: SearchResponse = {
      features: [],
      total: 0,
      success: false,
      message: 'Search error!',
    };
    return throwError(rq);
  }

  insert(p: OdataParams): Observable<any> {
    const url = `${p.url}`;
    const features: any = {};
    features['attributes'] = p.data;
    if (p.geometry) {
      features['geometry'] = p.geometry;
    }
    const paramsToSend = {
      f: 'json',
      features: JSON.stringify([features]),
    };

    return this.esriRequest(url, paramsToSend, 'insert').pipe(
      map((res) => {
        const result: InsertResponse = {
          features: [],
          total: 0,
          success: true,
          message: 'Insert Success',
        };
        if (res.message === 'Timeout exceeded') {
          result.success = false;
          result.message = 'Request timeout';
        }

        if (res) {
          result.features = res.addResults;
          result.total = res.addResults.length;
        }
        return result;
      }),
      catchError(this.handleInsertError)
    );
  }

  private handleInsertError(error: any) {
    const rq: InsertResponse = {
      features: [],
      total: 0,
      success: false,
      message: error?.message,
    };
    return throwError(rq);
  }

  update(p: OdataParams) {
    const url = `${p.url}`;

    const features: any = {};
    features['attributes'] = p.data;
    if (p.geometry) {
      features['geometry'] = p.geometry;
    }
    const paramsToSend = {
      f: 'json',
      features: JSON.stringify([features]),
    };
    return this.esriRequest(url, paramsToSend, 'update').pipe(
      map((res) => {
        if (res.name === 'Error') {
          return {
            success: false,
            message: res.message,
          };
        }
        const result: UpdateResponse = {
          success: true,
          message: '',
        };

        return result;
      }),
      catchError(this.handleUpdateError)
    );
  }

  private handleUpdateError(error: any) {
    const rq: UpdateResponse = {
      success: false,
      message: error.message,
    };
    return throwError(rq);
  }

  delete(p: OdataParams): Observable<any> {
    const url = `${p.url}`;

    return this.deteleItSelf(url, p.data, p?.primaryKey).pipe(
      map((res) => {
        const response: DeleteResponse = {
          data: res.data ? res.data.deleteResults : [],
          success: res.error ? false : true,
          message: 'Xóa thành công',
        };
        return response;
      })
    );
  }

  /** Xóa bản ghi ở trong bảng chính */
  private deteleItSelf(url: string, data: any, primaryKey: string | nullish) {
    const params = {
      f: 'json',
      objectIds: primaryKey ? data[primaryKey] : null,
    };
    return this.esriRequest(url, params, 'delete');
  }

  private decodeSql(whereClause: any[]) {
    let where = '';
    let logic = 'and';
    if (whereClause === null || whereClause === undefined) {
      return '';
    }

    if (whereClause.length === 0) {
      return '1=1';
    }
    const cloneWhere = JSON.parse(JSON.stringify(whereClause));
    if (cloneWhere.length > 0) {
      if (
        !Array.isArray(cloneWhere[0]) &&
        cloneWhere[0] !== 'and' &&
        cloneWhere[0] !== 'or'
      ) {
        if (
          typeof cloneWhere[2] === 'string' &&
          cloneWhere[2].startsWith('c$')
        ) {
          where += `${cloneWhere[0]} ${cloneWhere[1]} ${eval(cloneWhere[2])}`;
        } else {
          if (cloneWhere[1] === 'like') {
            where += `${cloneWhere[0]} LIKE n'%${cloneWhere[2]}%'`;
          } else if (cloneWhere[1] === 'in') {
            where += `${cloneWhere[0]} ${cloneWhere[1]} (${cloneWhere[2]})`;
          } else {
            if (typeof cloneWhere[2] === 'string') {
              where += `${cloneWhere[0]} ${cloneWhere[1]} '${cloneWhere[2]}''`;
            } else {
              where += `${cloneWhere[0]} ${cloneWhere[1]} ${cloneWhere[2]}`;
            }
          }
        }
      } else {
        cloneWhere.forEach((item: any, index: any) => {
          if (index === 0 && (item === 'and' || item === 'or')) {
            logic = item;
          } else {
            if (
              Array.isArray(item) &&
              (item[0] === 'and' || item[0] === 'or')
            ) {
              where += `(${this.decodeSql(item)}) ${logic} `;
            } else {
              if (typeof item[2] === 'string' && item[2].startsWith('c$')) {
                where += `${item[0]} ${item[1]} ${eval(item[2])} ${logic}`;
              } else {
                if (item[1] === 'like') {
                  where += `${item[0]} LIKE n'%${item[2]}%' ${logic} `;
                } else if (item[1] === 'in') {
                  where += `${item[0]} ${item[1]} (${item[2]}) ${logic} `;
                } else {
                  if (typeof item[2] === 'string') {
                    where += `${item[0]} ${item[1]} '${item[2]}' ${logic} `;
                  } else {
                    where += `${item[0]} ${item[1]} ${item[2]} ${logic} `;
                  }
                }
              }
            }
          }
        });
        const number = logic === 'and' ? 5 : 4;
        where = where.substring(0, where.length - number);
      }
    }

    return where;
  }

  private testDecode() {
    const where = ['table', 'in', [1, 2]];
    const where1 = ['or', ['table', 'in', [1, 2]], ['data', 'like', '1234']];
    const where2 = [
      'or',
      ['and', ['table', '=', 1], ['tableName', 'like', 'super']],
      ['data', 'like', '1234'],
    ];
    const whereCurrentUser = ['username', '=', '$userid'];
  }
}
