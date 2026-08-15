"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AuthCard } from "@/components/auth/auth-card";
import { api, apiErrorMessage } from "@/lib/api";

export default function VerifyEmailPage() {
  const [message, setMessage] = useState(() => {
    if (typeof window === "undefined") return "Verifying email";
    return new URLSearchParams(window.location.search).get("token")
      ? "Verifying email"
      : "Verification token is missing.";
  });

  useEffect(() => {
    const token = new URLSearchParams(window.location.search).get("token");
    if (!token) {
      return;
    }
    api
      .post("/api/auth/verify-email", { token })
      .then(() => setMessage("Email verified. You can now log in."))
      .catch((error) => setMessage(apiErrorMessage(error)));
  }, []);

  return (
    <AuthCard title="Email verification" subtitle={message} footer={<Link href="/login">Go to login</Link>}>
      <div className="h-1 rounded-full bg-zinc-200">
        <div className="h-1 w-1/2 rounded-full bg-zinc-950" />
      </div>
    </AuthCard>
  );
}
