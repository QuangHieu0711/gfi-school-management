import { createReducer, on } from '@ngrx/store';
import {
  clearQTTN,
  loadQTTNMenu,
  loadQTTNMenuFailure,
  loadQTTNMenuSuccess,
  updateQTTNReviewStatus,
} from './actions';
import { initialStateQTTN, StateQTTNState } from './state';

export const StateQTTNFeatureKey = 'stateQTTN';

export const StateQTTNReducer = createReducer<StateQTTNState>(
  initialStateQTTN,

  on(loadQTTNMenu, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(loadQTTNMenuSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    error: null,
  })),

  on(loadQTTNMenuFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  on(clearQTTN, () => initialStateQTTN),

  on(updateQTTNReviewStatus, (state, { id, reviewStatus }) => ({
    ...state,
    items: state.items.map((item) =>
      item.id === id ? { ...item, reviewStatus } : item
    ),
  }))
);
