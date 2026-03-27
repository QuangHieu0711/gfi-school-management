import { Pipe, PipeTransform } from '@angular/core';
import { format } from 'date-fns';

@Pipe({
  name: 'formatDateTime',
  standalone: true,
})
/**
 * A pipe that formats a Date object into 'DD/MM/YYYY HH:mm'.
 * - Returns an empty string if the input is null or undefined.
 */
export class FormatDateTimePipe implements PipeTransform {
  // Formats a Date object into 'DD/MM/YYYY HH:mm' format
  transform(date: Date): string {
    if (date) return format(date, 'DD/MM/YYYY HH:mm');
    return '';
  }
}
