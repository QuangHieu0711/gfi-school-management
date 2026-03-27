export interface TreeNode {
  id?: string | number;
  parentId?: string | number | null;
  name?: string;
  children?: TreeNode[];
  [key: string]: unknown;
}
