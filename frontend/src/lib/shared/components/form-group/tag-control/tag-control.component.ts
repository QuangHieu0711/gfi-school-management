import { Component, signal, WritableSignal } from '@angular/core';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';
import { distinctUntilChanged } from 'rxjs';
import { IconComponent } from '@components/app-icon/app-icon.component';

@Component({
  selector: 'app-tag-control',
  templateUrl: './tag-control.component.html',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, ...MATERIAL_MODULE, IconComponent],
})
export class TagControlComponent extends FormGroupAbstractComponent {
  readonly tags: WritableSignal<string[]> = signal([]);
  readonly separatorKeysCodes = [ENTER, COMMA] as const;

  override ngAfterViewInit(): void {
    super.ngAfterViewInit();

    // ✅ Sync FormControl → Signal
    const control = this.getControl();
    if (!control) return;
    // ✅ Watch for external value changes (e.g., patchValue)
    control.valueChanges.pipe(distinctUntilChanged()).subscribe((val: string[] | null) => {
      this.tags.set(val ?? []);
    });

    // ✅ Set initial value once (for safety)
    this.tags.set(control.value ?? []);
  }

  removeTag(tag: string) {
    this.tags.update((tags) => {
      const index = tags.indexOf(tag);
      if (index < 0) return tags;

      tags.splice(index, 1);
      return [...tags];
    });
    this.setValue();
  }

  addTag(event: MatChipInputEvent): void {
    const value = (event.value ?? '').trim();

    // Add our tag
    if (value) this.tags.update((tags) => [...tags, value]);

    this.setValue();

    // Clear the input value
    event.chipInput.clear();
  }

  setValue() {
    this.setControlValue(this.tags());
    this.emitValueChanged(this.tags());
  }
}
