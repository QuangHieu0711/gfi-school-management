/* eslint-disable prettier/prettier */
import { Component, Inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { MATERIAL_MODULE } from '@modules';

export interface NotificationDetailData {
  message: string;
  content: string;
  creator: string;
  time: string;
  read: boolean;
}

@Component({
  selector: 'notification-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, ...MATERIAL_MODULE, IconComponent],
  template: `
    <div class="p-0 min-w-[420px] max-w-[520px]">
      <!-- Header -->
      <div
        class="flex items-center justify-between px-5 py-4 border-b border-gray-200">
        <div class="flex items-center gap-2">
          <span class="flex items-center justify-center w-5 h-5">
            <app-icon
              icon="notifications"
              iconClass="!mr-0 !text-[20px] !w-5 !h-5 !leading-none text-primary-500" />
          </span>
          <span class="text-base font-semibold text-gray-800"
            >Chi tiết thông báo</span
          >
        </div>
        <button
          mat-icon-button
          (click)="close()">
          <app-icon
            icon="close"
            iconClass="!mr-0 text-gray-500" />
        </button>
      </div>

      <!-- Body -->
      <div class="px-5 py-4 flex flex-col gap-4">
        <!-- Tiêu đề thông báo -->
        <div
          class="flex items-start gap-2 p-3 rounded-lg border"
          [class.bg-blue-50]="!data.read"
          [class.bg-gray-50]="data.read"
          [class.border-blue-200]="!data.read"
          [class.border-gray-200]="data.read">
          <span
            class="flex items-center justify-center w-[18px] h-[18px] mt-[1px] shrink-0">
            <app-icon
              icon="info"
              iconClass="!mr-0 !text-[18px] !w-[18px] !h-[18px] !leading-none"
              [class.text-primary-500]="!data.read"
              [class.text-gray-400]="data.read" />
          </span>
          <p
            class="text-sm text-gray-800 leading-relaxed flex-1"
            [class.font-semibold]="!data.read">
            {{ data.message }}
          </p>
        </div>

        <!-- Thông tin meta -->
        <div class="flex flex-col gap-3">
          <div class="flex items-center gap-3">
            <div class="flex items-center gap-2 w-28 shrink-0">
              <span class="flex items-center justify-center w-4 h-4">
                <app-icon
                  icon="schedule"
                  iconClass="!mr-0 !text-[16px] !w-4 !h-4 !leading-none text-gray-400" />
              </span>
              <span class="text-xs font-medium text-gray-500">Thời gian</span>
            </div>
            <span class="text-sm text-gray-700">{{ data.time }}</span>
          </div>

          <div class="flex items-center gap-3">
            <div class="flex items-center gap-2 w-28 shrink-0">
              <span class="flex items-center justify-center w-4 h-4">
                <app-icon
                  icon="person"
                  iconClass="!mr-0 !text-[16px] !w-4 !h-4 !leading-none text-gray-400" />
              </span>
              <span class="text-xs font-medium text-gray-500">Người tạo</span>
            </div>
            <span class="text-sm text-gray-700">{{ data.creator }}</span>
          </div>
        </div>

        <!-- Nội dung -->
        <div class="flex flex-col gap-2">
          <div class="flex items-center gap-2">
            <span class="flex items-center justify-center w-4 h-4">
              <app-icon
                icon="description"
                iconClass="!mr-0 !text-[16px] !w-4 !h-4 !leading-none text-gray-400" />
            </span>
            <span class="text-xs font-medium text-gray-500">Nội dung</span>
          </div>
          <div
            class="p-3 rounded-lg bg-gray-50 border border-gray-200 text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">{{ data.content }}</div>
        </div>
      </div>

      <!-- Footer -->
      <div class="flex justify-end px-5 pb-4">
        <button
          color="info"
          matButton="filled"
          (click)="close()">
          Đóng
        </button>
      </div>
    </div>
  `,
})
export class NotificationDetailDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) public data: NotificationDetailData,
    private readonly dialogRef: MatDialogRef<NotificationDetailDialogComponent>
  ) {}

  close() {
    this.dialogRef.close();
  }
}
