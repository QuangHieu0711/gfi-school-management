import { createAction, props } from '@ngrx/store';
import { LoKhoanReviewLite } from './state';

/** Batch lưu reviewStatus từ danh sách lỗ khoan */
export const loadLoKhoanListSuccess = createAction(
  '[LoKhoan] Load List Success',
  props<{ items: LoKhoanReviewLite[] }>()
);

export const updateLoKhoanReviewStatus = createAction(
  '[LoKhoan] Update Review Status',
  props<{ id: string; reviewStatus: number }>()
);

export const clearLoKhoan = createAction('[LoKhoan] Clear');
