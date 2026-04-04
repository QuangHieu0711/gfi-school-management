import {
  Component,
  Inject,
  Injector,
  TemplateRef,
  ViewChild,
} from '@angular/core';
import { MtxGridCellTemplate, MtxGridColumn } from '@ng-matero/extensions/grid';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { TYPE_FORM } from '@constant/constant';
import { TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
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
  @ViewChild('selectHeaderTpl', { static: true })
  selectHeaderTpl!: TemplateRef<unknown>;

  @ViewChild('selectTpl', { static: true })
  selectTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly MON_HOC_KEY = MON_HOC_KEY;
  readonly $formItem = [
    TEXT_CONTROL({
      controlName: MON_HOC_KEY.NAME,
      placeholder: 'Tìm kiếm theo mã hoặc tên môn học',
      required: false,
      maxLength: 255,
    }),
  ];
  readonly tableConfig = {
    hasFilterPanel: false,
    rowSelectable: false,
    multiSelectable: false,
  };
  columns: MtxGridColumn[] = [];
  headerTemplate: MtxGridCellTemplate = {};

  dataSource: MonHocResponse[] = [];
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
    this.title = `Cấu hình môn học: ${data.gradeLevel?.[KHOI_KEY.NAME] ?? ''}`;
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.headerTemplate = {
      selected: this.selectHeaderTpl,
    };
    this.columns = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Mã môn học',
        field: MON_HOC_KEY.CODE,
      },
      {
        header: 'Tên môn học',
        field: MON_HOC_KEY.NAME,
      },
      {
        header: '',
        field: 'selected',
        width: '72px',
        class: 'text-center',
        cellTemplate: this.selectTpl,
      },
    ];

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
        this.bindSubjectData(
          subjects.data?.items || [],
          subjects.data?.recordTotal || 0
        );
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách môn học',
          'Thất bại'
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
            'Không tải được danh sách môn học',
          'Thất bại'
        );
      },
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  toggleSubject(subjectId: ID_TYPE, checked: boolean) {
    if (checked) {
      this.selectedSubjectIds.add(subjectId);
      return;
    }

    this.selectedSubjectIds.delete(subjectId);
  }

  isSelected(subjectId: ID_TYPE) {
    return this.selectedSubjectIds.has(subjectId);
  }

  toggleCurrentPage(checked: boolean) {
    this.dataSource.forEach((item) => {
      const subjectId = item[MON_HOC_KEY.ID];
      if (checked) {
        this.selectedSubjectIds.add(subjectId);
      } else {
        this.selectedSubjectIds.delete(subjectId);
      }
    });
  }

  isAllCurrentPageSelected() {
    return this.dataSource.length > 0
      ? this.dataSource.every((item) =>
          this.selectedSubjectIds.has(item[MON_HOC_KEY.ID])
        )
      : false;
  }

  saveConfig() {
    this.khoiMonHocService
      .assign({
        gradeLevelId: this.data.gradeLevelId,
        subjectIds: [...this.selectedSubjectIds],
      })
      .subscribe({
        next: () => {
          this.toastr.success('Lưu cấu hình thành công', 'Thành công');
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Lưu cấu hình thất bại',
            'Thất bại'
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
  }
}
