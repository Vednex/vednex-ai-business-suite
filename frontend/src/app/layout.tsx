import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Vednex AI Business Suite",
  description: "Enterprise SaaS platform foundation for Vednex AI Business Suite.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
