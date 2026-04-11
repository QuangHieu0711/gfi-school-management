/* eslint-disable @typescript-eslint/no-explicit-any */
import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';
import { Observable } from 'rxjs';
import { ID_TYPE } from '@model/response.model';

export interface RoleAssignmentItem {
  targetRoleId: ID_TYPE;
  targetRoleCode: string;
  targetRoleName: string;
  canCreate: number | boolean;
  canUpdate: number | boolean;
}

export interface SaveRoleAssignmentRequest {
  targetRoleId: ID_TYPE;
  canCreate: number | boolean;
  canUpdate: number | boolean;
}

export interface RoleAssignmentResponse {
  creatorRoleCode?: string;
  creatorRoleId?: ID_TYPE;
  creatorRoleName?: string;
  items?: RoleAssignmentItem[];
}

const ROLE_ASSIGNMENT_API_ENDPOINT = {
  BASE_PATH: 'role-assignment-permissions',
  SAVE: 'save',
};

@Injectable({ providedIn: 'root' })
export class RoleAssignmentService {
  private readonly baseUrl = `${environment.host_api}/${ROLE_ASSIGNMENT_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  getRoleAssignments(
    roleId: ID_TYPE
  ): Observable<IResponse<RoleAssignmentResponse>> {
    return this.http.get<IResponse<RoleAssignmentResponse>>(
      `${this.baseUrl}/${roleId}`,
      { context: this.silentContext }
    );
  }

  saveRoleAssignments(
    roleId: ID_TYPE,
    payload: SaveRoleAssignmentRequest[]
  ): Observable<IResponse<any>> {
    return this.http.post<IResponse<any>>(
      `${this.baseUrl}/${ROLE_ASSIGNMENT_API_ENDPOINT.SAVE}`,
      {
        creatorRoleId: roleId,
        items: payload,
      },
      { context: this.silentContext }
    );
  }
}
