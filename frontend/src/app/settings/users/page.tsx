"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useAuth } from "@/components/auth/auth-provider";
import { ProtectedShell } from "@/components/auth/protected-shell";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { api, apiErrorMessage } from "@/lib/api";

type PageData<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
};
type CompanyUser = {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  profileImageUrl: string | null;
  status: string;
  userStatus: string;
  membershipStatus: string;
  roles: string[];
};
type Invitation = { id: string; email: string; roleId: string; status: string; expiresAt: string };
type Role = { id: string; name: string };
type InviteForm = { email: string; roleId: string };

export default function UsersSettingsPage() {
  const { hasPermission } = useAuth();
  const [message, setMessage] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [page, setPage] = useState(0);
  const pageSize = 10;
  const inviteForm = useForm<InviteForm>();
  const users = useQuery({
    queryKey: ["company-users", debouncedSearch, page, pageSize],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        size: String(pageSize),
      });
      if (debouncedSearch.trim()) {
        params.set("search", debouncedSearch.trim());
      }
      return (await api.get<{ data: PageData<CompanyUser> }>(`/api/company/users?${params.toString()}`)).data.data;
    },
  });
  const invitations = useQuery({
    queryKey: ["company-invitations"],
    queryFn: async () =>
      (await api.get<{ data: PageData<Invitation> }>("/api/company/invitations?size=20")).data.data,
  });
  const roles = useQuery({
    queryKey: ["roles"],
    queryFn: async () => (await api.get<{ data: Role[] }>("/api/roles")).data.data,
  });

  const invite = useMutation({
    mutationFn: async (values: InviteForm) => api.post("/api/company/invitations", values),
    onSuccess: async () => {
      setMessage("Invitation sent");
      inviteForm.reset();
      await invitations.refetch();
    },
    onError: (error) => setMessage(apiErrorMessage(error)),
  });

  const updateStatus = useMutation({
    mutationFn: async ({ userId, status }: { userId: string; status: string }) =>
      api.put(`/api/company/users/${userId}/status`, { status }),
    onSuccess: async () => {
      setMessage("User status updated");
      await users.refetch();
    },
    onError: (error) => setMessage(apiErrorMessage(error)),
  });

  useEffect(() => {
    const timeout = window.setTimeout(() => setDebouncedSearch(search), 350);
    return () => window.clearTimeout(timeout);
  }, [search]);

  const visibleUsers = users.data?.content.filter((user) => user.membershipStatus !== "INVITED") ?? [];
  const pendingInvitations = invitations.data?.content.filter((invitation) => invitation.status === "PENDING") ?? [];
  const totalPages = users.data?.totalPages ?? 0;
  const currentPage = users.data?.number ?? page;
  const canUpdateStatus = hasPermission("USER_DISABLE");

  return (
    <ProtectedShell>
      <div className="mb-6">
        <h1 className="text-2xl font-semibold">Users</h1>
        <p className="mt-1 text-sm text-zinc-600">Invite users, review memberships, and manage account status.</p>
      </div>
      <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
        <Card>
          <div className="mb-4 flex gap-3">
            <Input
              placeholder="Search name or email"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(0);
              }}
            />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b text-xs uppercase text-zinc-500">
                <tr><th className="py-2">User</th><th>Status</th><th>Roles</th><th>Action</th></tr>
              </thead>
              <tbody>
                {users.isLoading ? (
                  <tr>
                    <td className="py-6 text-sm text-zinc-500" colSpan={4}>Loading users...</td>
                  </tr>
                ) : null}
                {!users.isLoading && users.isError ? (
                  <tr>
                    <td className="py-6 text-sm text-red-600" colSpan={4}>Unable to load company users.</td>
                  </tr>
                ) : null}
                {!users.isLoading && !users.isError ? visibleUsers.map((user) => (
                  <tr className="border-b border-zinc-100" key={user.userId}>
                    <td className="py-3">
                      <div className="flex items-center gap-3">
                        {user.profileImageUrl ? (
                          // eslint-disable-next-line @next/next/no-img-element
                          <img alt="" className="h-9 w-9 rounded-full object-cover" src={user.profileImageUrl} />
                        ) : (
                          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-zinc-100 text-xs font-semibold text-zinc-700">
                            {initials(user)}
                          </span>
                        )}
                        <div>
                          <p className="font-medium">{user.firstName} {user.lastName}</p>
                          <p className="text-zinc-500">{user.email}</p>
                        </div>
                      </div>
                    </td>
                    <td><StatusBadge status={user.membershipStatus} /></td>
                    <td>{user.roles.length > 0 ? user.roles.join(", ") : "-"}</td>
                    <td>
                      {canUpdateStatus ? (
                        <Button
                          disabled={updateStatus.isPending}
                          onClick={() => updateStatus.mutate({ userId: user.userId, status: user.membershipStatus === "ACTIVE" ? "SUSPENDED" : "ACTIVE" })}
                          type="button"
                          variant="secondary"
                        >
                          {user.membershipStatus === "ACTIVE" ? "Suspend" : "Activate"}
                        </Button>
                      ) : (
                        <span className="text-zinc-400">-</span>
                      )}
                    </td>
                  </tr>
                )) : null}
              </tbody>
            </table>
            {!users.isLoading && !users.isError && visibleUsers.length === 0 ? (
              <p className="py-6 text-sm text-zinc-500">{debouncedSearch.trim() ? "No users match your search." : "No users found."}</p>
            ) : null}
          </div>
          {totalPages > 1 ? <div className="mt-4 flex items-center justify-between gap-3 border-t border-zinc-100 pt-4 text-sm text-zinc-600">
            <span>
              Page {totalPages === 0 ? 0 : currentPage + 1} of {totalPages} - {users.data?.totalElements ?? 0} users
            </span>
            <div className="flex gap-2">
              <Button
                disabled={users.isFetching || currentPage <= 0}
                onClick={() => setPage((value) => Math.max(value - 1, 0))}
                type="button"
                variant="secondary"
              >
                Previous
              </Button>
              <Button
                disabled={users.isFetching || totalPages === 0 || currentPage >= totalPages - 1}
                onClick={() => setPage((value) => value + 1)}
                type="button"
                variant="secondary"
              >
                Next
              </Button>
            </div>
          </div> : null}
        </Card>
        <div className="space-y-4">
          <Card>
            <h2 className="mb-3 text-base font-semibold">Invite user</h2>
            <form className="grid gap-3" onSubmit={inviteForm.handleSubmit((values) => invite.mutate(values))}>
              <Input placeholder="Email" type="email" {...inviteForm.register("email", { required: true })} />
              <Select {...inviteForm.register("roleId", { required: true })}>
                <option value="">Select role</option>
                {roles.data?.map((role) => <option key={role.id} value={role.id}>{role.name}</option>)}
              </Select>
              <Button disabled={invite.isPending} type="submit">Send invitation</Button>
            </form>
          </Card>
          <Card>
            <h2 className="mb-3 text-base font-semibold">Pending invitations</h2>
            <div className="space-y-3 text-sm">
              {pendingInvitations.map((invitation) => (
                <div className="border-b border-zinc-100 pb-2" key={invitation.id}>
                  <p className="font-medium">{invitation.email}</p>
                  <p className="text-zinc-500">{invitation.status}</p>
                </div>
              ))}
              {pendingInvitations.length === 0 ? <p className="text-zinc-500">No pending invitations.</p> : null}
            </div>
          </Card>
        </div>
      </div>
      {message ? <p className="mt-4 text-sm text-zinc-600">{message}</p> : null}
    </ProtectedShell>
  );
}

function initials(user: CompanyUser) {
  const first = user.firstName?.trim().charAt(0) ?? "";
  const last = user.lastName?.trim().charAt(0) ?? "";
  const value = `${first}${last}`.trim();
  return value ? value.toUpperCase() : user.email.charAt(0).toUpperCase();
}

function StatusBadge({ status }: { status: string }) {
  const active = status === "ACTIVE";
  const suspended = status === "SUSPENDED";
  return (
    <span className={`inline-flex rounded-full px-2 py-1 text-xs font-medium ${
      active
        ? "bg-emerald-50 text-emerald-700"
        : suspended
          ? "bg-amber-50 text-amber-700"
          : "bg-zinc-100 text-zinc-700"
    }`}>
      {status}
    </span>
  );
}
