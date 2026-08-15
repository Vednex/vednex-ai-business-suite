"use client";

import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AuthCard } from "@/components/auth/auth-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, apiErrorMessage } from "@/lib/api";

const schema = z.object({
  firstName: z.string().optional(),
  lastName: z.string().optional(),
  password: z.string().optional(),
});
type Values = z.infer<typeof schema>;

export default function AcceptInvitationPage() {
  const [message, setMessage] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema) });

  async function onSubmit(values: Values) {
    const token = new URLSearchParams(window.location.search).get("token");
    if (!token) {
      setMessage("Invitation token is missing.");
      return;
    }
    try {
      await api.post(`/api/company/invitations/${token}/accept`, values);
      setMessage("Invitation accepted. You can now log in.");
    } catch (error) {
      setMessage(apiErrorMessage(error));
    }
  }

  return (
    <AuthCard title="Accept invitation" subtitle="Existing users can submit directly. New users should enter account details." footer={<Link href="/login">Go to login</Link>}>
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
        <Input placeholder="First name" {...form.register("firstName")} />
        <Input placeholder="Last name" {...form.register("lastName")} />
        <Input placeholder="Password for new account" type="password" {...form.register("password")} />
        <p className="text-sm text-zinc-600">{message}</p>
        <Button disabled={form.formState.isSubmitting} type="submit">Accept invitation</Button>
      </form>
    </AuthCard>
  );
}
