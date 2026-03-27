import { createReducer, on } from '@ngrx/store';
import { INITIAL_STATE } from './state';
import * as UserInfoActions from './actions';

export const UserInfoReducer = createReducer(
  INITIAL_STATE,
  on(UserInfoActions.Update, (state, { newState }) => newState)
);
