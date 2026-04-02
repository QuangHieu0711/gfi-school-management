import { Component, Inject, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { ID_TYPE } from '@model/response.model';

import { HOC_KY_KEY, HocKyResponse } from '@app/model/admin/hoc-ky.model';
import {
  NAM_HOC_KEY,
  NamHocResponse,
  SCHOOL_YEAR_STATUS_OPTIONS,
} from '@app/model/admin/nam-hoc.model';
import { HocKyService } from '@app/service/admin/hoc-ky.service';
import { DialogHocKyComponent } from '../dialog-hoc-ky/dialog-hoc-ky.component';

@Component({
  selector: 'dialog-cau-hinh-hoc-ky',
  templateUrl: './dialog-cau-hinh-hoc-ky.component.html',
  styleUrls: ['./dialog-cau-hinh-hoc-ky.component.scss'],
  imports: [
    AppDialogComponent,
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
  ],
})
export class DialogCauHinhHocKyComponent extends ComponentBaseAbstract {
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;
  @ViewChild('currentTpl', { static: true })
  currentTpl!: TemplateRef<unknown>;
  @ViewChild('periodTpl', { static: true })
  periodTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly tableConfig = {
    hasFilterPanel: false,
    showPaginator: false,
  };
  readonly columns: MtxGridColumn[] = [];

  dataSource: HocKyResponse[] = [];
  title = '';

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogCauHinhHocKyComponent>,
    private readonly hocKyService: HocKyService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      schoolYearId: ID_TYPE;
      schoolYear?: NamHocResponse | null;
    }
  ) {
    super(injector);
    this.pageSize = 100;
    this.title = `C\u1ea5u h\u00ecnh h\u1ecdc k\u1ef3: ${
      data.schoolYear?.[NAM_HOC_KEY.NAME] ?? ''
    }`;
  }

  protected override componentInit(): void {
    this.initializeColumns();
    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
    });
  }

  filterData(pageChangeEvent?: TableQueryEvent) {
    const payload = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? 0) + 1,
      filter: {
        schoolYearId: this.data.schoolYearId,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.hocKyService.filter(payload).subscribe({
      next: ({ data }) => {
        const items = Array.isArray(data) ? data : data?.items || [];
        this.dataSource = items;
        this.dataSourceTotal = Array.isArray(data)
          ? data.length
          : data?.recordTotal || items.length;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Kh\u00f4ng t\u1ea3i \u0111\u01b0\u1ee3c danh s\u00e1ch h\u1ecdc k\u1ef3',
          'Th\u1ea5t b\u1ea1i'
        );
      },
    });
  }

  openCreateDialog() {
    this.dialog.componentDialog(
      DialogHocKyComponent,
      {
        width: '720px',
        data: {
          type: this.TYPE_FORM.CREATE,
          schoolYearId: this.data.schoolYearId,
          schoolYearName: this.data.schoolYear?.[NAM_HOC_KEY.NAME],
        },
      },
      (result?: boolean) => {
        if (result) {
          this.filterData({
            pageIndex: this.pageIndex,
            pageSize: this.pageSize,
          });
        }
      }
    );
  }

  openUpdateDialog(rowData: HocKyResponse) {
    this.dialog.componentDialog(
      DialogHocKyComponent,
      {
        width: '720px',
        data: {
          type: this.TYPE_FORM.UPDATE,
          id: rowData[HOC_KY_KEY.ID],
          schoolYearId: this.data.schoolYearId,
          schoolYearName: this.data.schoolYear?.[NAM_HOC_KEY.NAME],
        },
      },
      (result?: boolean) => {
        if (result) {
          this.filterData({
            pageIndex: 0,
            pageSize: this.pageSize,
          });
        }
      }
    );
  }

  deleteSemester(rowData: HocKyResponse) {
    this.dialog.confirm(
      {
        title: 'X\u00e1c nh\u1eadn',
        message: `B\u1ea1n c\u00f3 ch\u1eafc ch\u1eafn mu\u1ed1n x\u00f3a h\u1ecdc k\u1ef3 ${
          rowData[HOC_KY_KEY.NAME]
        } kh\u00f4ng?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.hocKyService.delete(rowData[HOC_KY_KEY.ID]).subscribe({
          next: () => {
            this.toastr.success('X\u00f3a th\u00e0nh c\u00f4ng', 'Th\u00e0nh c\u00f4ng');
            this.filterData({
              pageIndex: 0,
              pageSize: this.pageSize,
            });
          },
          error: (error) => {
            this.toastr.error(
              error?.error?.userMessage ??
                error?.error?.message ??
                'X\u00f3a th\u1ea5t b\u1ea1i',
              'Th\u1ea5t b\u1ea1i'
            );
          },
        });
      }
    );
  }

  getStatusLabel(status?: number): string {
    return (
      SCHOOL_YEAR_STATUS_OPTIONS.find((item) => item.value === status)?.label ??
      '--'
    );
  }

  formatDateRange(start?: string, end?: string): string {
    return `${this.formatDate(start)} - ${this.formatDate(end)}`;
  }

  private initializeColumns() {
    this.columns.splice(0, this.columns.length);
    this.columns.push(
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'M\u00e3 h\u1ecdc k\u1ef3',
        field: HOC_KY_KEY.CODE,
      },
      {
        header: 'T\u00ean h\u1ecdc k\u1ef3',
        field: HOC_KY_KEY.NAME,
      },
      {
        header: 'Th\u1ee9 t\u1ef1',
        field: HOC_KY_KEY.SEMESTER_ORDER,
        class: 'text-center',
      },
      {
        header: 'Th\u1eddi gian',
        field: HOC_KY_KEY.START_DATE,
        cellTemplate: this.periodTpl,
      },
      {
        header: 'Tr\u1ea1ng th\u00e1i',
        field: HOC_KY_KEY.STATUS,
        class: 'text-center',
        cellTemplate: this.statusTpl,
      },
      {
        header: 'Hi\u1ec7n h\u00e0nh',
        field: HOC_KY_KEY.IS_CURRENT,
        class: 'text-center',
        cellTemplate: this.currentTpl,
      },
      {
        header: 'H\u00e0nh \u0111\u1ed9ng',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        buttons: [
          {
            type: 'icon',
            icon: 'edit',
            tooltip: 'Ch\u1ec9nh s\u1eeda',
            click: (rowData: HocKyResponse) => this.openUpdateDialog(rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            tooltip: 'X\u00f3a',
            click: (rowData: HocKyResponse) => this.deleteSemester(rowData),
          },
        ],
      }
    );
  }

  private formatDate(value?: string): string {
    if (!value) return '--';
    const raw = value.slice(0, 10);
    const [year, month, day] = raw.split('-');
    if (!year || !month || !day) return raw;
    return `${day}/${month}/${year}`;
  }

  closeDialog() {
    this.dialogRef.close();
  }
}
