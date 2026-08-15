"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { ProtectedShell } from "@/components/auth/protected-shell";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { api, apiErrorMessage } from "@/lib/api";
import { useEffect, useState } from "react";

type Profile = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  profileImageUrl?: string;
  emailVerified: boolean;
  status: string;
};

type ProfileForm = Pick<Profile, "firstName" | "lastName" | "phone" | "profileImageUrl">;

export default function ProfilePage() {
  const [message, setMessage] = useState<string | null>(null);
  const form = useForm<ProfileForm>();
  const { data, refetch } = useQuery({
    queryKey: ["me"],
    queryFn: async () => (await api.get<{ data: Profile }>("/api/users/me")).data.data,
  });

  useEffect(() => {
    if (data) form.reset(data);
  }, [data, form]);

  const update = useMutation({
    mutationFn: async (values: ProfileForm) => api.put("/api/users/me", values),
    onSuccess: async () => {
      setMessage("Profile updated");
      await refetch();
    },
    onError: (error) => setMessage(apiErrorMessage(error)),
  });

  return (
    <ProtectedShell>
      <div className="mb-6">
        <h1 className="text-2xl font-semibold">Profile</h1>
        <p className="mt-1 text-sm text-zinc-600">{data?.email}</p>
      </div>
      <Card className="max-w-2xl">
        <form className="grid gap-4" onSubmit={form.handleSubmit((values) => update.mutate(values))}>
          <div className="grid gap-3 sm:grid-cols-2">
            <Input placeholder="First name" {...form.register("firstName", { required: true })} />
            <Input placeholder="Last name" {...form.register("lastName", { required: true })} />
          </div>
          <Input placeholder="Phone" {...form.register("phone")} />
          <Input placeholder="Profile image URL" {...form.register("profileImageUrl")} />
          {message ? <p className="text-sm text-zinc-600">{message}</p> : null}
          <Button disabled={update.isPending} type="submit">Save profile</Button>
        </form>
      </Card>
    </ProtectedShell>
  );
}
