export interface BcdcReportLite {
  id: string;
  reviewStatus: number | null;
}

export interface StateBCDCState {
  items: BcdcReportLite[];
  loading: boolean;
  error: unknown | null;
}

export const initialStateBCDC: StateBCDCState = {
  items: [],
  loading: false,
  error: null,
};
