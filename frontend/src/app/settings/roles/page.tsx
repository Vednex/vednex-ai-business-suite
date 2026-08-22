"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { ProtectedShell } from "@/components/auth/protected-shell";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { api, apiErrorMessage } from "@/lib/api";

type Role = {
  id: string;
  name: string;
  description?: string;
  protectedSystemRole: boolean;
  permissions: string[];
};
type Permission = { id: string; code: string; description?: string };
type RoleForm = { name: string; description?: string; permissions: Record<string, boolean> };

function rolePayload(values: RoleForm) {
  return {
    name: values.name.trim().toUpperCase().replace(/\s+/g, "_"),
    description: values.description,
    permissions: Object.entries(values.permissions ?? {})
      .filter(([, enabled]) => enabled)
      .map(([code]) => code),
  };
}

export default function RolesSettingsPage() {
  const [message, setMessage] = useState<string | null>(null);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const form = useForm<RoleForm>();
  const editForm = useForm<RoleForm>();
  const roles = useQuery({
    queryKey: ["roles"],
    queryFn: async () => (await api.get<{ data: Role[] }>("/api/roles")).data.data,
  });
  const permissions = useQuery({
    queryKey: ["permissions"],
    queryFn: async () => (await api.get<{ data: Permission[] }>("/api/roles/permissions")).data.data,
  });

  const createRole = useMutation({
    mutationFn: async (values: RoleForm) => api.post("/api/roles", rolePayload(values)),
    onSuccess: async () => {
      setMessage("Role created");
      form.reset();
      await roles.refetch();
    },
    onError: (error) => setMessage(apiErrorMessage(error)),
  });

  const updateRole = useMutation({
    mutationFn: async ({ roleId, values }: { roleId: string; values: RoleForm }) =>
      api.put(`/api/roles/${roleId}`, rolePayload(values)),
    onSuccess: async () => {
      setMessage("Role updated");
      setEditingRole(null);
      editForm.reset();
      await roles.refetch();
    },
    onError: (error) => setMessage(apiErrorMessage(error)),
  });

  const deleteRole = useMutation({
    mutationFn: async (roleId: string) => api.delete(`/api/roles/${roleId}`),
    onSuccess: async () => {
      setMessage("Role deleted");
      setEditingRole(null);
      editForm.reset();
      await roles.refetch();
    },
    onError: (error) => setMessage(apiErrorMessage(error)),
  });

  function confirmDelete(role: Role) {
    if (!window.confirm(`Delete role ${role.name}?`)) {
      return;
    }
    deleteRole.mutate(role.id);
  }

  function startEditing(role: Role) {
    setEditingRole(role);
    editForm.reset({
      name: role.name,
      description: role.description ?? "",
      permissions: Object.fromEntries(role.permissions.map((permission) => [permission, true])),
    });
  }

  return (
    <ProtectedShell>
      <div className="mb-6">
        <h1 className="text-2xl font-semibold">Roles</h1>
        <p className="mt-1 text-sm text-zinc-600">Create roles, edit permissions, and manage role definitions.</p>
      </div>
      <div className="grid gap-4 lg:grid-cols-[1fr_420px]">
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b text-xs uppercase text-zinc-500">
                <tr><th className="py-2">Role</th><th>Type</th><th>Permissions</th><th>Action</th></tr>
              </thead>
              <tbody>
                {roles.data?.map((role) => (
                  <tr className="border-b border-zinc-100" key={role.id}>
                    <td className="py-3">
                      <p className="font-medium">{role.name}</p>
                      <p className="text-zinc-500">{role.description}</p>
                    </td>
                    <td>{role.protectedSystemRole ? "System" : "Custom"}</td>
                    <td>{role.permissions.length}</td>
                    <td>
                      <div className="flex flex-wrap gap-2">
                        <Button onClick={() => startEditing(role)} type="button" variant="secondary">Edit</Button>
                        <Button
                          disabled={role.protectedSystemRole || deleteRole.isPending}
                          onClick={() => confirmDelete(role)}
                          type="button"
                          variant="danger"
                        >
                          Delete
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
        <Card>
          <h2 className="mb-3 text-base font-semibold">{editingRole ? "Edit role" : "Create role"}</h2>
          {editingRole ? (
            <form
              className="grid gap-3"
              onSubmit={editForm.handleSubmit((values) => updateRole.mutate({ roleId: editingRole.id, values }))}
            >
              <Input placeholder="ROLE_NAME" {...editForm.register("name", { required: true })} />
              <Input placeholder="Description" {...editForm.register("description")} />
              <div className="max-h-80 space-y-2 overflow-auto rounded-md border border-zinc-200 p-3">
                {permissions.data?.map((permission) => (
                  <label className="flex items-center gap-2 text-sm" key={permission.code}>
                    <input type="checkbox" {...editForm.register(`permissions.${permission.code}`)} />
                    <span>{permission.code}</span>
                  </label>
                ))}
              </div>
              <div className="flex gap-2">
                <Button disabled={updateRole.isPending} type="submit">Save changes</Button>
                <Button
                  onClick={() => {
                    setEditingRole(null);
                    editForm.reset();
                  }}
                  type="button"
                  variant="secondary"
                >
                  Cancel
                </Button>
              </div>
            </form>
          ) : (
            <form className="grid gap-3" onSubmit={form.handleSubmit((values) => createRole.mutate(values))}>
              <Input placeholder="ROLE_NAME" {...form.register("name", { required: true })} />
              <Input placeholder="Description" {...form.register("description")} />
              <div className="max-h-80 space-y-2 overflow-auto rounded-md border border-zinc-200 p-3">
                {permissions.data?.map((permission) => (
                  <label className="flex items-center gap-2 text-sm" key={permission.code}>
                    <input type="checkbox" {...form.register(`permissions.${permission.code}`)} />
                    <span>{permission.code}</span>
                  </label>
                ))}
              </div>
              <Button disabled={createRole.isPending} type="submit">Create role</Button>
            </form>
          )}
        </Card>
      </div>
      {message ? <p className="mt-4 text-sm text-zinc-600">{message}</p> : null}
    </ProtectedShell>
  );
}
