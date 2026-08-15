"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { ProtectedShell } from "@/components/auth/protected-shell";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { api, apiErrorMessage } from "@/lib/api";

type Company = {
  name: string;
  legalName?: string;
  email: string;
  phone?: string;
  country: string;
  timezone: string;
  currency: string;
};

type Settings = {
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  gstNumber?: string;
  taxNumber?: string;
  language?: string;
  dateFormat?: string;
  logoUrl?: string;
};

export default function CompanySettingsPage() {
  const [message, setMessage] = useState<string | null>(null);
  const companyForm = useForm<Company>();
  const settingsForm = useForm<Settings>();
  const company = useQuery({
    queryKey: ["company"],
    queryFn: async () => (await api.get<{ data: Company }>("/api/company")).data.data,
  });
  const settings = useQuery({
    queryKey: ["company-settings"],
    queryFn: async () => (await api.get<{ data: Settings }>("/api/company/settings")).data.data,
  });

  useEffect(() => {
    if (company.data) companyForm.reset(company.data);
  }, [company.data, companyForm]);
  useEffect(() => {
    if (settings.data) settingsForm.reset(settings.data);
  }, [settings.data, settingsForm]);

  const saveCompany = useMutation({
    mutationFn: async (values: Company) => api.put("/api/company", values),
    onSuccess: () => setMessage("Company updated"),
    onError: (error) => setMessage(apiErrorMessage(error)),
  });
  const saveSettings = useMutation({
    mutationFn: async (values: Settings) => api.put("/api/company/settings", values),
    onSuccess: () => setMessage("Settings updated"),
    onError: (error) => setMessage(apiErrorMessage(error)),
  });

  return (
    <ProtectedShell>
      <div className="mb-6">
        <h1 className="text-2xl font-semibold">Company settings</h1>
        <p className="mt-1 text-sm text-zinc-600">Company profile, address, tax, and localization.</p>
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <form className="grid gap-3" onSubmit={companyForm.handleSubmit((values) => saveCompany.mutate(values))}>
            <Input placeholder="Company name" {...companyForm.register("name", { required: true })} />
            <Input placeholder="Legal name" {...companyForm.register("legalName")} />
            <Input placeholder="Email" type="email" {...companyForm.register("email", { required: true })} />
            <Input placeholder="Phone" {...companyForm.register("phone")} />
            <div className="grid gap-3 sm:grid-cols-3">
              <Input placeholder="Country" {...companyForm.register("country", { required: true })} />
              <Input placeholder="Timezone" {...companyForm.register("timezone", { required: true })} />
              <Input placeholder="Currency" {...companyForm.register("currency", { required: true })} />
            </div>
            <Button disabled={saveCompany.isPending} type="submit">Save company</Button>
          </form>
        </Card>
        <Card>
          <form className="grid gap-3" onSubmit={settingsForm.handleSubmit((values) => saveSettings.mutate(values))}>
            <Input placeholder="Logo URL" {...settingsForm.register("logoUrl")} />
            <Input placeholder="Address line 1" {...settingsForm.register("addressLine1")} />
            <Input placeholder="Address line 2" {...settingsForm.register("addressLine2")} />
            <div className="grid gap-3 sm:grid-cols-2">
              <Input placeholder="City" {...settingsForm.register("city")} />
              <Input placeholder="State" {...settingsForm.register("state")} />
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <Input placeholder="Postal code" {...settingsForm.register("postalCode")} />
              <Input placeholder="GST number" {...settingsForm.register("gstNumber")} />
            </div>
            <Input placeholder="Tax number" {...settingsForm.register("taxNumber")} />
            <div className="grid gap-3 sm:grid-cols-2">
              <Input placeholder="Language" {...settingsForm.register("language")} />
              <Input placeholder="Date format" {...settingsForm.register("dateFormat")} />
            </div>
            <Button disabled={saveSettings.isPending} type="submit">Save settings</Button>
          </form>
        </Card>
      </div>
      {message ? <p className="mt-4 text-sm text-zinc-600">{message}</p> : null}
    </ProtectedShell>
  );
}
