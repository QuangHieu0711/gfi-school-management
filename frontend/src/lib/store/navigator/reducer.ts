import { createReducer, on } from '@ngrx/store';
import { INITIAL_STATE } from './state';
import * as NavigatorActions from './actions';

export const NavigatorReducer = createReducer(
  INITIAL_STATE,
  on(NavigatorActions.Update, (state, { newState }) => newState)
);
