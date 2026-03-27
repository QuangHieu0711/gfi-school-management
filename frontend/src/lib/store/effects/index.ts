import { UserInfoEffect } from '@store/user-info/effect';
import { StateBCDCEffects } from '@store/state-bcdc';
import { StateQTTNEffects } from '@store/state-qttn';
import { StateDAPAEffects } from '@store/state-dapa';

export const effects = [
  UserInfoEffect,
  StateBCDCEffects,
  StateDAPAEffects,
  StateQTTNEffects,
];
