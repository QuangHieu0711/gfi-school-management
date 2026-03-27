import { createAction, props } from '@ngrx/store';
import { DapaReportLite } from './state';

export const loadDAPAMenu = createAction(
  '[DAPA] Load Menu',
  props<{ unitId: string }>()
);

export const loadDAPAMenuSuccess = createAction(
  '[DAPA] Load Menu Success',
  props<{ items: DapaReportLite[] }>()
);

export const loadDAPAMenuFailure = createAction(
  '[DAPA] Load Menu Failure',
  props<{ error: unknown }>()
);

export const updateDAPAReviewStatus = createAction(
  '[DAPA] Update Review Status',
  props<{ id: string; reviewStatus: number }>()
);

export const clearDAPA = createAction('[DAPA] Clear');
