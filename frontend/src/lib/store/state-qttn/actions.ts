import { createAction, props } from '@ngrx/store';
import { QttnReportLite } from './state';

export const loadQTTNMenu = createAction(
  '[QTTN] Load Menu',
  props<{ unitId: string }>()
);

export const loadQTTNMenuSuccess = createAction(
  '[QTTN] Load Menu Success',
  props<{ items: QttnReportLite[] }>()
);

export const loadQTTNMenuFailure = createAction(
  '[QTTN] Load Menu Failure',
  props<{ error: unknown }>()
);

export const updateQTTNReviewStatus = createAction(
  '[QTTN] Update Review Status',
  props<{ id: string; reviewStatus: number }>()
);
export const clearQTTN = createAction('[QTTN] Clear');
