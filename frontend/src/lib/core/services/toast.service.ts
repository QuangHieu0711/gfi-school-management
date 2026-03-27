import { Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { IToastrConfig, MessageSeverity } from '@model/toast.model';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  clear() {
    throw new Error('Method not implemented.');
  }
  constructor(private readonly toastr: ToastrService) {}

  DEFAULT_TIMEOUT = 5000;
  preventDuplicates = false;

  getToastrConfig(option?: IToastrConfig) {
    return {
      progressBar: option?.progressBar ?? true,
      positionClass: option?.positionClass ?? 'toast-top-right',
      timeOut: option?.timeOut ?? this.DEFAULT_TIMEOUT,
      extendedTimeOut: option?.extendedTimeOut ?? this.DEFAULT_TIMEOUT,
      closeButton: option?.closeButton ?? true,
    };
  }

  showToastr(summary: string, detail: string, severity: MessageSeverity, option?: IToastrConfig) {
    const config = this.getToastrConfig(option);
    switch (severity) {
      case MessageSeverity.SUCCESS:
        this.toastr.success(summary, detail, config);
        break;
      case MessageSeverity.WARNING:
        this.toastr.warning(summary, detail, config);
        break;
      case MessageSeverity.ERROR:
        this.toastr.error(summary, detail, config);
        break;
      default:
        this.toastr.info(summary, detail, config);
        break;
    }
  }

  removeToastr() {
    this.toastr.clear();
  }

  success(summary: string, detail: string, option?: IToastrConfig) {
    this.showToastr(summary, detail, MessageSeverity.SUCCESS, option);
  }

  warning(summary: string, detail: string, option?: IToastrConfig) {
    this.showToastr(summary, detail, MessageSeverity.WARNING, option);
  }

  info(summary: string, detail: string, option?: IToastrConfig) {
    this.showToastr(summary, detail, MessageSeverity.INFO, option);
  }

  error(summary: string, detail: string, option?: IToastrConfig) {
    this.showToastr(summary, detail, MessageSeverity.ERROR, option);
  }
}
