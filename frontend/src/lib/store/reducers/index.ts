// import { ActionReducerMap } from '@ngrx/store';
// import { TreeNode } from '@model/tree.models';
// import { ICurrentUser } from '@model/auth.model';
// import { NavigatorReducer } from '@store/navigator';
// import { UserInfoReducer } from '@store/user-info';

// export interface State {
//   navigator: TreeNode[];
//   userInfo: ICurrentUser;
// }

// export const reducers: ActionReducerMap<State> = { navigator: NavigatorReducer, userInfo: UserInfoReducer };

import { ActionReducerMap } from '@ngrx/store';
import { TreeNode } from '@model/tree.models';
import { ICurrentUser } from '@model/auth.model';
import { NavigatorReducer } from '@store/navigator';
import { UserInfoReducer } from '@store/user-info';

export interface State {
  navigator: TreeNode[];
  userInfo: ICurrentUser;
}

export const reducers: ActionReducerMap<State> = {
  navigator: NavigatorReducer,
  userInfo: UserInfoReducer,
};
