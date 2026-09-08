import { mkdir, readFile, writeFile, rename, chmod } from 'node:fs/promises';
import { dirname } from 'node:path';
import { randomUUID } from 'node:crypto';
import type { DesktopSettingsInput, PublicDesktopSettings } from './desktop-api.cjs';
import { validateSettings } from './security.cjs';

export interface SecureValueCodec {
  encrypt(value: string): Promise<Buffer>;
  decrypt(value: Buffer): Promise<string>;
}

interface StoredSettings extends Omit<DesktopSettingsInput, 'dbPassword' | 'redisPassword'> {
  version: 1;
  encryptedDbPassword: string;
  encryptedRedisPassword: string;
}

export class SettingsStore {
  constructor(private readonly file: string, private readonly codec: SecureValueCodec) {}

  private async read(): Promise<StoredSettings | null> {
    try {
      const parsed: unknown = JSON.parse(await readFile(this.file, 'utf8'));
      if (!parsed || typeof parsed !== 'object') throw new Error('invalid');
      const data = parsed as StoredSettings;
      validateSettings({ dbUrl: data.dbUrl, dbUsername: data.dbUsername, redisHost: data.redisHost, redisPort: data.redisPort });
      if (data.version !== 1 || ![data.encryptedDbPassword, data.encryptedRedisPassword].every(value => typeof value === 'string' && /^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(value))) throw new Error('invalid');
      return data;
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null;
      throw new Error('配置文件无法读取，请重新填写并保存设置');
    }
  }

  private publicSettings(data: StoredSettings): PublicDesktopSettings {
    return { dbUrl: data.dbUrl, dbUsername: data.dbUsername, redisHost: data.redisHost, redisPort: data.redisPort, dbPasswordConfigured: !!data.encryptedDbPassword, redisPasswordConfigured: !!data.encryptedRedisPassword };
  }

  async readPublic(): Promise<PublicDesktopSettings | null> {
    const data = await this.read();
    return data ? this.publicSettings(data) : null;
  }

  async save(value: unknown): Promise<PublicDesktopSettings> {
    const input = validateSettings(value);
    // An unreadable file can be replaced through the first-run recovery form.
    const previous = await this.read().catch(() => null);
    const encrypt = async (text: string | undefined, existing: string | undefined): Promise<string> => text ? (await this.codec.encrypt(text)).toString('base64') : existing ?? '';
    const data: StoredSettings = {
      version: 1, dbUrl: input.dbUrl, dbUsername: input.dbUsername, redisHost: input.redisHost, redisPort: input.redisPort,
      encryptedDbPassword: await encrypt(input.dbPassword, previous?.encryptedDbPassword),
      encryptedRedisPassword: await encrypt(input.redisPassword, previous?.encryptedRedisPassword),
    };
    await mkdir(dirname(this.file), { recursive: true, mode: 0o700 });
    const temporary = `${this.file}.${randomUUID()}.tmp`;
    await writeFile(temporary, JSON.stringify(data, null, 2), { mode: 0o600, flag: 'wx' });
    await rename(temporary, this.file);
    await chmod(this.file, 0o600);
    return this.publicSettings(data);
  }

  // Main-process-only method. It is never exposed through preload or IPC.
  async readForBackend(): Promise<DesktopSettingsInput | null> {
    const data = await this.read();
    if (!data) return null;
    try {
      return { dbUrl: data.dbUrl, dbUsername: data.dbUsername, redisHost: data.redisHost, redisPort: data.redisPort,
        dbPassword: data.encryptedDbPassword ? await this.codec.decrypt(Buffer.from(data.encryptedDbPassword, 'base64')) : '',
        redisPassword: data.encryptedRedisPassword ? await this.codec.decrypt(Buffer.from(data.encryptedRedisPassword, 'base64')) : '' };
    } catch { throw new Error('无法解密配置，请重新输入密码并保存设置'); }
  }
}
