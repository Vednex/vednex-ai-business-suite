"use client";

import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import {
  api,
  apiErrorMessage,
  AuthResponse,
  clearStoredAuth,
  getRefreshToken,
  loadStoredTokens,
  refreshStoredTokens,
  setAuthFailureHandler,
  setTokens,
} from "@/lib/api";

type AuthState = {
  user: AuthResponse["user"] | null;
  company: AuthResponse["company"] | null;
  roles: string[];
  permissions: string[];
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: Record<string, unknown>) => Promise<void>;
  logout: () => Promise<void>;
  hasPermission: (permission: string) => boolean;
  refreshSession: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [user, setUser] = useState<AuthResponse["user"] | null>(null);
  const [company, setCompany] = useState<AuthResponse["company"] | null>(null);
  const [roles, setRoles] = useState<string[]>([]);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  const clearAuth = useCallback(() => {
    clearStoredAuth();
    setUser(null);
    setCompany(null);
    setRoles([]);
    setPermissions([]);
    queryClient.clear();
  }, [queryClient]);

  const applyAuth = useCallback(async (response: AuthResponse) => {
    setTokens({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    });
    setUser(response.user);
    setCompany(response.company);
    setRoles(response.roles);
    setPermissions(response.permissions);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    clearAuth();
    try {
      const response = await api.post<{ data: AuthResponse }>("/api/auth/login", {
        email,
        password,
      });
      await applyAuth(response.data.data);
    } catch (error) {
      clearAuth();
      throw error;
    }
  }, [applyAuth, clearAuth]);

  const register = useCallback(async (payload: Record<string, unknown>) => {
    await api.post("/api/auth/register", payload);
  }, []);

  const refreshSession = useCallback(async () => {
    const token = getRefreshToken();
    if (!token) {
      clearAuth();
      return;
    }
    try {
      await applyAuth(await refreshStoredTokens());
    } catch {
      clearAuth();
    }
  }, [applyAuth, clearAuth]);

  const logout = useCallback(async () => {
    const token = getRefreshToken();
    try {
      if (token) {
        await api.post("/api/auth/logout", { refreshToken: token });
      }
    } catch (error) {
      console.warn(apiErrorMessage(error));
    } finally {
      clearAuth();
      router.push("/login");
    }
  }, [clearAuth, router]);

  const hasPermission = useCallback((permission: string) => permissions.includes(permission), [permissions]);

  useEffect(() => {
    setAuthFailureHandler(() => {
      clearAuth();
      router.replace("/login");
    });
    return () => setAuthFailureHandler(null);
  }, [clearAuth, router]);

  useEffect(() => {
    let cancelled = false;
    loadStoredTokens();
    queueMicrotask(() => {
      if (!cancelled) {
        void refreshSession().finally(() => {
          if (!cancelled) {
            setLoading(false);
          }
        });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [refreshSession]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      company,
      roles,
      permissions,
      loading,
      login,
      register,
      logout,
      refreshSession,
      hasPermission,
    }),
    [user, company, roles, permissions, loading, login, register, logout, refreshSession, hasPermission],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
