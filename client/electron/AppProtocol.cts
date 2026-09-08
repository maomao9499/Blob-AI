import { readFile, realpath } from 'node:fs/promises';
import { extname, isAbsolute, relative, resolve } from 'node:path';
import type { BackendConnection } from './desktop-api.cjs';

const MIME: Record<string, string> = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.gif': 'image/gif', '.webp': 'image/webp', '.ico': 'image/x-icon', '.woff2': 'font/woff2', '.woff': 'font/woff', '.ttf': 'font/ttf' };
const CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https: http:; font-src 'self' data:; connect-src 'self' http://127.0.0.1:*; object-src 'none'; frame-src 'none'; base-uri 'none'";

export class AppProtocol {
  private connection: BackendConnection | null = null;
  constructor(private readonly root: string, private readonly request: typeof fetch = fetch) {}
  setBackendConnection(connection: BackendConnection | null): void { this.connection = connection; }

  async handle(request: Request): Promise<Response> {
    const url = new URL(request.url);
    if (url.protocol !== 'blob-app:' || url.host !== 'app' || url.username || url.password) return new Response('Forbidden', { status: 403 });
    if (request.method !== 'GET' && request.method !== 'HEAD') return new Response('Method not allowed', { status: 405 });
    let pathname: string;
    try { pathname = decodeURIComponent(url.pathname); } catch { return new Response('Bad path', { status: 400 }); }
    if (pathname.includes('\\') || pathname.includes('\0') || pathname.split('/').includes('..')) return new Response('Forbidden', { status: 403 });
    if (pathname === '/api' || pathname.startsWith('/api/')) {
      if (!/^\/api\/v1\/media\/images\/[a-zA-Z0-9_-]+\.(png|jpe?g|gif|webp)$/.test(pathname) || url.search) return new Response('Forbidden', { status: 403 });
      if (!this.connection) return new Response('Backend unavailable', { status: 503 });
      try {
        const upstream = await this.request(`${this.connection.apiBaseUrl}${pathname}`, { method: request.method, headers: { 'X-Blob-Desktop-Token': this.connection.sessionToken }, redirect: 'error', signal: AbortSignal.timeout(10000) });
        const headers = new Headers({ 'X-Content-Type-Options': 'nosniff', 'Cache-Control': 'private, max-age=3600' });
        for (const key of ['content-type', 'content-length']) { const value = upstream.headers.get(key); if (value) headers.set(key, value); }
        return new Response(upstream.body, { status: upstream.status, headers });
      } catch { return new Response('Image unavailable', { status: 502 }); }
    }
    try {
      const root = await realpath(this.root);
      const target = await realpath(resolve(root, `.${pathname === '/' ? '/index.html' : pathname}`));
      const rel = relative(root, target);
      if (!rel || rel.startsWith('..') || isAbsolute(rel)) return new Response('Forbidden', { status: 403 });
      const bytes = request.method === 'HEAD' ? null : new Uint8Array(await readFile(target));
      return new Response(bytes, { headers: { 'Content-Type': MIME[extname(target)] ?? 'application/octet-stream', 'Content-Security-Policy': CSP, 'X-Content-Type-Options': 'nosniff' } });
    } catch { return new Response('Not found', { status: 404 }); }
  }
}
