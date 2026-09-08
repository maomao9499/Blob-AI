import { mkdir, stat, rename, appendFile } from 'node:fs/promises';
import { dirname } from 'node:path';

export class RotatingLog {
  private pending = Promise.resolve();
  constructor(private readonly file: string, private readonly secrets: string[], private readonly maxBytes = 2 * 1024 * 1024) {}

  write(text: string): Promise<void> {
    this.pending = this.pending.then(async () => {
      let sanitized = text;
      for (const secret of this.secrets.filter(Boolean).sort((a, b) => b.length - a.length)) sanitized = sanitized.split(secret).join('[REDACTED]');
      sanitized = sanitized.replace(/((?:password|passwd|token|authorization)\s*[=:]\s*)[^\s,;]+/gi, '$1[REDACTED]');
      const bytes = Buffer.from(sanitized).subarray(0, this.maxBytes);
      await mkdir(dirname(this.file), { recursive: true, mode: 0o700 });
      const size = await stat(this.file).then(value => value.size).catch(() => 0);
      if (size + bytes.length > this.maxBytes) await rename(this.file, `${this.file}.1`).catch((error: NodeJS.ErrnoException) => { if (error.code !== 'ENOENT') throw error; });
      await appendFile(this.file, bytes, { mode: 0o600 });
    }).catch(() => { /* Logging failure must not crash the app or expose raw output. */ });
    return this.pending;
  }
}
