import { Pipe, PipeTransform } from '@angular/core';
import { format } from 'date-fns';

@Pipe({
  name: 'formatDate',
  standalone: true,
})
/**
 * A pipe that formats a Date object into 'DD/MM/YYYY'.
 * - Returns an empty string if the input is null or undefined.
 */
export class FormatDatePipe implements PipeTransform {
  // Formats a Date object into 'DD/MM/YYYY' format
  transform(date: Date): string {
    if (date) return format(date, 'DD/MM/YYYY');
    return '';
  }
}
