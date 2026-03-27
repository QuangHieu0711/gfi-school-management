import { createFeatureSelector, createSelector } from '@ngrx/store';
import { StateBCDCFeatureKey } from './reducer';
import { StateBCDCState } from './state';

export const selectStateBCDC =
  createFeatureSelector<StateBCDCState>(StateBCDCFeatureKey);

export const selectBCDCItems = createSelector(selectStateBCDC, (s) => s.items);
export const selectBCDCLoading = createSelector(
  selectStateBCDC,
  (s) => s.loading
);
export const selectBCDCError = createSelector(selectStateBCDC, (s) => s.error);
export const selectBCDCItemById = (id: string) =>
  createSelector(
    selectBCDCItems,
    (items) => items.find((x) => x.id === id) ?? null
  );

export const selectBCDCReviewStatusById = (id: string) =>
  createSelector(selectBCDCItemById(id), (item) => item?.reviewStatus ?? null);
