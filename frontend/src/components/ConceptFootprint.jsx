export default function ConceptFootprint({ concepts, loading, error }) {
  if (loading) {
    return (
      <div className="space-y-2">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="animate-pulse h-14 bg-slate-200 rounded-lg" />
        ))}
      </div>
    );
  }
  if (error) return <p className="text-sm text-red-600">Couldn't load concept footprint: {error}</p>;
  if (!concepts.length) {
    return <p className="text-sm text-slate-500">No research found — this author's papers don't reference any catalogued technologies yet.</p>;
  }

  const maxCount = Math.max(...concepts.map((c) => c.paperCount));

  return (
    <div className="space-y-3">
      {concepts.map((c) => (
        <div key={c.conceptId} className="border border-slate-200 rounded-xl p-4 bg-white shadow-sm">
          <div className="flex items-center justify-between">
            <p className="font-semibold text-slate-800">{c.conceptName}</p>
            <span className="text-xs font-medium bg-indigo-50 text-indigo-700 px-2 py-1 rounded-full">
              {c.paperCount} paper{c.paperCount === 1 ? '' : 's'}
            </span>
          </div>
          <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden mt-2 mb-2">
            <div
              className="h-full bg-indigo-500 rounded-full"
              style={{ width: `${(c.paperCount / maxCount) * 100}%` }}
            />
          </div>
          <p className="text-xs text-slate-400">via {c.viaTechnologies.join(', ')}</p>
        </div>
      ))}
    </div>
  );
}
