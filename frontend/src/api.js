const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function request(path) {
  let res;
  try {
    res = await fetch(`${BASE_URL}${path}`);
  } catch (networkErr) {
    const err = new Error('Could not reach the API server. Is the backend running?');
    err.cause = networkErr;
    throw err;
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      message = body.message || message;
    } catch {
      // ignore parse errors, use default message
    }
    throw new Error(message);
  }
  return res.json();
}

export const api = {
  listAuthors: () => request('/api/authors'),
  listPapers: () => request('/api/papers'),
  listConcepts: () => request('/api/concepts'),
  conceptFootprint: (authorId) => request(`/api/authors/${authorId}/concept-footprint`),
  similarAuthors: (authorId) => request(`/api/authors/${authorId}/similar`),
  influenceNetwork: (authorId) => request(`/api/authors/${authorId}/influence`),
  citationPath: (from, to) => request(`/api/papers/citation-path?from=${from}&to=${to}`),
};
