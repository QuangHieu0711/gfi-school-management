import {
  Component,
  Inject,
  Injector,
  TemplateRef,
  ViewChild,
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MtxGridCellTemplate, MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { TYPE_FORM } from '@constant/constant';
import { TEXT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import { LOP_KEY, LopResponse } from '@app/model/admin/lop.model';
import { LopMonHocDetailSubjectResponse } from '@app/model/admin/lop-mon-hoc.model';
import { MON_HOC_KEY } from '@app/model/admin/mon-hoc.model';
import { LopMonHocService } from '@app/service/admin/lop-mon-hoc.service';

@Component({
  selector: 'dialog-cau-hinh-mon-hoc-lop',
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
export class DialogCauHinhMonHocLopComponent extends ComponentBaseAbstract {
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
  dataSource: LopMonHocDetailSubjectResponse[] = [];
  allSubjects: LopMonHocDetailSubjectResponse[] = [];
  title = '';
  selectedSubjectIds = new Set<ID_TYPE>();

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogCauHinhMonHocLopComponent>,
    private readonly lopMonHocService: LopMonHocService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      classroomId: ID_TYPE;
      classroom?: LopResponse | null;
    }
  ) {
    super(injector);
    this.title = `Cấu hình môn học: ${data.classroom?.[LOP_KEY.NAME] ?? ''}`;
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
        field: 'subjectCode',
      },
      {
        header: 'Tên môn học',
        field: 'subjectName',
      },
      {
        header: '',
        field: 'selected',
        width: '72px',
        class: 'text-center',
        cellTemplate: this.selectTpl,
      },
    ];

    this.loadDetail();
  }

  filterData(pageChangeEvent?: TableQueryEvent) {
    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    const keyword = (this.form.getRawValue()?.[MON_HOC_KEY.NAME] ?? '')
      .toString()
      .trim()
      .toLowerCase();

    const filteredData = this.allSubjects.filter((item) => {
      if (!keyword) return true;
      return (
        item.subjectCode?.toLowerCase().includes(keyword) ||
        item.subjectName?.toLowerCase().includes(keyword)
      );
    });

    this.dataSourceTotal = filteredData.length;
    const start = this.pageIndex * this.pageSize;
    this.dataSource = filteredData.slice(start, start + this.pageSize);
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
      if (checked) {
        this.selectedSubjectIds.add(item.subjectId);
      } else {
        this.selectedSubjectIds.delete(item.subjectId);
      }
    });
  }

  isAllCurrentPageSelected() {
    return this.dataSource.length > 0
      ? this.dataSource.every((item) =>
          this.selectedSubjectIds.has(item.subjectId)
        )
      : false;
  }

  saveConfig() {
    this.lopMonHocService
      .assign({
        classroomId: this.data.classroomId,
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

  private loadDetail() {
    this.lopMonHocService.getDetail(this.data.classroomId).subscribe({
      next: ({ data }) => {
        this.allSubjects = (data?.subjects ?? []).map((item) => ({
          ...item,
          id: item.subjectId,
        }));

        const selectedIds = data?.subjectIds?.length
          ? data.subjectIds
          : this.allSubjects
              .filter((item) => item.selected)
              .map((item) => item.subjectId);

        this.selectedSubjectIds = new Set(selectedIds);
        this.filterData({
          pageIndex: 0,
          pageSize: this.pageSize,
        });
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được chi tiết cấu hình môn học',
          'Thất bại'
        );
      },
    });
  }
}
