import { ICurrentUser } from '@model/auth.model';
import { createAction, props } from '@ngrx/store';

export const GetCurrentUser = createAction('[User Info] Get Current User');
export const Update = createAction(
  '[User Info] Update',
  props<{ newState: ICurrentUser }>()
);
