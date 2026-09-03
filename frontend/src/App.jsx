import { useEffect, useState } from 'react';
import { api } from './api';
import AuthorPicker from './components/AuthorPicker';
import ConceptFootprint from './components/ConceptFootprint';
import SimilarAuthors from './components/SimilarAuthors';
import InfluencePanel from './components/InfluencePanel';
import CitationPathExplorer from './components/CitationPathExplorer';

export default function App() {
  const [authors, setAuthors] = useState([]);
  const [papers, setPapers] = useState([]);
  const [initLoading, setInitLoading] = useState(true);
  const [initError, setInitError] = useState(null);
  const [selectedAuthorId, setSelectedAuthorId] = useState(null);

  const [footprint, setFootprint] = useState([]);
  const [footprintLoading, setFootprintLoading] = useState(false);
  const [footprintError, setFootprintError] = useState(null);

  const [similar, setSimilar] = useState([]);
  const [similarLoading, setSimilarLoading] = useState(false);
  const [similarError, setSimilarError] = useState(null);

  const [influence, setInfluence] = useState([]);
  const [influenceLoading, setInfluenceLoading] = useState(false);
  const [influenceError, setInfluenceError] = useState(null);

  // Initial load: authors + papers. If this fails, CognoDB is almost
  // certainly unreachable.
  useEffect(() => {
    Promise.all([api.listAuthors(), api.listPapers()])
      .then(([authorsRes, papersRes]) => {
        setAuthors(authorsRes);
        setPapers(papersRes);
        if (authorsRes.length) setSelectedAuthorId(authorsRes[0].id);
      })
      .catch((e) => setInitError(e.message))
      .finally(() => setInitLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedAuthorId) return;

    setFootprintLoading(true);
    setFootprintError(null);
    api.conceptFootprint(selectedAuthorId)
      .then(setFootprint)
      .catch((e) => setFootprintError(e.message))
      .finally(() => setFootprintLoading(false));

    setSimilarLoading(true);
    setSimilarError(null);
    api.similarAuthors(selectedAuthorId)
      .then(setSimilar)
      .catch((e) => setSimilarError(e.message))
      .finally(() => setSimilarLoading(false));

    setInfluenceLoading(true);
    setInfluenceError(null);
    api.influenceNetwork(selectedAuthorId)
      .then(setInfluence)
      .catch((e) => setInfluenceError(e.message))
      .finally(() => setInfluenceLoading(false));
  }, [selectedAuthorId]);

  // Whole-app error state: database unreachable at startup.
  if (initError && !initLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4">
        <div className="max-w-md text-center">
          <div className="text-4xl mb-3">⚠️</div>
          <h1 className="text-lg font-semibold text-slate-800 mb-1">Can't reach the graph database</h1>
          <p className="text-sm text-slate-500">{initError}</p>
          <p className="text-xs text-slate-400 mt-3">
            Check that the backend is running and CognoDB connection env vars (COGNODB_URI, COGNODB_USERNAME, COGNODB_PASSWORD) are set correctly.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="max-w-5xl mx-auto px-6 py-5 flex items-center justify-between flex-wrap gap-3">
          <div>
            <h1 className="text-xl font-bold text-slate-900">Research Network Explorer</h1>
            <p className="text-sm text-slate-500">Backed by CognoDB — a graph of authors, papers, technologies, concepts and institutions.</p>
          </div>
          <AuthorPicker
            authors={authors}
            selectedId={selectedAuthorId}
            onSelect={setSelectedAuthorId}
            loading={initLoading}
            error={initError}
          />
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-8 space-y-10">
        <section>
          <h2 className="text-base font-semibold text-slate-800 mb-1">Concept footprint</h2>
          <p className="text-sm text-slate-500 mb-4">
            A 3-hop traversal — Author → Paper → Technology → Concept — showing which research fields
            this author's work touches, via the technologies their papers actually use.
          </p>
          <ConceptFootprint concepts={footprint} loading={footprintLoading} error={footprintError} />
        </section>

        <section>
          <h2 className="text-base font-semibold text-slate-800 mb-1">Similar authors</h2>
          <p className="text-sm text-slate-500 mb-4">Authors whose papers use the most of the same technologies.</p>
          <SimilarAuthors authors={similar} loading={similarLoading} error={similarError} />
        </section>

        <section>
          <h2 className="text-base font-semibold text-slate-800 mb-1">Influence network</h2>
          <p className="text-sm text-slate-500 mb-4">Authors whose papers cite this author's work, up to 3 citation hops out.</p>
          <InfluencePanel influencers={influence} loading={influenceLoading} error={influenceError} />
        </section>

        <section>
          <h2 className="text-base font-semibold text-slate-800 mb-1">Citation path explorer</h2>
          {papers.length > 0 ? (
            <CitationPathExplorer papers={papers} />
          ) : (
            <p className="text-sm text-slate-500">No papers loaded yet.</p>
          )}
        </section>
      </main>
    </div>
  );
}
