import axios, { AxiosError } from "axios";

export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type ApiErrorResponse = {
  success: false;
  code: string;
  message: string;
  fieldErrors: Array<{ field: string; message: string }>;
  timestamp: string;
  path: string;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
  user: {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    emailVerified: boolean;
  };
  company: {
    id: string;
    name: string;
    slug: string;
    status: string;
  };
  roles: string[];
  permissions: string[];
};

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const PUBLIC_AUTH_PATHS = new Set([
  "/api/auth/register",
  "/api/auth/verify-email",
  "/api/auth/resend-verification",
  "/api/auth/login",
  "/api/auth/refresh",
  "/api/auth/logout",
  "/api/auth/forgot-password",
  "/api/auth/reset-password",
]);

let accessToken: string | null = null;
let refreshToken: string | null = null;

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

export function setTokens(tokens: {
  accessToken: string | null;
  refreshToken: string | null;
}) {
  accessToken = tokens.accessToken;
  refreshToken = tokens.refreshToken;
  if (typeof window !== "undefined") {
    if (accessToken) localStorage.setItem("vednex.accessToken", accessToken);
    else localStorage.removeItem("vednex.accessToken");
    if (refreshToken) localStorage.setItem("vednex.refreshToken", refreshToken);
    else localStorage.removeItem("vednex.refreshToken");
  }
}

export function loadStoredTokens() {
  if (typeof window === "undefined") return;
  accessToken = localStorage.getItem("vednex.accessToken");
  refreshToken = localStorage.getItem("vednex.refreshToken");
}

export function getRefreshToken() {
  return refreshToken;
}

function requestPath(url?: string) {
  if (!url) return "";
  try {
    return new URL(url, API_BASE_URL).pathname;
  } catch {
    return url.split("?")[0] ?? "";
  }
}

function isPublicAuthRequest(url?: string) {
  return PUBLIC_AUTH_PATHS.has(requestPath(url));
}

function removeAuthorizationHeader(headers: unknown) {
  if (!headers) return;
  const maybeAxiosHeaders = headers as { delete?: (header: string) => void };
  if (typeof maybeAxiosHeaders.delete === "function") {
    maybeAxiosHeaders.delete("Authorization");
    return;
  }
  delete (headers as Record<string, unknown>).Authorization;
  delete (headers as Record<string, unknown>).authorization;
}

api.interceptors.request.use((config) => {
  if (isPublicAuthRequest(config.url)) {
    removeAuthorizationHeader(config.headers);
  } else if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const original = error.config;
    if (
      error.response?.status === 401 &&
      original &&
      refreshToken &&
      !isPublicAuthRequest(original.url)
    ) {
      try {
        const response = await axios.post<ApiResponse<AuthResponse>>(
          `${API_BASE_URL}/api/auth/refresh`,
          { refreshToken },
        );
        setTokens({
          accessToken: response.data.data.accessToken,
          refreshToken: response.data.data.refreshToken,
        });
        original.headers.Authorization = `Bearer ${response.data.data.accessToken}`;
        return api(original);
      } catch (refreshError) {
        setTokens({ accessToken: null, refreshToken: null });
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  },
);

export function apiErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? error.message;
  }
  return "Something went wrong";
}

export function apiErrorCode(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.code;
  }
  return undefined;
}
