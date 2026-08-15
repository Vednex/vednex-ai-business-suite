"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ReactNode, useEffect } from "react";
import { Building2, LayoutDashboard, LogOut, Shield, User, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/components/auth/auth-provider";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard, permission: "COMPANY_VIEW" },
  { href: "/profile", label: "Profile", icon: User },
  { href: "/settings/company", label: "Company", icon: Building2, permission: "COMPANY_VIEW" },
  { href: "/settings/users", label: "Users", icon: Users, permission: "USER_VIEW" },
  { href: "/settings/roles", label: "Roles", icon: Shield, permission: "ROLE_VIEW" },
];

export function ProtectedShell({ children }: { children: ReactNode }) {
  const { company, user, loading, hasPermission, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, router, user]);

  if (loading || !user) {
    return <div className="flex min-h-screen items-center justify-center text-sm text-zinc-500">Loading</div>;
  }

  return (
    <div className="min-h-screen bg-zinc-50 text-zinc-950">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-zinc-200 bg-white p-4 md:flex md:flex-col">
        <Link href="/dashboard" className="mb-6 flex flex-col gap-1 px-2">
          <span className="text-sm font-semibold">Vednex AI Business Suite</span>
          <span className="text-xs text-zinc-500">{company?.name ?? "No company"}</span>
        </Link>
        <nav className="flex flex-1 flex-col gap-1">
          {navItems
            .filter((item) => !item.permission || hasPermission(item.permission))
            .map((item) => (
              <Link
                className={cn(
                  "flex h-10 items-center gap-3 rounded-md px-3 text-sm text-zinc-700",
                  pathname === item.href && "bg-zinc-100 font-medium text-zinc-950",
                )}
                href={item.href}
                key={item.href}
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </Link>
            ))}
        </nav>
        <Button className="w-full" onClick={logout} type="button" variant="secondary">
          <LogOut className="h-4 w-4" />
          Logout
        </Button>
      </aside>
      <div className="md:pl-64">
        <header className="sticky top-0 z-10 border-b border-zinc-200 bg-white/95 px-4 py-3 backdrop-blur md:px-8">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-medium">{company?.name}</p>
              <p className="text-xs text-zinc-500">Current company</p>
            </div>
            <div className="flex items-center gap-3 text-sm">
              <span className="hidden text-zinc-600 sm:inline">{user.firstName} {user.lastName}</span>
              <Button onClick={logout} type="button" variant="ghost">
                <LogOut className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </header>
        <main className="px-4 py-6 md:px-8">{children}</main>
      </div>
    </div>
  );
}
