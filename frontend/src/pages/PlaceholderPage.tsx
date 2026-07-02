export function PlaceholderPage({ title, description }: { title: string; description: string }) {
  return (
    <div className="mx-auto flex max-w-xl flex-col items-center px-6 py-24 text-center">
      <h1 className="text-xl font-semibold text-gray-100">{title}</h1>
      <p className="mt-2 text-sm text-gray-400">{description}</p>
    </div>
  )
}
