import { ID_TYPE } from '@model/response.model';

export const KHOI_MON_HOC_API_ENDPOINT = {
  BASE_PATH: 'grade-level-subjects',
  ASSIGN: 'assign',
};

export interface KhoiMonHocAssignRequest {
  gradeLevelId: ID_TYPE;
  subjectIds: ID_TYPE[];
}

export interface KhoiMonHocDetailSubjectResponse {
  id?: ID_TYPE;
  subjectId: ID_TYPE;
  subjectCode: string;
  subjectName: string;
  subjectType?: number;
}

export interface KhoiMonHocDetailResponse {
  gradeLevelId: ID_TYPE;
  gradeLevelName?: string;
  subjectIds?: ID_TYPE[];
  subjects?: KhoiMonHocDetailSubjectResponse[];
}
