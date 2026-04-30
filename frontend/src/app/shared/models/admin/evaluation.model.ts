export interface EvaluationBulkSaveRequest {
  classroomId: number | string;
  subjectId: number | string;
  semesterId: number | string;
  items: EvaluationItemSaveRequest[];
}

export interface EvaluationItemSaveRequest {
  studentId: number | string;
  midtermLevel?: string;
  midtermRemark?: string;
  finalLevel?: string;
  finalRemark?: string;
}
