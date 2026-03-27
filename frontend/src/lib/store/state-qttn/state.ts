export interface QttnReportLite {
  id: string;
  reviewStatus: number | null;
}

export interface StateQTTNState {
  items: QttnReportLite[];
  loading: boolean;
  error: unknown | null;
}

export const initialStateQTTN: StateQTTNState = {
  items: [],
  loading: false,
  error: null,
};
