import Link from "next/link";
import { Building2, LogIn, UserPlus } from "lucide-react";

export default function Home() {
  return (
    <main className="flex min-h-screen bg-zinc-50 text-zinc-950">
      <section className="mx-auto flex w-full max-w-5xl flex-col justify-center gap-8 px-6 py-10">
        <div className="max-w-2xl">
          <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-zinc-950 text-white">
            <Building2 className="h-6 w-6" />
          </div>
          <h1 className="text-4xl font-semibold">Vednex AI Business Suite</h1>
          <p className="mt-3 text-base leading-7 text-zinc-600">
            Identity, company management, RBAC, and tenant-safe workspace access for the business suite foundation.
          </p>
        </div>
        <div className="flex flex-col gap-3 sm:flex-row">
          <Link className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-zinc-950 px-4 text-sm font-medium text-white" href="/login">
            <LogIn className="h-4 w-4" />
            Login
          </Link>
          <Link className="inline-flex h-10 items-center justify-center gap-2 rounded-md border border-zinc-300 bg-white px-4 text-sm font-medium" href="/register">
            <UserPlus className="h-4 w-4" />
            Register company
          </Link>
        </div>
      </section>
    </main>
  );
}
