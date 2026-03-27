import { createFeatureSelector, createSelector } from '@ngrx/store';
import { StateDAPAFeatureKey } from './reducer';
import { StateDAPAState } from './state';

export const selectStateDAPAState =
  createFeatureSelector<StateDAPAState>(StateDAPAFeatureKey);

export const selectDAPAItems = createSelector(
  selectStateDAPAState,
  (s) => s.items
);

export const selectDAPALoading = createSelector(
  selectStateDAPAState,
  (s) => s.loading
);

export const selectDAPAError = createSelector(
  selectStateDAPAState,
  (s) => s.error
);

export const selectDAPAItemById = (id: string) =>
  createSelector(
    selectDAPAItems,
    (items) => items.find((x) => x.id === id) ?? null
  );

export const selectDAPAReviewStatusById = (id: string) =>
  createSelector(selectDAPAItemById(id), (item) => item?.reviewStatus ?? null);
