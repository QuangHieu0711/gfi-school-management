export interface ISidebarItem {
  key: string;
  label: string;
  path?: readonly string[];
  url?: string;
  icon?: string;
  children?: ISidebarItem[];
}
export interface EndpointTree {
  [key: string]: string | ({ BASE_PATH?: string } & EndpointTree);
}
