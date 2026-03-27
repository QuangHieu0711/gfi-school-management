import { createReducer, on } from '@ngrx/store';
import {
  clearLoKhoan,
  loadLoKhoanListSuccess,
  updateLoKhoanReviewStatus,
} from './actions';
import { initialStateLoKhoan, StateLoKhoanState } from './state';

export const StateLoKhoanFeatureKey = 'stateLoKhoan';

export const StateLoKhoanReducer = createReducer<StateLoKhoanState>(
  initialStateLoKhoan,

  // Batch upsert từ danh sách lỗ khoan
  on(loadLoKhoanListSuccess, (state, { items }) => {
    const map = new Map(state.items.map((i) => [i.id, i]));
    for (const item of items) {
      map.set(item.id, item);
    }
    return { ...state, items: Array.from(map.values()) };
  }),

  on(updateLoKhoanReviewStatus, (state, { id, reviewStatus }) => {
    const exists = state.items.some((item) => item.id === id);
    return {
      ...state,
      items: exists
        ? state.items.map((item) =>
            item.id === id ? { ...item, reviewStatus } : item
          )
        : [...state.items, { id, reviewStatus }],
    };
  }),

  on(clearLoKhoan, () => initialStateLoKhoan)
);
