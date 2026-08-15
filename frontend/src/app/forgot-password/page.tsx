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

const schema = z.object({ email: z.email("Enter a valid email") });
type Values = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const [message, setMessage] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema) });

  async function onSubmit(values: Values) {
    try {
      await api.post("/api/auth/forgot-password", values);
      setMessage("Password reset email queued if the account exists.");
    } catch (error) {
      setMessage(apiErrorMessage(error));
    }
  }

  return (
    <AuthCard title="Reset password" subtitle="Enter your account email to receive a reset link." footer={<Link href="/login">Back to login</Link>}>
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
        <Input placeholder="Email" type="email" {...form.register("email")} />
        <p className="text-sm text-zinc-600">{message ?? form.formState.errors.email?.message}</p>
        <Button disabled={form.formState.isSubmitting} type="submit">Send reset link</Button>
      </form>
    </AuthCard>
  );
}
