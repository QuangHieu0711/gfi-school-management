import { Injectable } from '@angular/core';
import { from, Observable, switchMap } from 'rxjs';

export interface DiaChiTinhThanhResponse {
  requestId?: string;
  provinces?: DiaChiTinhThanhItem[];
}

export interface DiaChiTinhThanhItem {
  code: string;
  name: string;
  type?: string;
  administrativeLevel?: string;
}

export interface DiaChiPhuongXaItem {
  code: string;
  name: string;
  type?: string;
  province_code?: string;
  provinceCode?: string;
  administrativeLevel?: string;
}

export interface DiaChiPhuongXaResponse {
  requestId?: string;
  communes?: DiaChiPhuongXaItem[];
}

@Injectable({ providedIn: 'root' })
export class DiaChiHanhChinhService {
  private readonly baseUrl = '/address-kit';
  private readonly effectiveDate = '2025-07-01';

  getProvinces(
    effectiveDate = this.effectiveDate
  ): Observable<DiaChiTinhThanhResponse> {
    return this.getJson<DiaChiTinhThanhResponse>(
      `${this.baseUrl}/${effectiveDate}/provinces`
    );
  }

  getCommunesByProvince(
    provinceCode: string,
    effectiveDate = this.effectiveDate
  ): Observable<DiaChiPhuongXaResponse> {
    return this.getJson<DiaChiPhuongXaResponse>(
      `${this.baseUrl}/${effectiveDate}/provinces/${provinceCode}/communes`
    );
  }

  private getJson<T>(url: string): Observable<T> {
    return from(
      fetch(url, {
        method: 'GET',
        headers: {
          Accept: 'application/json, text/plain, */*',
        },
        mode: 'cors',
        credentials: 'omit',
      })
    ).pipe(
      switchMap(async (response) => {
        const text = await response.text();
        if (!response.ok) {
          throw new Error(
            `Address kit request failed: ${response.status} ${response.statusText}`
          );
        }
        return this.parseJson<T>(text);
      })
    );
  }

  private parseJson<T>(response: string): T {
    const text = `${response ?? ''}`.trim().replace(/^\uFEFF/, '');
    return JSON.parse(text) as T;
  }
}
