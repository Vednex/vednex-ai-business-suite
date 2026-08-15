"use client";

import { useQuery } from "@tanstack/react-query";
import { ProtectedShell } from "@/components/auth/protected-shell";
import { Card } from "@/components/ui/card";
import { api } from "@/lib/api";

type Summary = {
  companyName: string;
  currentPlan: string;
  trialExpiry: string;
  totalCompanyUsers: number;
  activeUsers: number;
  pendingInvitations: number;
};

export default function DashboardPage() {
  const { data } = useQuery({
    queryKey: ["dashboard-summary"],
    queryFn: async () =>
      (await api.get<{ data: Summary }>("/api/company/dashboard-summary")).data.data,
  });

  const cards = [
    ["Company", data?.companyName ?? "-"],
    ["Current plan", data?.currentPlan ?? "-"],
    ["Trial expiry", data?.trialExpiry ? new Date(data.trialExpiry).toLocaleDateString() : "-"],
    ["Total users", data?.totalCompanyUsers ?? 0],
    ["Active users", data?.activeUsers ?? 0],
    ["Pending invitations", data?.pendingInvitations ?? 0],
  ];

  return (
    <ProtectedShell>
      <div className="mb-6">
        <h1 className="text-2xl font-semibold">Dashboard</h1>
        <p className="mt-1 text-sm text-zinc-600">Workspace status and account foundation.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {cards.map(([label, value]) => (
          <Card key={label}>
            <p className="text-sm text-zinc-500">{label}</p>
            <p className="mt-2 text-2xl font-semibold">{value}</p>
          </Card>
        ))}
      </div>
    </ProtectedShell>
  );
}
