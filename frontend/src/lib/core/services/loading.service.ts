import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/** A service to manage global loading state in the application.
 * - Provides methods to show and hide the loading indicator.
 * - Uses a BehaviorSubject to track the loading state.
 * Components can subscribe to the `loading$` observable to react to loading state changes.
 */
@Injectable({
  providedIn: 'root',
})
export class LoadingService {
  private readonly _loading = new BehaviorSubject<boolean>(false);
  public readonly loading$ = this._loading.asObservable();

  /**
   * Shows the loading indicator by setting the loading state to true.
   */
  show() {
    this._loading.next(true);
  }

  /**
   * Hides the loading indicator by setting the loading state to false.
   */
  hide() {
    this._loading.next(false);
  }
}
