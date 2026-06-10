import axios from 'axios';
import type { ApiResponse } from '../types';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080/api';

export class ApiError extends Error {
  code: string;

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

export const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
});

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>;
    if (body && body.success === false) {
      throw new ApiError(body.code, body.message);
    }
    return response;
  },
  (error) => {
    const body = error.response?.data as ApiResponse<unknown> | undefined;
    if (body?.message) {
      throw new ApiError(body.code || 'REQUEST_ERROR', body.message);
    }
    throw new ApiError('NETWORK_ERROR', '接口暂时不可用，请检查后端服务是否已启动');
  },
);

export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise;
  return response.data.data;
}
