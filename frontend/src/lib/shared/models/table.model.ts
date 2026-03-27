import { ID_TYPE } from '@model/response.model';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

export interface TableDataSource {
  id: ID_TYPE;
  [key: string]: unknown;
}
export interface TableQueryEvent {
  sortField?: string;
  sortDirection?: 'asc' | 'desc' | '';
  pageSize?: number;
  pageIndex?: number;
}
export interface TableConfig {
  hasImport?: boolean;
  hasExport?: boolean;
  hasCreate?: boolean;
  hasDelete?: boolean;
  hasFilterPanel?: boolean;
  hasFilterPanelButton?: boolean;
  columnResizable?: boolean;
  pageSizeOptions?: number[];
  sortDisabled?: boolean;
  rowSelectable?: boolean;
  multiSelectable?: boolean;
  showToolbar?: boolean;
  columnHideable?: boolean;
  columnSortable?: boolean;
  columnPinnable?: boolean;
  showPaginator?: boolean;
}

export const DEFAULT_TABLE_CONFIG: Required<TableConfig> = {
  hasImport: false,
  hasExport: false,
  hasCreate: false,
  hasDelete: false,
  hasFilterPanel: false,
  hasFilterPanelButton: false,
  columnResizable: false,
  pageSizeOptions: [10, 20, 50],
  sortDisabled: false,
  rowSelectable: false,
  multiSelectable: false,
  showToolbar: false,
  columnHideable: false,
  columnSortable: false,
  columnPinnable: false,
  showPaginator: true,
};

export enum COMMON_TABLE_KEY {
  STT = 'rowIndex',
  ACTION = 'actions',
}

export interface TableRequest {
  pageNo?: number;
  pageSize?: number;
  [key: string]: unknown;
}

export interface HeaderCell {
  title: string;
  colspan?: number;
  rowspan?: number;
  slotField?: string;
}

/** An ordered list of header cells that together form a single header row. */
export type HeaderRows = HeaderCell[];

/** Extended column definition for the table */
export interface TableColumns extends MtxGridColumn {
  rowspan?: number;
}
