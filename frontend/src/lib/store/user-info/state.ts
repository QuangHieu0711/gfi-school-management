import { ICurrentUser } from '@model/auth.model';

export const INITIAL_STATE: ICurrentUser = {
  id: '',
  username: '',
  fullName: '',
  email: '',
  phone: '',
  status: 0,
  role: {
    id: '',
    code: '',
    name: '',
    rules: [],
  },
  unit: {
    id: '',
    code: '',
    name: '',
  },
  permissions: {
    menus: [],
  },
};
