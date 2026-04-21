import { ID_TYPE } from '@model/response.model';

export const PHAN_CONG_GIAO_VIEN_API_ENDPOINT = {
  SUBJECTS: 'subjects',
  CLASSROOMS: 'classrooms',
};

export interface PhanCongGiaoVienClassroomQueryParams {
  unitId?: ID_TYPE;
}

export interface PhanCongGiaoVienClassroomResponse {
  id: ID_TYPE;
  name: string;
}
