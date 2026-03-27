import { createFeatureSelector, createSelector } from '@ngrx/store';
import { IStyle } from './state';

export const selectStyleState = createFeatureSelector<IStyle>('style');

export const selectMatBtn = createSelector(
  selectStyleState,
  (state: IStyle) => state.mat_btn
);
