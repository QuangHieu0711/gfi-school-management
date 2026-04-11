import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Injector,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  TemplateRef,
  ViewChild,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { finalize, forkJoin } from 'rxjs';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { ComponentBaseAbstract } from '@layout';
import { TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource } from '@model/table.model';

import { VAI_TRO_KEY, VaiTroResponse } from '@app/model/admin/vai-tro.model';
import {
  RoleAssignmentItem,
  RoleAssignmentService,
} from '@app/service/admin/role-assignment.service';
import { VaiTroService } from '@app/service/admin/vai-tro.service';

interface RoleAssignmentRow extends TableDataSource {
  id: ID_TYPE;
  roleId: ID_TYPE;
  roleCode: string;
  roleName: string;
  canCreate: boolean;
  canUpdate: boolean;
  originalCanCreate: boolean;
  originalCanUpdate: boolean;
}

@Component({
  selector: 'phan-quyen-vai-tro',
  templateUrl: './phan-quyen-vai-tro.component.html',
  styleUrls: ['./phan-quyen-vai-tro.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppTableComponent,
    IconComponent,
    ...FORM_CONTROL_MODULE,
    ...MATERIAL_MODULE,
  ],
})
export class PhanQuyenVaiTroComponent
  extends ComponentBaseAbstract
  implements OnChanges
{
  @ViewChild('toggleTpl', { static: true }) toggleTpl!: TemplateRef<unknown>;

  @Input() roleId: ID_TYPE | null = null;
  @Input() roleDetail?: VaiTroResponse;

  @Output() dirtyStateChange = new EventEmitter<{
    count: number;
    hasDirty: boolean;
  }>();

  readonly tableConfig = {
    showPaginator: false,
  };
  readonly searchForm = new FormGroup({
    code: new FormControl('', { nonNullable: true }),
  });
  readonly searchItems = [
    TEXT_CONTROL({
      controlName: VAI_TRO_KEY.CODE,
      placeholder: 'Tìm theo mã vai trò',
      required: false,
      maxLength: 50,
    }),
  ];
  readonly key = VAI_TRO_KEY;

  columns: MtxGridColumn[] = [];
  loading = false;
  saving = false;
  tableData: RoleAssignmentRow[] = [];

  private allRows: RoleAssignmentRow[] = [];
  private dirtyRowIds = new Set<ID_TYPE>();

  constructor(
    protected override injector: Injector,
    private readonly vaiTroService: VaiTroService,
    private readonly roleAssignmentService: RoleAssignmentService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.columns = [
      { header: 'Mã vai trò', field: 'roleCode' },
      { header: 'Tên vai trò', field: 'roleName' },
      {
        header: 'Tạo mới',
        field: 'canCreate',
        class: 'text-center',
        cellTemplate: this.toggleTpl,
      },
      {
        header: 'Cập nhật',
        field: 'canUpdate',
        class: 'text-center',
        cellTemplate: this.toggleTpl,
      },
    ];

    this.loadRoles();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['roleId'] && this.roleId != null) {
      this.loadRoles();
    }
  }

  get hasDirtyRows(): boolean {
    return this.dirtyRowIds.size > 0;
  }

  submitSearch(): void {
    this.loadRoles();
  }

  resetSearch(): void {
    this.searchForm.reset({ code: '' });
    this.loadRoles();
  }

  resetChanges(): void {
    if (!this.dirtyRowIds.size) {
      this.toastr.warning('Chưa có thay đổi để hủy', 'Thông báo');
      return;
    }

    this.allRows = this.allRows.map((row) => ({
      ...row,
      canCreate: row.originalCanCreate,
      canUpdate: row.originalCanUpdate,
    }));
    this.tableData = [...this.allRows];
    this.dirtyRowIds.clear();
    this.updateDirtyState();
  }

  saveChanges(): void {
    if (this.roleId == null) return;
    if (!this.dirtyRowIds.size) {
      this.toastr.warning('Chưa có thay đổi để lưu', 'Thông báo');
      return;
    }

    const payload = this.allRows
      .filter((row) => this.dirtyRowIds.has(row.roleId))
      .map((row) => ({
        targetRoleId: row.roleId,
        canCreate: Number(row.canCreate),
        canUpdate: Number(row.canUpdate),
      }));

    this.saving = true;
    this.roleAssignmentService
      .saveRoleAssignments(this.roleId, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.toastr.success(
            'Cập nhật phân quyền vai trò thành công',
            'Thành công'
          );
          this.loadRoles();
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Cập nhật phân quyền vai trò thất bại',
            'Thất bại'
          );
        },
      });
  }

  onToggleChange(
    row: RoleAssignmentRow,
    event: MatSlideToggleChange,
    field?: string | null
  ): void {
    if (field !== 'canCreate' && field !== 'canUpdate') return;
    row[field] = event.checked;
    this.syncDirtyState(row);
    this.tableData = [...this.tableData];
  }

  private loadRoles(): void {
    if (this.roleId == null) return;

    const roleCode = this.searchForm.controls.code.value.trim();
    const payload = {
      pageSize: 1000,
      pageNow: 1,
      filter: {
        code: roleCode || undefined,
      },
    };

    this.loading = true;
    forkJoin({
      roles: this.vaiTroService.filter(payload),
      assignments: this.roleAssignmentService.getRoleAssignments(this.roleId),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ roles, assignments }) => {
          const assignmentMap = new Map<ID_TYPE, RoleAssignmentItem>(
            (assignments.data?.items ?? []).map((item) => [
              item.targetRoleId,
              item,
            ])
          );

          this.allRows = (roles.data?.items ?? [])
            .filter((item) => item[VAI_TRO_KEY.ID] !== this.roleId)
            .map((item) => this.mapRoleRow(item, assignmentMap.get(item.id)));

          this.tableData = [...this.allRows];
          this.dirtyRowIds.clear();
          this.updateDirtyState();
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Không tải được danh sách phân quyền vai trò',
            'Thất bại'
          );
        },
      });
  }

  private mapRoleRow(
    role: VaiTroResponse,
    assignment?: RoleAssignmentItem
  ): RoleAssignmentRow {
    const canCreate = Number(assignment?.canCreate ?? 0) === 1;
    const canUpdate = Number(assignment?.canUpdate ?? 0) === 1;

    return {
      id: role[VAI_TRO_KEY.ID],
      roleId: role[VAI_TRO_KEY.ID],
      roleCode: role[VAI_TRO_KEY.CODE] ?? '',
      roleName: role[VAI_TRO_KEY.ROLE_NAME] ?? '',
      canCreate,
      canUpdate,
      originalCanCreate: canCreate,
      originalCanUpdate: canUpdate,
    };
  }

  private syncDirtyState(row: RoleAssignmentRow): void {
    const hasChanged =
      row.canCreate !== row.originalCanCreate ||
      row.canUpdate !== row.originalCanUpdate;

    if (hasChanged) {
      this.dirtyRowIds.add(row.roleId);
    } else {
      this.dirtyRowIds.delete(row.roleId);
    }

    this.updateDirtyState();
  }

  private updateDirtyState(): void {
    const count = this.dirtyRowIds.size;
    this.dirtyStateChange.emit({
      count,
      hasDirty: count > 0,
    });
  }
}
