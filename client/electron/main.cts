import { app, BrowserWindow, ipcMain, protocol, safeStorage } from 'electron';
import { isAbsolute, join } from 'node:path';
import { AppProtocol } from './AppProtocol.cjs';
import { BackendProcessManager } from './BackendProcessManager.cjs';
import { SettingsStore } from './SettingsStore.cjs';
import { isTrustedSender, publicErrorMessage, validateSettings } from './security.cjs';
import type { DesktopBootstrap, DependencyStatus } from './desktop-api.cjs';

app.setName('Blob');
// Used by local smoke runs to isolate first-run data; never exposed to renderer.
if (process.env.BLOB_USER_DATA_DIR && isAbsolute(process.env.BLOB_USER_DATA_DIR)) app.setPath('userData', process.env.BLOB_USER_DATA_DIR);
protocol.registerSchemesAsPrivileged([{ scheme: 'blob-app', privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true, stream: true } }]);

const development = !app.isPackaged && process.argv.includes('--blob-dev');
let mainWindow: BrowserWindow | null = null;
let manager: BackendProcessManager | null = null;
let quitting = false;

async function initialize(): Promise<void> {
  const dataRoot = join(app.getPath('userData'), 'blob-data');
  const resources = app.isPackaged ? process.resourcesPath : join(__dirname, '../resources');
  const appProtocol = new AppProtocol(app.isPackaged ? join(resources, 'web') : join(__dirname, '../dist'));
  protocol.handle('blob-app', request => appProtocol.handle(request));
  const store = new SettingsStore(join(dataRoot, 'config/settings.json'), {
    encrypt: async value => {
      if (!safeStorage.isEncryptionAvailable()) throw new Error('系统安全存储暂不可用，请解锁登录钥匙串后重试');
      return safeStorage.encryptStringAsync(value);
    },
    decrypt: async value => (await safeStorage.decryptStringAsync(value)).result,
  });
  manager = new BackendProcessManager({
    javaPath: app.isPackaged ? join(resources, 'runtime/Contents/Home/bin/java') : process.env.BLOB_JAVA_PATH ?? join(resources, 'runtime/Contents/Home/bin/java'),
    jarPath: app.isPackaged ? join(resources, 'server/blob-server.jar') : process.env.BLOB_SERVER_JAR ?? join(__dirname, '../../server/target/blob-server.jar'),
    uploadDir: join(dataRoot, 'images'), logDir: join(dataRoot, 'logs'),
    allowDevOrigin: development,
    onUnexpectedExit: message => {
      appProtocol.setBackendConnection(null);
      if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send('blob:backend-exit', message);
    },
  });
  let lastError: string | undefined;
  const bootstrap = async (): Promise<DesktopBootstrap> => {
    const settings = await store.readPublic().catch(() => { lastError = '配置文件无法读取，请重新填写并保存设置'; return null; });
    const connection = manager?.getConnection();
    return { setupRequired: !settings || !connection, apiBaseUrl: connection ? `${connection.apiBaseUrl}/api/v1` : null, sessionToken: connection?.sessionToken ?? null, settings, ...(lastError ? { error: lastError } : {}) };
  };
  const start = async (): Promise<DesktopBootstrap> => {
    try {
      const settings = await store.readForBackend();
      if (!settings) throw new Error('请先保存 MySQL 和 Redis 设置');
      const connection = await manager!.start(settings);
      appProtocol.setBackendConnection(connection);
      lastError = undefined;
    } catch (error) { lastError = publicErrorMessage(error); }
    return bootstrap();
  };
  // Mutating operations run serially, including first-run automatic startup.
  let operation: Promise<unknown> = store.readPublic().then(settings => settings ? start() : undefined).catch(() => { lastError = '配置文件无法读取，请重新填写并保存设置'; });
  const serial = <T,>(action: () => Promise<T>): Promise<T> => {
    const result = operation.then(action, action);
    operation = result.catch(() => undefined);
    return result;
  };
  const register = (channel: string, acceptsPayload: boolean, handler: (payload: unknown) => Promise<unknown>): void => {
    ipcMain.handle(channel, async (event, ...args: unknown[]) => {
      if (!mainWindow || event.sender !== mainWindow.webContents || event.senderFrame !== mainWindow.webContents.mainFrame || !isTrustedSender(event.senderFrame?.url ?? '', development)) throw new Error('不允许的调用来源');
      if (acceptsPayload ? args.length !== 1 : args.length !== 0) throw new Error('调用参数格式不正确');
      try { return await handler(args[0]); } catch (error) { throw new Error(publicErrorMessage(error)); }
    });
  };
  register('blob:get-bootstrap', false, async () => { await operation; return bootstrap(); });
  register('blob:start-backend', false, () => serial(start));
  register('blob:save-settings', true, input => {
    const validated = validateSettings(input);
    return serial(async () => {
      const saved = await store.save(validated);
      await manager!.stop();
      appProtocol.setBackendConnection(null);
      lastError = undefined;
      return saved;
    });
  });
  register('blob:get-dependency-status', false, async () => {
    const connection = manager?.getConnection();
    const unavailable: DependencyStatus = { status: 'UNKNOWN', message: '请先启动本地后端' };
    if (!connection) return { mysql: unavailable, redis: unavailable };
    try {
      const response = await fetch(`${connection.apiBaseUrl}/api/v1/system/dependencies`, { headers: { 'X-Blob-Desktop-Token': connection.sessionToken }, redirect: 'error', signal: AbortSignal.timeout(8000) });
      if (!response.ok) throw new Error('unavailable');
      const body = await response.json() as { data: { mysql: DependencyStatus; redis: DependencyStatus } };
      return body.data;
    } catch { return { mysql: unavailable, redis: unavailable }; }
  });
  await createWindow();
}

async function createWindow(): Promise<void> {
  mainWindow = new BrowserWindow({ width: 1200, height: 800, minWidth: 760, minHeight: 540, title: 'Blob',
    webPreferences: { preload: join(__dirname, 'preload.cjs'), contextIsolation: true, nodeIntegration: false, sandbox: true } });
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  mainWindow.webContents.on('will-navigate', event => event.preventDefault());
  mainWindow.webContents.on('did-fail-load', (_event, code, description) => console.error('Blob 页面加载失败', code, description));
  mainWindow.webContents.session.setPermissionRequestHandler((_contents, _permission, callback) => callback(false));
  mainWindow.on('closed', () => { mainWindow = null; });
  await mainWindow.loadURL(development ? 'http://127.0.0.1:5173' : 'blob-app://app/');
}

if (!app.requestSingleInstanceLock()) app.quit();
else {
  app.on('second-instance', () => { if (mainWindow) { if (mainWindow.isMinimized()) mainWindow.restore(); mainWindow.show(); mainWindow.focus(); } });
  app.whenReady().then(initialize).catch(() => { console.error('Blob 桌面初始化失败，请检查本地运行资源'); app.quit(); });
  app.on('activate', () => { if (!mainWindow && !quitting && manager) void createWindow(); });
  app.on('window-all-closed', () => app.quit());
  app.on('before-quit', event => {
    if (quitting) return;
    event.preventDefault();
    quitting = true;
    void (manager?.stop() ?? Promise.resolve()).finally(() => app.quit());
  });
}
