export interface Rule {
  ruleId: number;
  roleId: number;
  moduleId: number;
  pid?: number | null;
  name: string;
  url: string;
  icon: string;
  ordinal: number;
  isView: number;
}

export interface Role {
  id: number;
  name: string;
  rules: Rule[];
}

export interface UserData {
  id: number;
  username: string;
  role: Role;
}

export interface ApiResponse {
  code: number;
  message: string;
  data: UserData;
}

export interface MenuItem {
  key: string;
  name: string;
  icon?: string;
  url?: string;
  children?: MenuItem[];
  expanded?: boolean;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: any;
}
