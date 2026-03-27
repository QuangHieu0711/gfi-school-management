/* eslint-disable @typescript-eslint/consistent-type-definitions */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, of, switchMap } from 'rxjs';
import {
  loadBCDCMenu,
  loadBCDCMenuFailure,
  loadBCDCMenuSuccess,
} from './actions';
import { BcdcReportLite } from './state';
import { BaoCaoDiaChatService } from '@app/service/bao-cao-dia-chat/bao-cao-dia-chat.service';

type MenuNode = {
  id?: string;
  type?: string;
  reviewStatus?: number;
  children?: MenuNode[];
};

function extractBCDCItems(regions?: MenuNode[]): BcdcReportLite[] {
  const out: BcdcReportLite[] = [];
  const seen = new Set<string>();

  const walk = (nodes?: MenuNode[]) => {
    if (!nodes?.length) return;

    for (const n of nodes) {
      if (
        n?.type === 'report' &&
        typeof n.id === 'string' &&
        n.id.startsWith('BCDC')
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
export class StateBCDCEffects {
  private readonly actions$ = inject(Actions);
  private readonly service = inject(BaoCaoDiaChatService);

  loadMenu$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loadBCDCMenu),
      switchMap(({ unitId }) =>
        this.service.getDynamicMenu(unitId).pipe(
          map((res: any) => {
            // robust hơn để tránh lệch shape response
            const regions =
              res?.data?.regions ??
              res?.data?.data?.regions ??
              res?.regions ??
              [];

            const items = extractBCDCItems(regions);

            return loadBCDCMenuSuccess({ items });
          }),
          catchError((error) => {
            console.error('[BCDC effect] error', error);
            return of(loadBCDCMenuFailure({ error }));
          })
        )
      )
    )
  );
}
