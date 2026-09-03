export default function InfluencePanel({ influencers, loading, error }) {
  if (loading) {
    return (
      <div className="space-y-2">
        {[...Array(2)].map((_, i) => (
          <div key={i} className="animate-pulse h-12 bg-slate-200 rounded-lg" />
        ))}
      </div>
    );
  }
  if (error) return <p className="text-sm text-red-600">Couldn't load influence network: {error}</p>;
  if (!influencers.length) {
    return <p className="text-sm text-slate-500">No one has cited this author's work yet (within 3 citation hops).</p>;
  }

  return (
    <ul className="space-y-2">
      {influencers.map((inf, i) => (
        <li key={`${inf.authorId}-${i}`} className="flex items-center justify-between border border-slate-200 rounded-lg px-3 py-2 bg-white shadow-sm">
          <div>
            <p className="text-sm font-medium text-slate-800">{inf.name}</p>
            <p className="text-xs text-slate-400">via "{inf.viaPaper}"</p>
          </div>
          <span className="text-xs font-medium bg-amber-50 text-amber-700 px-2 py-1 rounded-full whitespace-nowrap">
            {inf.hops} hop{inf.hops === 1 ? '' : 's'} away
          </span>
        </li>
      ))}
    </ul>
  );
}
