export interface EvaluationBulkSaveRequest {
  classroomId: number | string;
  subjectId: number | string;
  semesterId: number | string;
  items: EvaluationItemSaveRequest[];
}

export interface EvaluationItemSaveRequest {
  studentId: number | string;
  midtermLevel?: string;
  midtermScore?: number;
  midtermRemark?: string;
  finalLevel?: string;
  finalScore?: number;
  finalRemark?: string;
}

export interface EvaluationGenerateCommentRequest {
  classroomId: number | string;
  subjectId: number | string;
  studentId: number | string;
  term: string;
  evaluation: string;
  participationLevel?: string;
  behaviorTag?: string;
}

export interface EvaluationBulkGenerateCommentItem {
  studentId: number | string;
  evaluation: string;
}

export interface EvaluationBulkGenerateCommentRequest {
  classroomId: number | string;
  subjectId: number | string;
  term: string;
  items: EvaluationBulkGenerateCommentItem[];
}

export interface EvaluationSheetResponse {
  classroomId: number;
  classroomName: string;
  subjectId: number;
  subjectName: string;
  semesterId: number;
  semesterName: string;
  students: EvaluationSheetStudent[];
}

export interface EvaluationSheetStudent {
  studentId: number;
  studentCode: string;
  studentName: string;
  midtermLevel: string;
  midtermScore: number;
  midtermRemark: string;
  finalLevel: string;
  finalScore: number;
  finalRemark: string;
}
