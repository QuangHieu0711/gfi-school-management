export interface LoKhoanReviewLite {
  id: string;
  reviewStatus: number | null;
}

export interface StateLoKhoanState {
  items: LoKhoanReviewLite[];
  loading: boolean;
  error: unknown | null;
}

export const initialStateLoKhoan: StateLoKhoanState = {
  items: [],
  loading: false,
  error: null,
};
