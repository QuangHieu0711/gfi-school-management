import { ComponentType, ProgressAnimationType } from 'ngx-toastr';

export enum MessageSeverity {
  SUCCESS = 'SUCCESS',
  WARNING = 'WARNING',
  ERROR = 'ERROR',
  INFO = 'INFO',
}

export interface IToastrConfig {
  /**
   * disable both timeOut and extendedTimeOut
   * default: false
   */
  disableTimeOut?: boolean | 'timeOut' | 'extendedTimeOut';
  /**
   * toast time to live in milliseconds
   * default: 5000
   */
  timeOut?: number;
  /**
   * toast show close button
   * default: false
   */
  closeButton?: boolean;
  /**
   * time to close after a user hovers over toast
   * default: 1000
   */
  extendedTimeOut?: number;
  /**
   * show toast progress bar
   * default: false
   */
  progressBar?: boolean;
  /**
   * changes toast progress bar animation
   * default: decreasing
   */
  progressAnimation?: ProgressAnimationType;
  /**
   * render html in toast message (possibly unsafe)
   * default: false
   */
  enableHtml?: boolean;
  /**
   * css class on toast component
   * default: ngx-toastr
   */
  toastClass?: string;
  /**
   * css class on toast container
   * default: toast-top-right
   */
  positionClass?: string;
  /**
   * css class on toast title
   * default: toast-title
   */
  titleClass?: string;
  /**
   * css class on toast message
   * default: toast-message
   */
  messageClass?: string;
  /**
   * animation easing on toast
   * default: ease-in
   */
  easing?: string;
  /**
   * animation ease time on toast
   * default: 300
   */
  easeTime?: string | number;
  /**
   * clicking on toast dismisses it
   * default: true
   */
  tapToDismiss?: boolean;
  /**
   * Angular toast component to be shown
   * default: Toast
   */
  toastComponent?: ComponentType<unknown>;
  /**
   * Helps show toast from a websocket or from event outside Angular
   * default: false
   */
  onActivateTick?: boolean;
  /**
   * New toast placement
   * default: true
   */
  newestOnTop?: boolean;
}
