import { useState } from 'react';
import { api } from '../api';

export default function CitationPathExplorer({ papers }) {
  const [from, setFrom] = useState(papers[0]?.id || '');
  const [to, setTo] = useState(papers[1]?.id || '');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searched, setSearched] = useState(false);

  const handleSearch = async () => {
    if (!from || !to) return;
    setLoading(true);
    setError(null);
    setSearched(true);
    try {
      const res = await api.citationPath(from, to);
      setResult(res);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
      <p className="text-sm text-slate-600 mb-3">
        Find the shortest citation chain connecting two papers — a variable-length graph traversal
        that a relational database would need a recursive query (or app-side BFS) to express.
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <select value={from} onChange={(e) => setFrom(e.target.value)} className="border border-slate-300 rounded-lg px-2 py-1.5 text-sm max-w-[220px]">
          {papers.map((p) => <option key={p.id} value={p.id}>{p.title}</option>)}
        </select>
        <span className="text-slate-400 text-sm">→</span>
        <select value={to} onChange={(e) => setTo(e.target.value)} className="border border-slate-300 rounded-lg px-2 py-1.5 text-sm max-w-[220px]">
          {papers.map((p) => <option key={p.id} value={p.id}>{p.title}</option>)}
        </select>
        <button
          onClick={handleSearch}
          disabled={loading}
          className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-1.5 rounded-lg"
        >
          {loading ? 'Searching…' : 'Find path'}
        </button>
      </div>

      {searched && (
        <div className="mt-4">
          {loading && <p className="text-sm text-slate-400">Traversing citation links…</p>}
          {error && <p className="text-sm text-red-600">Error: {error}</p>}
          {!loading && !error && result && (
            result.hops === -1 ? (
              <p className="text-sm text-slate-500">No citation path found within 6 hops.</p>
            ) : (
              <div className="flex flex-col gap-2">
                {result.chain.map((title, i) => (
                  <div key={i} className="flex items-start gap-2">
                    <span className="text-sm bg-indigo-50 text-indigo-800 px-2.5 py-1 rounded-full font-medium">
                      {title}
                    </span>
                    {i < result.chain.length - 1 && <span className="text-slate-300 pl-1">↓ cites</span>}
                  </div>
                ))}
                <span className="text-xs text-slate-400 mt-1">({result.hops} hop{result.hops === 1 ? '' : 's'})</span>
              </div>
            )
          )}
        </div>
      )}
    </div>
  );
}
