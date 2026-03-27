import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { FILE_API_ENDPOINT, FileDownloadRequest, FileUploadRequest } from '@model/file.model';

@Injectable({ providedIn: 'root' })
export class FileService {
  downloadFile(blob: Blob, fileName: string) {
    const a = document.createElement('a');
    const url = window.URL.createObjectURL(blob);
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
  }

  uploadFileAsync(payload: FileUploadRequest): Observable<{ data: { fileName: string; endpoint: string } }> {
    return of({
      data: {
        fileName: payload.files[0]?.name ?? 'local-file',
        endpoint: FILE_API_ENDPOINT.UPLOAD,
      },
    });
  }

  downloadFileAsync(payload: FileDownloadRequest): Observable<Blob> {
    const content = `Local file stub\nid=${payload.id}\nfileId=${payload.fileId ?? ''}\ntype=${payload.type}`;
    return of(new Blob([content], { type: 'text/plain' }));
  }

  getFileName() {
    const date = new Date();
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${hours}:${minutes}:${seconds}-${day}${month}${year}`;
  }

  getFileUrlAsync(fileName: string): Observable<{ data: string }> {
    return of({
      data: `local-file://${encodeURIComponent(fileName)}`,
    });
  }

  downloadFileUrl(url: string) {
    if (url.startsWith('local-file://')) {
      const decoded = decodeURIComponent(url.replace('local-file://', ''));
      const blob = new Blob([`Local stub for ${decoded}`], { type: 'text/plain' });
      this.downloadFile(blob, decoded);
      return;
    }

    const a = document.createElement('a');
    a.href = url;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
