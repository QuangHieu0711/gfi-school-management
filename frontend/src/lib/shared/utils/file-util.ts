export function saveBlobAsFile(blob: Blob, filename: string) {
  if (!(blob instanceof Blob)) {
    console.error('Invalid Blob object provided:', blob);
    throw new Error('The provided data is not a valid Blob.');
  }

  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export function defaultExportFileName(base = 'export', ext = 'xlsx') {
  const ts = new Date().toISOString().replace(/[:.]/g, '-');
  return `${base}-${ts}.${ext}`;
}

export function getMimeType(filename: string): string {
  const ext = filename.split('.').pop()?.toLowerCase();

  switch (ext) {
    case 'pdf':
      return 'application/pdf';
    case 'doc':
      return 'application/msword';
    case 'docx':
      return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
    case 'xls':
      return 'application/vnd.ms-excel';
    case 'xlsx':
      return 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    case 'csv':
      return 'text/csv';
    case 'jpg':
    case 'jpeg':
      return 'image/jpeg';
    case 'png':
      return 'image/png';
    case 'gif':
      return 'image/gif';
    case 'dwg':
      return 'application/acad';
    case 'tab':
      return 'text/tab-separated-values';
    case 'zip':
      return 'application/zip';
    default:
      return 'application/octet-stream';
  }
}
