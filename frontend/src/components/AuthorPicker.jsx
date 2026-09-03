export default function AuthorPicker({ authors, selectedId, onSelect, loading, error }) {
  if (loading) {
    return <div className="animate-pulse h-11 bg-slate-200 rounded-lg w-72" />;
  }
  if (error) {
    return <p className="text-sm text-red-600">Couldn't load authors: {error}</p>;
  }
  if (!authors.length) {
    return <p className="text-sm text-slate-500">No authors found. Have you run the seed script?</p>;
  }
  return (
    <div className="flex items-center gap-3">
      <label htmlFor="author-select" className="text-sm font-medium text-slate-600">
        Explore author
      </label>
      <select
        id="author-select"
        value={selectedId || ''}
        onChange={(e) => onSelect(e.target.value)}
        className="border border-slate-300 rounded-lg px-3 py-2 text-sm bg-white shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 min-w-[240px]"
      >
        {authors.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name} — {a.institution} ({a.paperCount} papers)
          </option>
        ))}
      </select>
    </div>
  );
}
