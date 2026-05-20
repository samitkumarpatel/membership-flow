async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body !== undefined) opts.body = JSON.stringify(body);
  const res = await fetch(path, opts);
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: res.statusText }));
    throw new Error(err.detail || err.title || res.statusText);
  }
  if (res.status === 204) return null;
  return res.json();
}

const get   = (path)       => api('GET',   path);
const post  = (path, body) => api('POST',  path, body);
const patch = (path, body) => api('PATCH', path, body);
const del   = (path)       => api('DELETE', path);
