import { MatDialogConfig } from '@angular/material/dialog';
import { LanguageTranslator } from '@model/common.model';
import { DialogData } from '@model/dialog.model';

/**
 * Dialog constants
 */
export const DIALOG_DEFAULT_WIDTH = '500px';
export const DIALOG_SEMANTIC_WIDTH = '600px';

/**
 * Get default confirm dialog data
 */
export const DIALOG_DEFAULT_CONFIRM_CONFIG = (payload: DialogData, t: LanguageTranslator): MatDialogConfig => {
  return {
    width: DIALOG_SEMANTIC_WIDTH,
    data: {
      title: t('dialog.confirm.title'),
      message: t('dialog.confirm.message'),
      cancelButtonText: t('dialog.confirm.cancel'),
      confirmButtonText: t('dialog.confirm.confirm'),
      ...payload,
    },
  };
};

/**
 * Get default success dialog data
 */
export const DIALOG_DEFAULT_SUCCESS_CONFIG = (payload: DialogData, t: LanguageTranslator): MatDialogConfig => {
  return {
    width: DIALOG_SEMANTIC_WIDTH,
    data: {
      title: t('dialog.success.title'),
      message: t('dialog.success.message'),
      closeButtonText: t('dialog.success.close'),
      ...payload,
    },
  };
};

/**
 * Get default error dialog data
 */
export const DIALOG_DEFAULT_ERROR_CONFIG = (payload: DialogData, t: LanguageTranslator): MatDialogConfig => {
  return {
    width: DIALOG_SEMANTIC_WIDTH,
    data: {
      title: t('dialog.error.title'),
      message: t('dialog.error.message'),
      closeButtonText: t('dialog.error.close'),
      ...payload,
    },
  };
};
