/* eslint-disable @typescript-eslint/consistent-type-definitions */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import {
  loadQTTNMenu,
  loadQTTNMenuFailure,
  loadQTTNMenuSuccess,
} from './actions';
import { QttnReportLite } from './state';
import { BaoCaoQuanTriTaiNguyenService } from '@app/service/quan-tri-tai-nguyen/bao-cao-quan-tri-tai-nguyen.service';

type MenuNode = {
  id?: string;
  type?: string;
  reviewStatus?: any;
  children?: MenuNode[];
};

type ResourceManagementLite = {
  id?: string;
  reviewStatus?: any;
};

function extractQTTNItems(regions?: MenuNode[]): QttnReportLite[] {
  const out: QttnReportLite[] = [];
  const seen = new Set<string>();

  const walk = (nodes?: MenuNode[]) => {
    if (!nodes?.length) return;
    for (const n of nodes) {
      if (
        n?.type === 'report' &&
        typeof n.id === 'string' &&
        n.id.startsWith('QTTN')
      ) {
        if (!seen.has(n.id)) {
          seen.add(n.id);
          out.push({ id: n.id, reviewStatus: n.reviewStatus ?? null });
        }
      }
      if (n?.children?.length) walk(n.children);
    }
  };

  walk(regions);
  return out;
}

function extractQTTNFromResourceManagements(
  rms?: ResourceManagementLite[]
): QttnReportLite[] {
  const out: QttnReportLite[] = [];
  const seen = new Set<string>();

  if (!Array.isArray(rms)) return out;

  for (const r of rms) {
    const id = r?.id;
    if (typeof id !== 'string' || !id.startsWith('QTTN')) continue;

    if (!seen.has(id)) {
      seen.add(id);

      const rs = r?.reviewStatus;
      out.push({
        id,
        reviewStatus:
          rs === null || rs === undefined
            ? null
            : typeof rs === 'number'
              ? rs
              : Number.isNaN(Number(rs))
                ? null
                : Number(rs),
      });
    }
  }

  return out;
}

@Injectable()
export class StateQTTNEffects {
  private readonly actions$ = inject(Actions);
  private readonly service = inject(BaoCaoQuanTriTaiNguyenService);

  loadMenu$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loadQTTNMenu),
      distinctUntilChanged((prev, curr) => prev.unitId === curr.unitId),
      switchMap(({ unitId }) =>
        this.service.getMenu(unitId).pipe(
          map((res: any) => {
            const raw = res?.data ?? res;
            let serverResponse = raw;

            if (
              serverResponse?.id === undefined &&
              raw?.data?.status !== undefined
            ) {
              serverResponse = raw.data;
            }

            if (serverResponse?.id === false) {
              return loadQTTNMenuFailure({ error: serverResponse });
            }

            const payload =
              serverResponse?.id !== undefined
                ? (serverResponse?.data ?? {})
                : (raw ?? {});

            const resourceManagements = payload?.resourceManagements ?? [];
            const regions = payload?.regions ?? [];

            const items =
              Array.isArray(resourceManagements) && resourceManagements.length
                ? extractQTTNFromResourceManagements(resourceManagements)
                : extractQTTNItems(regions);
            return loadQTTNMenuSuccess({ items });
          }),
          catchError((error) => of(loadQTTNMenuFailure({ error })))
        )
      )
    )
  );
}
