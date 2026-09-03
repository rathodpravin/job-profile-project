export default function SimilarAuthors({ authors, loading, error }) {
  if (loading) {
    return (
      <div className="space-y-2">
        {[...Array(2)].map((_, i) => (
          <div key={i} className="animate-pulse h-12 bg-slate-200 rounded-lg" />
        ))}
      </div>
    );
  }
  if (error) return <p className="text-sm text-red-600">Couldn't load similar authors: {error}</p>;
  if (!authors.length) return <p className="text-sm text-slate-500">No authors share technologies with this one yet.</p>;

  return (
    <ul className="space-y-2">
      {authors.map((a) => (
        <li key={a.authorId} className="border border-slate-200 rounded-lg px-3 py-2 bg-white shadow-sm">
          <div className="flex items-center justify-between">
            <p className="text-sm font-medium text-slate-800">{a.name}</p>
            <span className="text-xs font-medium bg-indigo-50 text-indigo-700 px-2 py-1 rounded-full">
              {a.sharedCount} shared
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1">{a.sharedTechnologies.join(', ')}</p>
        </li>
      ))}
    </ul>
  );
}
