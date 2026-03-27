export type StorageType = 'all' | 'local' | 'session';
type ValueType =
  | 'json-string'
  | 'json-number'
  | 'json-boolean'
  | 'array'
  | 'object'
  | 'null'
  | 'raw-string'
  | 'invalid';
export interface ParsedValue {
  type: ValueType;
  parsed: unknown;
}
