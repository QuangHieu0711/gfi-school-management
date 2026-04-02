import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { TYPE_FORM } from '@constant/constant';
import { TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { COMMON_TABLE_KEY, TableDataSource, TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import { KHOI_KEY, KhoiResponse } from '@app/model/admin/khoi.model';
import { MON_HOC_KEY, MonHocResponse } from '@app/model/admin/mon-hoc.model';
import { KhoiMonHocService } from '@app/service/admin/khoi-mon-hoc.service';
import { MonHocService } from '@app/service/admin/mon-hoc.service';

@Component({
  selector: 'dialog-cau-hinh-mon-hoc',
  templateUrl: './dialog-cau-hinh-mon-hoc.component.html',
  styleUrls: ['./dialog-cau-hinh-mon-hoc.component.scss'],
  imports: [
    AppDialogComponent,
    IconComponent,
    AppTableComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class DialogCauHinhMonHocComponent extends ComponentBaseAbstract {
  override readonly TYPE_FORM = TYPE_FORM;
  readonly MON_HOC_KEY = MON_HOC_KEY;
  readonly $formItem = [
    TEXT_CONTROL({
      controlName: MON_HOC_KEY.NAME,
      placeholder: 'T\u00ecm ki\u1ebfm theo m\u00e3 ho\u1eb7c t\u00ean m\u00f4n h\u1ecdc',
      required: false,
      maxLength: 255,
    }),
  ];
  readonly tableConfig = {
    hasFilterPanel: false,
    rowSelectable: true,
    multiSelectable: true,
  };
  readonly columns: MtxGridColumn[] = [
    {
      header: 'STT',
      class: 'text-center',
      field: COMMON_TABLE_KEY.STT,
    },
    {
      header: 'M\u00e3 m\u00f4n h\u1ecdc',
      field: MON_HOC_KEY.CODE,
    },
    {
      header: 'T\u00ean m\u00f4n h\u1ecdc',
      field: MON_HOC_KEY.NAME,
    },
  ];

  dataSource: MonHocResponse[] = [];
  rowSelected: TableDataSource[] = [];
  title = '';
  selectedSubjectIds = new Set<ID_TYPE>();

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogCauHinhMonHocComponent>,
    private readonly monHocService: MonHocService,
    private readonly khoiMonHocService: KhoiMonHocService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      gradeLevelId: ID_TYPE;
      gradeLevel?: KhoiResponse | null;
    }
  ) {
    super(injector);
    this.title = `C\u1ea5u h\u00ecnh m\u00f4n h\u1ecdc: ${data.gradeLevel?.[KHOI_KEY.NAME] ?? ''}`;
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);

    forkJoin({
      detail: this.khoiMonHocService.getDetail(this.data.gradeLevelId),
      subjects: this.monHocService.filter({
        pageSize: this.pageSize,
        pageNow: 1,
        filter: {},
      }),
    }).subscribe({
      next: ({ detail, subjects }) => {
        const selectedIds = detail.data?.subjectIds?.length
          ? detail.data.subjectIds
          : (detail.data?.subjects ?? []).map((item) => item.subjectId);

        this.selectedSubjectIds = new Set(selectedIds);
        this.bindSubjectData(subjects.data?.items || [], subjects.data?.recordTotal || 0);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Kh\u00f4ng t\u1ea3i \u0111\u01b0\u1ee3c danh s\u00e1ch m\u00f4n h\u1ecdc',
          'Th\u1ea5t b\u1ea1i'
        );
      },
    });
  }

  filterData(pageChangeEvent?: TableQueryEvent) {
    const formValues = this.form.getRawValue();
    const payload = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? 0) + 1,
      filter: {
        subject: formValues[MON_HOC_KEY.NAME] ?? undefined,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.monHocService.filter(payload).subscribe({
      next: ({ data }) => {
        this.bindSubjectData(data.items || [], data.recordTotal || 0);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Kh\u00f4ng t\u1ea3i \u0111\u01b0\u1ee3c danh s\u00e1ch m\u00f4n h\u1ecdc',
          'Th\u1ea5t b\u1ea1i'
        );
      },
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  onRowSelected(rows: TableDataSource[]) {
    this.selectedSubjectIds = new Set(rows.map((row) => row.id as ID_TYPE));
  }

  saveConfig() {
    this.khoiMonHocService
      .assign({
        gradeLevelId: this.data.gradeLevelId,
        subjectIds: [...this.selectedSubjectIds],
      })
      .subscribe({
        next: () => {
          this.toastr.success(
            'L\u01b0u c\u1ea5u h\u00ecnh th\u00e0nh c\u00f4ng',
            'Th\u00e0nh c\u00f4ng'
          );
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'L\u01b0u c\u1ea5u h\u00ecnh th\u1ea5t b\u1ea1i',
            'Th\u1ea5t b\u1ea1i'
          );
        },
      });
  }

  closeDialog() {
    this.dialogRef.close();
  }

  private bindSubjectData(dataSource: MonHocResponse[], total: number) {
    this.dataSource = dataSource;
    this.dataSourceTotal = total;
    this.rowSelected = this.dataSource.filter((row) =>
      this.selectedSubjectIds.has(row[MON_HOC_KEY.ID])
    );
  }
}
