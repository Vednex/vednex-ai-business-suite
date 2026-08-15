"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AuthCard } from "@/components/auth/auth-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, apiErrorMessage } from "@/lib/api";

const schema = z.object({ email: z.email("Enter a valid email") });
type Values = z.infer<typeof schema>;

export default function ResendVerificationPage() {
  const [message, setMessage] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema) });

  useEffect(() => {
    const email = new URLSearchParams(window.location.search).get("email");
    if (email) {
      form.setValue("email", email);
    }
  }, [form]);

  async function onSubmit(values: Values) {
    try {
      await api.post("/api/auth/resend-verification", values);
      setMessage("If your account requires verification, a verification email has been sent.");
    } catch (error) {
      setMessage(apiErrorMessage(error));
    }
  }

  return (
    <AuthCard title="Resend verification" subtitle="Request a fresh email verification link." footer={<Link href="/login">Back to login</Link>}>
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
        <Input placeholder="Email" type="email" {...form.register("email")} />
        <p className="text-sm text-zinc-600">{message ?? form.formState.errors.email?.message}</p>
        <Button disabled={form.formState.isSubmitting} type="submit">Send</Button>
      </form>
    </AuthCard>
  );
}
