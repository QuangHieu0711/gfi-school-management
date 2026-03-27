import { createFeatureSelector, createSelector } from '@ngrx/store';
import { StateLoKhoanFeatureKey } from './reducer';
import { StateLoKhoanState } from './state';

export const selectStateLoKhoan = createFeatureSelector<StateLoKhoanState>(
  StateLoKhoanFeatureKey
);

export const selectLoKhoanItems = createSelector(
  selectStateLoKhoan,
  (s) => s.items
);

export const selectLoKhoanItemById = (id: string) =>
  createSelector(
    selectLoKhoanItems,
    (items) => items.find((x) => x.id === id) ?? null
  );

export const selectLoKhoanReviewStatusById = (id: string) =>
  createSelector(
    selectLoKhoanItemById(id),
    (item) => item?.reviewStatus ?? null
  );
