import { ICurrentUser } from '@model/auth.model';

export const INITIAL_STATE: ICurrentUser = {
  id: '',
  username: '',
  name: '',
  email: '',
  role: {
    id: '',
    name: '',
    rules: [],
  },
  donVi: {
    maDonVi: '',
    tenDonVi: '',
    tenVietTat: '',
    role: [],
  },
};
