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

const schema = z.object({ newPassword: z.string().min(8, "Password must be at least 8 characters") });
type Values = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  const [message, setMessage] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema) });

  async function onSubmit(values: Values) {
    const token = new URLSearchParams(window.location.search).get("token");
    if (!token) {
      setMessage("Reset token is missing.");
      return;
    }
    try {
      await api.post("/api/auth/reset-password", { token, newPassword: values.newPassword });
      setMessage("Password reset successful. You can now log in.");
    } catch (error) {
      setMessage(apiErrorMessage(error));
    }
  }

  return (
    <AuthCard title="Choose new password" subtitle="Reset links are one-time use and expire automatically." footer={<Link href="/login">Back to login</Link>}>
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
        <Input placeholder="New password" type="password" {...form.register("newPassword")} />
        <p className="text-sm text-zinc-600">{message ?? form.formState.errors.newPassword?.message}</p>
        <Button disabled={form.formState.isSubmitting} type="submit">Reset password</Button>
      </form>
    </AuthCard>
  );
}
