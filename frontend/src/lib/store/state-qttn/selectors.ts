import { createFeatureSelector, createSelector } from '@ngrx/store';
import { StateQTTNFeatureKey } from './reducer';
import { StateQTTNState } from './state';

export const selectStateQTTN =
  createFeatureSelector<StateQTTNState>(StateQTTNFeatureKey);

export const selectQTTNItems = createSelector(selectStateQTTN, (s) => s.items);
export const selectQTTNLoading = createSelector(
  selectStateQTTN,
  (s) => s.loading
);
export const selectQTTNError = createSelector(selectStateQTTN, (s) => s.error);
export const selectQTTNItemById = (id: string) =>
  createSelector(
    selectQTTNItems,
    (items) => items.find((x) => x.id === id) ?? null
  );

export const selectQTTNReviewStatusById = (id: string) =>
  createSelector(selectQTTNItemById(id), (item) => item?.reviewStatus ?? null);
