import { createReducer, on } from '@ngrx/store';
import { INITIAL_STATE } from './state';
import * as StyleActions from './actions';

export const StyleReducer = createReducer(
  INITIAL_STATE,
  on(StyleActions.Update, (state, { newState }) => newState)
);
