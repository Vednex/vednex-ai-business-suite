import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";

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

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const AUTH_STORAGE_KEYS = [
  "vednex.accessToken",
  "vednex.refreshToken",
  "vednex.user",
  "vednex.company",
  "vednex.roles",
  "vednex.permissions",
  "vednex.session",
];

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
let refreshPromise: Promise<AuthResponse> | null = null;
let authFailureHandler: (() => void) | null = null;

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

export function clearStoredAuth() {
  accessToken = null;
  refreshToken = null;
  if (typeof window !== "undefined") {
    for (const key of AUTH_STORAGE_KEYS) {
      localStorage.removeItem(key);
    }
  }
}

export function setTokens(tokens: {
  accessToken: string | null;
  refreshToken: string | null;
}) {
  if (!tokens.accessToken && !tokens.refreshToken) {
    clearStoredAuth();
    return;
  }
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

export function setAuthFailureHandler(handler: (() => void) | null) {
  authFailureHandler = handler;
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

function setAuthorizationHeader(config: InternalAxiosRequestConfig, token: string) {
  const maybeAxiosHeaders = config.headers as { set?: (header: string, value: string) => void };
  if (typeof maybeAxiosHeaders.set === "function") {
    maybeAxiosHeaders.set("Authorization", `Bearer ${token}`);
    return;
  }
  config.headers.Authorization = `Bearer ${token}`;
}

function responseData<T>(response: ApiResponse<T>) {
  if (!response.success || !response.data) {
    throw new Error(response.message || "API request failed");
  }
  return response.data;
}

export async function refreshStoredTokens() {
  if (!refreshToken) {
    clearStoredAuth();
    throw new Error("No refresh token is available");
  }
  if (!refreshPromise) {
    const token = refreshToken;
    refreshPromise = axios
      .post<ApiResponse<AuthResponse>>(
        `${API_BASE_URL}/api/auth/refresh`,
        { refreshToken: token },
        { headers: { "Content-Type": "application/json", Accept: "application/json" } },
      )
      .then((response) => {
        const auth = responseData(response.data);
        setTokens({
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
        });
        return auth;
      })
      .catch((error) => {
        clearStoredAuth();
        authFailureHandler?.();
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.request.use((config) => {
  if (isPublicAuthRequest(config.url)) {
    removeAuthorizationHeader(config.headers);
  } else if (accessToken) {
    setAuthorizationHeader(config, accessToken);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const original = error.config as RetryableRequestConfig | undefined;
    if (
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      refreshToken &&
      !isPublicAuthRequest(original.url)
    ) {
      original._retry = true;
      try {
        const response = await refreshStoredTokens();
        setAuthorizationHeader(original, response.accessToken);
        return api(original);
      } catch (refreshError) {
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  },
);

export function apiErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response) {
      const status = error.response.status;
      const backendMessage = error.response.data?.message;
      if (error.response.data?.code === "INVALID_CREDENTIALS") {
        return "Invalid email or password";
      }
      return backendMessage ? `${status}: ${backendMessage}` : `${status}: ${error.message}`;
    }
    if (error.request) {
      return `${error.message}. Check that NEXT_PUBLIC_API_URL points to ${API_BASE_URL} and the API gateway is running.`;
    }
    return error.message;
  }
  return "Something went wrong";
}

export function apiErrorCode(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.code;
  }
  return undefined;
}
