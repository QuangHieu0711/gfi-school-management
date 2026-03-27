import { createAction, props } from '@ngrx/store';
import { BcdcReportLite } from './state';

export const loadBCDCMenu = createAction(
  '[BCDC] Load Menu',
  props<{ unitId: string }>()
);

export const loadBCDCMenuSuccess = createAction(
  '[BCDC] Load Menu Success',
  props<{ items: BcdcReportLite[] }>()
);

export const loadBCDCMenuFailure = createAction(
  '[BCDC] Load Menu Failure',
  props<{ error: unknown }>()
);

export const updateBCDCReviewStatus = createAction(
  '[BCDC] Update Review Status',
  props<{ id: string; reviewStatus: number }>()
);
export const clearBCDC = createAction('[BCDC] Clear');
