import {
  ComponentRef,
  Directive,
  ElementRef,
  Input,
  OnChanges,
  Renderer2,
  SimpleChanges,
  ViewContainerRef,
  booleanAttribute,
  inject,
} from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Directive({
  selector: `[mat-button][loading], [matButton][loading],
             [mat-raised-button][loading],
             [mat-stroked-button][loading],
             [mat-flat-button][loading],
             [mat-icon-button][loading], [matIconButton][loading],
             [mat-fab][loading], [matFab][loading],
             [mat-mini-fab][loading], [matMiniFab][loading]`,
})
export class ButtonLoadingDirective implements OnChanges {
  private readonly _elementRef = inject<ElementRef<HTMLButtonElement>>(ElementRef);
  private readonly _viewContainerRef = inject(ViewContainerRef);
  private readonly _renderer = inject(Renderer2);

  private spinner: ComponentRef<MatProgressSpinner> | null = null;

  @Input({ transform: booleanAttribute }) loading = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['loading']) return;

    const button = this._elementRef.nativeElement;

    if (changes['loading'].currentValue) {
      button.classList.add('button-loading');
      button.setAttribute('loading', 'true');
      button.setAttribute('disabled', 'true');

      this.createSpinner();
    } else if (!changes['loading'].firstChange) {
      button.classList.remove('button-loading');
      button.removeAttribute('loading');
      button.removeAttribute('disabled');
      this.destroySpinner();
    }
  }

  private createSpinner(): void {
    if (!this.spinner) {
      this.spinner = this._viewContainerRef.createComponent(MatProgressSpinner);
      this.spinner.instance.diameter = 24;
      this.spinner.instance.mode = 'indeterminate';
      this._renderer.appendChild(this._elementRef.nativeElement, this.spinner.instance._elementRef.nativeElement);
    }
  }

  private destroySpinner(): void {
    if (this.spinner) {
      this.spinner.destroy();
      this.spinner = null;
    }
  }
}
