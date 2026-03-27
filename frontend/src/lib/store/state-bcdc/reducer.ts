import { createReducer, on } from '@ngrx/store';
import {
  clearBCDC,
  loadBCDCMenu,
  loadBCDCMenuFailure,
  loadBCDCMenuSuccess,
  updateBCDCReviewStatus,
} from './actions';
import { initialStateBCDC, StateBCDCState } from './state';

export const StateBCDCFeatureKey = 'stateBCDC';

export const StateBCDCReducer = createReducer<StateBCDCState>(
  initialStateBCDC,

  on(loadBCDCMenu, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(loadBCDCMenuSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    error: null,
  })),

  on(loadBCDCMenuFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  on(clearBCDC, () => initialStateBCDC),

  on(updateBCDCReviewStatus, (state, { id, reviewStatus }) => ({
    ...state,
    items: state.items.map((item) =>
      item.id === id ? { ...item, reviewStatus } : item
    ),
  }))
);
