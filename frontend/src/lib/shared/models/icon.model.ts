// Central source of truth for icon types and prefixes
export const ICONS = {
  SVG: { type: 'svg', prefix: 'svg:' },
  MAT: { type: 'mat', prefix: 'mat:' },
} as const;

// Export derived maps for convenience
export const ICON_PREFIXES = Object.fromEntries(Object.entries(ICONS).map(([k, v]) => [k, v.prefix])) as {
  [K in keyof typeof ICONS]: string;
};

export const ICONS_TYPE = Object.fromEntries(Object.entries(ICONS).map(([k, v]) => [k, v.type])) as { [K in keyof typeof ICONS]: IconType };

// Union type of all possible icon types
export type IconType = (typeof ICONS)[keyof typeof ICONS]['type'];
