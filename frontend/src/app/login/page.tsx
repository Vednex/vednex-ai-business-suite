"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AuthCard } from "@/components/auth/auth-card";
import { useAuth } from "@/components/auth/auth-provider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { apiErrorMessage } from "@/lib/api";

const schema = z.object({
  email: z.email("Enter a valid email"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

type LoginValues = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<LoginValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: LoginValues) {
    setError(null);
    try {
      await login(values.email, values.password);
      router.push("/dashboard");
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  return (
    <AuthCard
      title="Log in"
      subtitle="Access your company workspace with your verified account."
      footer={
        <div className="flex justify-between">
          <Link href="/register">Create account</Link>
          <Link href="/forgot-password">Forgot password</Link>
        </div>
      }
    >
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
        <label className="block text-sm font-medium">
          Email
          <Input className="mt-1" type="email" {...form.register("email")} />
          <span className="text-xs text-red-600">{form.formState.errors.email?.message}</span>
        </label>
        <label className="block text-sm font-medium">
          Password
          <Input className="mt-1" type="password" {...form.register("password")} />
          <span className="text-xs text-red-600">{form.formState.errors.password?.message}</span>
        </label>
        {error ? <p className="text-sm text-red-600">{error}</p> : null}
        <Button className="w-full" disabled={form.formState.isSubmitting} type="submit">
          Log in
        </Button>
      </form>
    </AuthCard>
  );
}
