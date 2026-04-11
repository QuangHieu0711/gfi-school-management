import { Component, Input } from '@angular/core';
import { HeaderRows } from '@model/table.model';

@Component({
  selector: 'app-table-header',
  templateUrl: './app-table-header.component.html',
  standalone: true,
})
export class AppTableHeaderComponent {
  @Input() headerRows: HeaderRows[] = [];
}
