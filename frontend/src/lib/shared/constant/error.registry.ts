import { HttpContextToken } from '@angular/common/http';
import { ErrorHandlingContext, ErrorRegistry } from '@model/response.model';

export const ERROR_REGISTRY: ErrorRegistry = {
  400: {
    DEFAULT: (toast, t, error) => {
      toast.error(error.error.userMessage ?? 'Có lỗi xảy ra123.', 'Thất bại');
    },
  },
  403: {
    DEFAULT: (toast, t, error) => {
      toast.error(error.error.userMessage ?? 'Có lỗi xảy ra234.', 'Thất bại');
    },
  },
  404: {
    DEFAULT: (toast, t, error) => {
      toast.error(
        error.error.userMessage ??
          'Phiên làm việc đã hết hạn, vui lòng đăng nhập lại.',
        'Thất bại'
      );
    },
  },
  // 500: {
  //   DEFAULT: (toast, t, error) => {
  //     toast.error(error.error.message ?? 'Có lỗi xảy ra.', 'Thất bại');
  //   },
  // },
  // DEFAULT: {
  //   DEFAULT: (toast, t, error) => toast.error(error.error.message ?? 'Có lỗi xảy ra.', 'Thất bại'),
  // },
};

export const ERROR_CONTEXT = new HttpContextToken<ErrorHandlingContext>(() => ({
  silent: false,
}));
