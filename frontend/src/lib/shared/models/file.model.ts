export interface FileUploadRequest {
  id?: string;
  type?: string;
  files: File[];
}
export interface FileDownloadRequest {
  id: string;
  type: string;
  fileId?: string;
  objectId?: string;
}

export enum FILE_API_ENDPOINT {
  UPLOAD = 'file/upload-public',
  DOWNLOAD = 'file/download',
  GET_FILE_URL = 'file/get-file-url',
}
