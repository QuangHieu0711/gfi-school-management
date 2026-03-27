import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { FILE_API_ENDPOINT, FileDownloadRequest, FileUploadRequest } from '@model/file.model';
import { ApiService } from '@service';

/**
 * Handles file-related operations such as uploading and downloading.
 */
@Injectable({ providedIn: 'root' })
export class FileService {
  constructor(readonly apiService: ApiService) {}

  /**
   * Triggers a browser download for a given Blob and filename.
   * @param blob File content as Blob
   * @param fileName Target filename for download
   */
  downloadFile(blob: Blob, fileName: string) {
    const a = document.createElement('a');
    const url = window.URL.createObjectURL(blob);
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
  }

  /**
   * Uploads one or more files to the backend via FormData.
   * @param payload Upload request payload
   */
  uploadFileAsync(payload: FileUploadRequest) {
    const formData = new FormData();

    if (payload.id) formData.append('id', payload.id!);
    if (payload.type) formData.append('type', payload.type!);
    formData.append('file', payload.files[0]);
    return this.apiService.postFile(FILE_API_ENDPOINT.UPLOAD, formData, { fileName: payload.files[0].name }, { apiHost: 'gis',routerHost: 'sys', });
  }

  /**
   * Downloads a file from the server with optional filtering parameters.
   * @param payload Contains ID, fileId, and type for the download
   */
  downloadFileAsync(payload: FileDownloadRequest) {
    const params = new HttpParams().set('id', payload.id).set('fileId', payload.fileId!).set('type', payload.type);
    return this.apiService.get(FILE_API_ENDPOINT.DOWNLOAD, {
      params,
      responseType: 'blob',
      routerHost: 'sys',
    });
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

  /**
   * Downloads a file from the server with optional filtering parameters.
   * @param payload Contains ID, fileId, and type for the download
   */
  getFileUrlAsync(fileName: string) {
    return this.apiService.get<string>(FILE_API_ENDPOINT.DOWNLOAD, {
      params: { fileName: encodeURIComponent(fileName) },
      apiHost: 'gis',
      routerHost: 'sys',
    });
  }

  downloadFileUrl(url: string) {
    const a = document.createElement('a');
    a.href = url;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
