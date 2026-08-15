import Link from "next/link";
import { AuthCard } from "@/components/auth/auth-card";

export default function OnboardingPage() {
  return (
    <AuthCard
      title="Verify your email"
      subtitle="Your company workspace has been created. Check your inbox for the verification link before logging in."
    >
      <div className="flex gap-3">
        <Link className="flex h-10 flex-1 items-center justify-center rounded-md bg-zinc-950 text-sm font-medium text-white" href="/login">
          Go to login
        </Link>
        <Link className="flex h-10 flex-1 items-center justify-center rounded-md border border-zinc-300 text-sm font-medium" href="/resend-verification">
          Resend email
        </Link>
      </div>
    </AuthCard>
  );
}
