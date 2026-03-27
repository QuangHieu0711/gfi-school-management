import { createAction, props } from '@ngrx/store';
import { TreeNode } from '@model/tree.models';

export const Update = createAction('[Navigator] Update', props<{ newState: TreeNode[] }>());
