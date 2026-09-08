import type { DesktopSettingsInput } from './desktop-api.cjs';

export function validateSettings(value: unknown): DesktopSettingsInput {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('配置格式不正确');
  const input = value as Record<string, unknown>;
  const fields = ['dbUrl', 'dbUsername', 'dbPassword', 'redisHost', 'redisPort', 'redisPassword'];
  if (Object.keys(input).some(key => !fields.includes(key))) throw new Error('配置包含不支持的字段');
  const stringField = (key: string, max: number): string => {
    const text = input[key];
    if (typeof text !== 'string' || !text.trim() || text.length > max || /[\r\n\0]/.test(text)) throw new Error(`${key} 格式不正确`);
    return text.trim();
  };
  const dbUrl = stringField('dbUrl', 2048);
  let decodedDbUrl: string;
  try { decodedDbUrl = decodeURIComponent(dbUrl); } catch { throw new Error('dbUrl 格式不正确'); }
  if (!/^jdbc:mysql:\/\/[^/?#]+\/[^?#]+(?:\?[^#]*)?$/.test(dbUrl) || /(?:password|passwd|user|username)\s*=|@/i.test(decodedDbUrl)) throw new Error('请使用不含用户名和密码的 MySQL JDBC 地址');
  const redisHost = stringField('redisHost', 253);
  if (!/^[a-zA-Z0-9.:_-]+$/.test(redisHost)) throw new Error('Redis 主机格式不正确');
  if (typeof input.redisPort !== 'number' || !Number.isInteger(input.redisPort) || input.redisPort < 1 || input.redisPort > 65535) throw new Error('Redis 端口应为 1 到 65535');
  for (const key of ['dbPassword', 'redisPassword']) {
    if (input[key] !== undefined && (typeof input[key] !== 'string' || (input[key] as string).length > 4096 || (input[key] as string).includes('\0'))) throw new Error('密码格式不正确');
  }
  return { dbUrl, dbUsername: stringField('dbUsername', 128), redisHost, redisPort: input.redisPort, dbPassword: input.dbPassword as string | undefined, redisPassword: input.redisPassword as string | undefined };
}

export function isTrustedSender(value: string, development: boolean): boolean {
  try {
    const url = new URL(value);
    if (url.username || url.password) return false;
    return (url.protocol === 'blob-app:' && url.host === 'app') || (development && url.origin === 'http://127.0.0.1:5173');
  } catch { return false; }
}

const PUBLIC_ERRORS = new Set([
  '配置格式不正确', '配置包含不支持的字段', 'dbUrl 格式不正确', 'dbUsername 格式不正确', 'redisHost 格式不正确',
  '请使用不含用户名和密码的 MySQL JDBC 地址', 'Redis 主机格式不正确', 'Redis 端口应为 1 到 65535', '密码格式不正确',
  '配置文件无法读取，请重新填写并保存设置', '无法解密配置，请重新输入密码并保存设置',
  '系统安全存储暂不可用，请解锁登录钥匙串后重试', '请先保存 MySQL 和 Redis 设置',
  '无法分配后端端口', '后端启动已取消', '后端启动失败，请检查 MySQL 配置和本地日志', '后端启动超时，请检查 MySQL 配置后重试',
]);

export function publicErrorMessage(error: unknown): string {
  return error instanceof Error && PUBLIC_ERRORS.has(error.message) ? error.message : '操作失败，请检查配置、系统钥匙串和本地日志后重试';
}
