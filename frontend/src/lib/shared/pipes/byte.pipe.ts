import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'byte',
  standalone: true,
})
/**
 * A pipe that converts a byte value into a human-readable format.
 * - Supports units from Bytes to YB.
 * - Allows optional decimal precision (default is 0).
 */
export class BytePipe implements PipeTransform {
  // Transforms a number or numeric string into a human-readable byte format (e.g., "1.5 MB")
  transform(value: string | number, decimals: number | string = 0): string {
    value = value.toString();

    // Only transform non-negative numeric values
    if (parseInt(value, 10) >= 0) value = this.formatBytes(+value, +decimals);

    return value;
  }

  // Converts bytes into KB, MB, GB, etc., with configurable decimal precision
  formatBytes(bytes: number, decimals = 2): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }
}
