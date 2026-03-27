/* eslint-disable @typescript-eslint/no-explicit-any */
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  NGUOI_DUNG_KEY,
  NguoiDungFormRequest,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { HttpOptions, ID_TYPE, ITableResponse } from '@model/response.model';
import { Observable, of } from 'rxjs';

interface FilterPayload {
  pageSize?: number;
  pageNow?: number;
  filter?: {
    unitId?: string[] | string;
    name?: string;
    status?: number | number[];
  };
}

@Injectable({ providedIn: 'root' })
export class NguoiDungService {
  private readonly storageKey = 'local_nguoi_dung_data';
  private readonly defaultUsers: NguoiDungResponse[] = [
    {
      id: 1,
      no: 1,
      lastName: 'Nguyen',
      firstName: 'Admin',
      username: 'admin',
      email: 'admin@local.dev',
      phone: 912345678,
      roleId: 'admin',
      roleName: 'Quan tri vien',
      unitId: 'DV001',
      unitName: 'Don vi local',
      shortName: 'LOCAL',
      status: 1,
    },
    {
      id: 2,
      no: 2,
      lastName: 'Tran',
      firstName: 'Van A',
      username: 'vana',
      email: 'vana@local.dev',
      phone: 987654321,
      roleId: 'editor',
      roleName: 'Bien tap vien',
      unitId: 'DV002',
      unitName: 'Phong nghiep vu',
      shortName: 'PNV',
      status: 1,
    },
    {
      id: 3,
      no: 3,
      lastName: 'Le',
      firstName: 'Thi B',
      username: 'thib',
      email: 'thib@local.dev',
      phone: 901234567,
      roleId: 'viewer',
      roleName: 'Nguoi xem',
      unitId: 'DV003',
      unitName: 'Phong tong hop',
      shortName: 'PTH',
      status: 0,
    },
  ];

  filter(
    payload: FilterPayload
  ): Observable<{ data: ITableResponse<NguoiDungResponse> }> {
    const pageSize = payload.pageSize ?? 10;
    const pageNow = payload.pageNow ?? 0;
    const users = this.applyFilter(this.readUsers(), payload.filter);
    const pageStart = pageNow * pageSize;
    const items = users
      .slice(pageStart, pageStart + pageSize)
      .map((user, index) => ({
        ...user,
        [NGUOI_DUNG_KEY.STT]: pageStart + index + 1,
      }));

    return of({
      data: {
        pageNo: pageNow + 1,
        pageSize,
        totalCount: users.length,
        totalPage: Math.ceil(users.length / pageSize) || 1,
        data: items,
        items,
        recordTotal: users.length,
        pageTotal: Math.ceil(users.length / pageSize) || 1,
      },
    });
  }

  getById(id: ID_TYPE): Observable<{ data: NguoiDungResponse }> {
    const user = this.readUsers().find(
      (item) => String(item.id) === String(id)
    );
    return of({
      data: user ?? this.createEmptyUser(),
    });
  }

  delete(ids: ID_TYPE[]): Observable<{ data: { deleted: number } }> {
    const idSet = new Set(ids.map(String));
    const users = this.readUsers().filter(
      (item) => !idSet.has(String(item.id))
    );
    this.writeUsers(users);

    return of({
      data: { deleted: ids.length },
    });
  }

  create(
    payload: NguoiDungFormRequest,
    _options?: HttpOptions
  ): Observable<{ data: NguoiDungResponse }> {
    const users = this.readUsers();
    const created: NguoiDungResponse = {
      ...this.createEmptyUser(),
      ...payload,
      id: this.nextId(users),
      roleName: this.resolveRoleName(payload.roleId),
      unitName: this.resolveUnitName(payload.unitId),
      status: Number(payload.status ?? 1),
    };

    users.unshift(created);
    this.writeUsers(users);

    return of({ data: created });
  }

  update(
    payload: NguoiDungFormRequest,
    _options?: HttpOptions
  ): Observable<{ data: NguoiDungResponse }> {
    const users = this.readUsers();
    const index = users.findIndex(
      (item) => String(item.id) === String(payload.id)
    );
    const updated: NguoiDungResponse = {
      ...(index >= 0 ? users[index] : this.createEmptyUser()),
      ...payload,
      roleName: this.resolveRoleName(payload.roleId) || users[index]?.roleName,
      unitName: this.resolveUnitName(payload.unitId) || users[index]?.unitName,
      status: Number(payload.status ?? users[index]?.status ?? 1),
    };

    if (index >= 0) {
      users[index] = updated;
      this.writeUsers(users);
    }

    return of({ data: updated });
  }

  filterAll(): Observable<{ data: NguoiDungResponse[] }> {
    return of({ data: this.readUsers() });
  }

  export(payload: {
    exportType?: 'PDF' | 'EXCEL';
  }): Observable<HttpResponse<Blob>> {
    const users = this.readUsers();
    const content =
      payload.exportType === 'PDF'
        ? this.buildPdfText(users)
        : this.buildCsv(users);
    const blob = new Blob([content], {
      type:
        payload.exportType === 'PDF'
          ? 'application/pdf'
          : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const extension = payload.exportType === 'PDF' ? 'pdf' : 'xlsx';

    return of(
      new HttpResponse({
        body: blob,
        headers: new HttpHeaders({
          'content-disposition': `attachment; filename="nguoi-dung.${extension}"`,
        }),
      })
    );
  }

  import(payload: FormData): Observable<{ data: { message: string } }> {
    const file = payload.get('file');

    return of({
      data: {
        message:
          file instanceof File
            ? `Imported locally: ${file.name}`
            : 'Imported locally',
      },
    });
  }

  downloadTemplate(): Observable<Blob> {
    const template =
      'Ho,Ten,TenTaiKhoan,Email,SoDienThoai,DonVi,TrangThai,NhomQuyen\n' +
      'Nguyen,Van C,vanc,vanc@local.dev,0911111111,DV001,1,admin\n';

    return of(
      new Blob([template], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
    );
  }

  private readUsers(): NguoiDungResponse[] {
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (!raw) {
        this.writeUsers(this.defaultUsers);
        return [...this.defaultUsers];
      }

      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) && parsed.length > 0
        ? parsed
        : [...this.defaultUsers];
    } catch {
      return [...this.defaultUsers];
    }
  }

  private writeUsers(users: NguoiDungResponse[]): void {
    localStorage.setItem(this.storageKey, JSON.stringify(users));
  }

  private applyFilter(
    users: NguoiDungResponse[],
    filter?: FilterPayload['filter']
  ): NguoiDungResponse[] {
    const keyword = (filter?.name ?? '').toString().trim().toLowerCase();
    const unitIds = Array.isArray(filter?.unitId)
      ? filter?.unitId.map(String)
      : filter?.unitId
        ? [String(filter.unitId)]
        : [];
    const statuses = Array.isArray(filter?.status)
      ? filter.status.map(Number)
      : filter?.status !== undefined && filter?.status !== null
        ? [Number(filter.status)]
        : [];

    return users.filter((user) => {
      const fullName = `${user.lastName ?? ''} ${user.firstName ?? ''}`
        .trim()
        .toLowerCase();
      const matchesKeyword =
        !keyword ||
        fullName.includes(keyword) ||
        String(user.username ?? '')
          .toLowerCase()
          .includes(keyword) ||
        String(user.email ?? '')
          .toLowerCase()
          .includes(keyword);
      const matchesUnit =
        unitIds.length === 0 || unitIds.includes(String(user.unitId ?? ''));
      const matchesStatus =
        statuses.length === 0 || statuses.includes(Number(user.status ?? 0));

      return matchesKeyword && matchesUnit && matchesStatus;
    });
  }

  private nextId(users: NguoiDungResponse[]): number {
    return (
      users.reduce((max, item) => Math.max(max, Number(item.id) || 0), 0) + 1
    );
  }

  private resolveRoleName(roleId?: string): string {
    switch (roleId) {
      case 'admin':
        return 'Quan tri vien';
      case 'editor':
        return 'Bien tap vien';
      case 'viewer':
        return 'Nguoi xem';
      default:
        return roleId ?? 'Chua gan nhom';
    }
  }

  private resolveUnitName(unitId?: ID_TYPE): string {
    switch (String(unitId ?? '')) {
      case 'DV001':
        return 'Don vi local';
      case 'DV002':
        return 'Phong nghiep vu';
      case 'DV003':
        return 'Phong tong hop';
      default:
        return String(unitId ?? 'Don vi moi');
    }
  }

  private createEmptyUser(): NguoiDungResponse {
    return {
      id: 0,
      no: 0,
      lastName: '',
      firstName: '',
      username: '',
      email: '',
      phone: 0,
      roleId: '',
      roleName: '',
      unitId: '',
      unitName: '',
      shortName: '',
      status: 1,
    };
  }

  private buildCsv(users: NguoiDungResponse[]): string {
    const rows = [
      'ID,Ho,Ten,TenTaiKhoan,Email,SoDienThoai,DonVi,TrangThai,NhomQuyen',
      ...users.map((user) =>
        [
          user.id,
          user.lastName,
          user.firstName,
          user.username,
          user.email,
          user.phone,
          user.unitName,
          user.status,
          user.roleName,
        ].join(',')
      ),
    ];

    return rows.join('\n');
  }

  private buildPdfText(users: NguoiDungResponse[]): string {
    const lines = [
      'DANH SACH NGUOI DUNG',
      '',
      ...users.map(
        (user) =>
          `${user.id}. ${user.lastName ?? ''} ${user.firstName ?? ''} - ${user.username ?? ''} - ${user.email ?? ''}`
      ),
    ];

    return lines.join('\n');
  }
}
