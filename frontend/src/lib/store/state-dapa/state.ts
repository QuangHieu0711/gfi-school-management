export interface DapaReportLite {
  id: string;
  reviewStatus: number | null;
}

export interface StateDAPAState {
  items: DapaReportLite[];
  loading: boolean;
  error: unknown | null;
}

export const initialStateDAPAState: StateDAPAState = {
  items: [],
  loading: false,
  error: null,
};
