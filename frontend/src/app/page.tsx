export default function Home() {
  return (
    <div className="flex flex-1 bg-zinc-50 text-zinc-950">
      <main className="mx-auto flex w-full max-w-6xl flex-col gap-10 px-6 py-10 sm:px-10">
        <header className="flex flex-col gap-2 border-b border-zinc-200 pb-6">
          <p className="text-sm font-medium uppercase tracking-wide text-zinc-500">
            Milestone 1
          </p>
          <h1 className="text-3xl font-semibold">
            Vednex AI Business Suite
          </h1>
          <p className="max-w-2xl text-base leading-7 text-zinc-600">
            Enterprise SaaS foundation with backend health, public status,
            OpenAPI documentation, PostgreSQL, Flyway, and a production-ready
            Next.js build pipeline.
          </p>
        </header>

        <section className="grid gap-4 md:grid-cols-3">
          {[
            ["Backend", "Spring Boot 4.1, Security, Actuator, OpenAPI"],
            ["Database", "PostgreSQL managed by Docker Compose and Flyway"],
            ["Frontend", "Next.js, TypeScript, Tailwind CSS"],
          ].map(([title, description]) => (
            <article
              className="rounded-lg border border-zinc-200 bg-white p-5"
              key={title}
            >
              <h2 className="text-base font-semibold">{title}</h2>
              <p className="mt-2 text-sm leading-6 text-zinc-600">
                {description}
              </p>
            </article>
          ))}
        </section>
      </main>
    </div>
  );
}
