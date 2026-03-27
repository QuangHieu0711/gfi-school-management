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

import { StateBCDCReducer, StateBCDCState } from '@store/state-bcdc';
import { StateQTTNReducer, StateQTTNState } from '@store/state-qttn';
import { StateLoKhoanReducer, StateLoKhoanState } from '@store/state-tlnt';
import { StateDAPAReducer, StateDAPAState } from '@store/state-dapa';

export interface State {
  navigator: TreeNode[];
  userInfo: ICurrentUser;

  stateBCDC: StateBCDCState;
  stateQTTN: StateQTTNState;
  stateLoKhoan: StateLoKhoanState;
  stateDAPAState: StateDAPAState;
}

export const reducers: ActionReducerMap<State> = {
  navigator: NavigatorReducer,
  userInfo: UserInfoReducer,

  stateBCDC: StateBCDCReducer,
  stateQTTN: StateQTTNReducer,
  stateLoKhoan: StateLoKhoanReducer,
  stateDAPAState: StateDAPAReducer,
};
