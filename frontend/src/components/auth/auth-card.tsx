import Link from "next/link";
import { ReactNode } from "react";
import { Card } from "@/components/ui/card";

export function AuthCard({
  title,
  subtitle,
  children,
  footer,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-zinc-50 px-4 py-10 text-zinc-950">
      <div className="w-full max-w-md">
        <Link className="mb-6 block text-center text-sm font-semibold" href="/">
          Vednex AI Business Suite
        </Link>
        <Card>
          <div className="mb-5">
            <h1 className="text-xl font-semibold">{title}</h1>
            <p className="mt-1 text-sm leading-6 text-zinc-600">{subtitle}</p>
          </div>
          {children}
          {footer ? <div className="mt-5 text-sm text-zinc-600">{footer}</div> : null}
        </Card>
      </div>
    </main>
  );
}
