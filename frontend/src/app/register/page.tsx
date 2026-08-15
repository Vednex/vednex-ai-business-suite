"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AuthCard } from "@/components/auth/auth-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, apiErrorCode, apiErrorMessage } from "@/lib/api";

const schema = z
  .object({
    companyName: z.string().min(2, "Company name is required"),
    firstName: z.string().min(1, "First name is required"),
    lastName: z.string().min(1, "Last name is required"),
    email: z.email("Enter a valid email"),
    password: z.string().min(8, "Password must be at least 8 characters"),
    confirmPassword: z.string().min(8, "Confirm your password"),
    country: z.string().min(2, "Country is required"),
    timezone: z.string().min(2, "Timezone is required"),
    terms: z.boolean().refine((value) => value, "Accept the terms to continue"),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

type RegisterValues = z.infer<typeof schema>;

export default function RegisterPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pendingEmail, setPendingEmail] = useState<string | null>(null);
  const [existingEmail, setExistingEmail] = useState<string | null>(null);
  const [resendMessage, setResendMessage] = useState<string | null>(null);
  const form = useForm<RegisterValues>({
    resolver: zodResolver(schema),
    defaultValues: { country: "India", timezone: "Asia/Kolkata", terms: false },
  });

  async function onSubmit(values: RegisterValues) {
    setError(null);
    setPendingEmail(null);
    setExistingEmail(null);
    setResendMessage(null);
    try {
      await api.post("/api/auth/register", {
        companyName: values.companyName,
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        password: values.password,
        country: values.country,
        timezone: values.timezone,
      });
      router.push("/onboarding");
    } catch (requestError) {
      const code = apiErrorCode(requestError);
      if (code === "EMAIL_VERIFICATION_PENDING") {
        setPendingEmail(values.email);
        return;
      }
      if (code === "ACCOUNT_ALREADY_EXISTS") {
        setExistingEmail(values.email);
        setError(apiErrorMessage(requestError));
        return;
      }
      setError(apiErrorMessage(requestError));
    }
  }

  async function resendVerification() {
    if (!pendingEmail) return;
    setResendMessage(null);
    try {
      const response = await api.post<{ message: string }>("/api/auth/resend-verification", { email: pendingEmail });
      setResendMessage(response.data.message);
    } catch (requestError) {
      setResendMessage(apiErrorMessage(requestError));
    }
  }

  return (
    <AuthCard
      title="Create company account"
      subtitle="Register your company and owner account. You will verify email before login."
      footer={<Link href="/login">Already have an account?</Link>}
    >
      <form className="grid gap-4" onSubmit={form.handleSubmit(onSubmit)}>
        <Input placeholder="Company name" {...form.register("companyName")} />
        <div className="grid gap-3 sm:grid-cols-2">
          <Input placeholder="First name" {...form.register("firstName")} />
          <Input placeholder="Last name" {...form.register("lastName")} />
        </div>
        <Input placeholder="Email" type="email" {...form.register("email")} />
        <div className="grid gap-3 sm:grid-cols-2">
          <Input placeholder="Password" type="password" {...form.register("password")} />
          <Input placeholder="Confirm password" type="password" {...form.register("confirmPassword")} />
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <Input placeholder="Country" {...form.register("country")} />
          <Input placeholder="Timezone" {...form.register("timezone")} />
        </div>
        <label className="flex items-start gap-2 text-sm text-zinc-700">
          <input className="mt-1" type="checkbox" {...form.register("terms")} />
          I accept the terms for creating this workspace.
        </label>
        <div className="space-y-1 text-xs text-red-600">
          {Object.values(form.formState.errors).map((fieldError, index) => (
            <p key={index}>{fieldError.message}</p>
          ))}
          {error ? <p>{error}</p> : null}
        </div>
        {pendingEmail ? (
          <div className="space-y-3 text-sm text-zinc-700">
            <p>Your account has already been created, but your email still needs verification.</p>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button onClick={resendVerification} type="button">
                Resend verification email
              </Button>
              <Link
                className="inline-flex h-10 items-center justify-center rounded-md border border-zinc-300 px-4 text-sm font-medium text-zinc-900 hover:bg-zinc-100"
                href={`/resend-verification?email=${encodeURIComponent(pendingEmail)}`}
              >
                Go to resend page
              </Link>
            </div>
            <Link className="font-medium text-zinc-950 underline" href="/login">
              Back to login
            </Link>
            {resendMessage ? <p className="text-zinc-600">{resendMessage}</p> : null}
          </div>
        ) : null}
        {existingEmail ? (
          <div className="text-sm text-zinc-700">
            <Link className="font-medium text-zinc-950 underline" href={`/login?email=${encodeURIComponent(existingEmail)}`}>
              Back to login
            </Link>
          </div>
        ) : null}
        {!pendingEmail ? (
          <Button disabled={form.formState.isSubmitting} type="submit">
            Register
          </Button>
        ) : null}
      </form>
    </AuthCard>
  );
}
