import { createReducer, on } from '@ngrx/store';
import {
  clearDAPA,
  loadDAPAMenu,
  loadDAPAMenuFailure,
  loadDAPAMenuSuccess,
  updateDAPAReviewStatus,
} from './actions';
import { initialStateDAPAState, StateDAPAState } from './state';

export const StateDAPAFeatureKey = 'stateDAPAState';

export const StateDAPAReducer = createReducer<StateDAPAState>(
  initialStateDAPAState,

  on(loadDAPAMenu, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(loadDAPAMenuSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    error: null,
  })),

  on(loadDAPAMenuFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  on(clearDAPA, () => initialStateDAPAState),

  on(updateDAPAReviewStatus, (state, { id, reviewStatus }) => ({
    ...state,
    items: state.items.map((item) =>
      item.id === id ? { ...item, reviewStatus } : item
    ),
  }))
);
