import { toSignal } from '@angular/core/rxjs-interop';
import { Component } from '@angular/core';
import { LoadingService } from '@service';

@Component({
  selector: 'app-loading-indicator',
  templateUrl: './loading-indicator.component.html',
  styleUrls: ['./loading-indicator.component.scss'],
})
export class LoadingIndicatorComponent {
  loading$: ReturnType<typeof toSignal>;

  constructor(public loadingService: LoadingService) {
    this.loading$ = toSignal(this.loadingService.loading$, { initialValue: false });
  }
}
