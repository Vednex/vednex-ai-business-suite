import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  reactCompiler: true,
  experimental: {
    cpus: 1,
    workerThreads: false,
    staticGenerationMaxConcurrency: 1,
    staticGenerationMinPagesPerWorker: 1,
  },
  typescript: {
    // The build script runs `tsc --noEmit` explicitly before `next build`.
    // This avoids a Windows EPERM failure in Next's internal type-check worker.
    ignoreBuildErrors: true,
  },
};

export default nextConfig;
