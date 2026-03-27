import { createAction, props } from '@ngrx/store';
import { IStyle } from './state';

export const GetStyle = createAction('[Style] Get Style');
export const Update = createAction(
  '[Style] Update',
  props<{ newState: IStyle }>()
);
