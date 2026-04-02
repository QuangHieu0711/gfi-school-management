import { ID_TYPE } from '@model/response.model';
import { TableDataSource } from '@model/table.model';

export const LOP_MON_HOC_API_ENDPOINT = {
  BASE_PATH: 'classroom-subjects',
  ASSIGN: 'assign',
};

export interface LopMonHocAssignRequest {
  classroomId: ID_TYPE;
  subjectIds: ID_TYPE[];
}

export interface LopMonHocDetailSubjectResponse extends TableDataSource {
  id: ID_TYPE;
  subjectId: ID_TYPE;
  subjectCode: string;
  subjectName: string;
  subjectType?: number;
  selected?: boolean;
}

export interface LopMonHocDetailResponse {
  classroomId: ID_TYPE;
  classroomName?: string;
  gradeLevelId?: ID_TYPE;
  gradeLevelName?: string;
  subjectIds?: ID_TYPE[];
  subjects?: LopMonHocDetailSubjectResponse[];
}
