/* eslint-disable @typescript-eslint/consistent-type-definitions */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, of, switchMap } from 'rxjs';
import {
  loadDAPAMenu,
  loadDAPAMenuFailure,
  loadDAPAMenuSuccess,
} from './actions';
import { DapaReportLite } from './state';
import { DeAnPhuongAnService } from '@app/service/de-an-phuong-an/de-an-phuong-an.service';

type MenuNode = {
  id?: string;
  type?: string;
  reviewStatus?: number;
  children?: MenuNode[];
};

function extractDAPAItems(regions?: MenuNode[]): DapaReportLite[] {
  const out: DapaReportLite[] = [];
  const seen = new Set<string>();

  const walk = (nodes?: MenuNode[]) => {
    if (!nodes?.length) return;

    for (const n of nodes) {
      if (
        n?.type === 'report' &&
        typeof n.id === 'string' &&
        n.id.startsWith('DAPA')
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

@Injectable()
export class StateDAPAEffects {
  private readonly actions$ = inject(Actions);
  private readonly service = inject(DeAnPhuongAnService);

  loadMenu$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loadDAPAMenu),
      switchMap(({ unitId }) =>
        this.service.getDynamicMenu(unitId).pipe(
          map((res: any) => {
            const regions =
              res?.data?.regions ??
              res?.data?.data?.regions ??
              res?.regions ??
              [];

            const items = extractDAPAItems(regions);

            return loadDAPAMenuSuccess({ items });
          }),
          catchError((error) => {
            console.error('[DAPA effect] error', error);
            return of(loadDAPAMenuFailure({ error }));
          })
        )
      )
    )
  );
}
