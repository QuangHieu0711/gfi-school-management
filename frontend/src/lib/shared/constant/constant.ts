/**
 * This file is part of the Chromia project.
 * It contains constants used throughout the application.
 * Do not modify this file unless you know what you are doing.
 */

import { TableConfig } from '@model/table.model';

// Shared
export const DEFAULT_TIMEOUT = 5000;

// localStorage keys
export const ACCESS_TOKEN_KEY = 'accessToken';
export const REFRESH_TOKEN_KEY = 'refreshToken';
export const AUTH_CHANNEL = 'auth_channel';

// Language settings
export const LANGUAGE_KEY = 'language';
export const FALLBACK_LANGUAGE = 'vi';
export const SUPPORT_LANGUAGE = ['vi'];

// Regular expressions
export const REGEX = {
  HTML_TAG_VALIDATE: /<\/?([a-zA-Z][a-zA-Z0-9]*)\b[^>]*?>/g,
  VIETNAMESE_NAME:
    /^[a-zA-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴÝỶỸàáâãèéêìíòóôõùúăđĩũơưạảấầẩẫậắằẳẵặẹẻẽềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵýỷỹ\s]+$/g,
  // Matches emails that end with @gmail.com (case-insensitive)
  EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/i,
};

// Pagination constants
export const PAGINATION_MAX_PAGE = 7;
export const PAGINATION_DELTA = 2;
export const PAGINATION_DEFAULT_PAGE_SIZE = 10;

// Form types
export enum TYPE_FORM {
  CREATE = 'create',
  UPDATE = 'update',
  DETAIL = 'detail',
}
export type TYPE_FORM_KEY = 'create' | 'update' | 'detail';

// Toast
export const MAX_OPENED_TOASTS = 7;

// Form
export const DEFAULT_TEXTAREA_ROWS = 3;

// File upload
export const MAX_FILE_SIZE = 5000000; // 5 MB
export const MAX_TOTAL_SIZE = 15000000; // 15 MB
export const MAX_QUANTITY = 15000000; // 15 MB

// Table
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
  cellMerge: false,
};

// Account options
export const ACCOUNT_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Kích hoạt' },
  { value: 'INACTIVE', label: 'Chưa kích hoạt' },
  { value: 'LOCKED', label: 'Khóa' },
  { value: 'DISABLED', label: 'Vô hiệu hóa' },
];

export const ROLE_OPTIONS = [
  { value: 1, label: 'Quản trị viên' },
  { value: 2, label: 'Trưởng phòng' },
  { value: 3, label: 'Giáo viên' },
  { value: 4, label: 'Nhân viên' },
];
