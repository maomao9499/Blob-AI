import { app, BrowserWindow } from 'electron';
import { join } from 'node:path';

const createWindow = async (): Promise<void> => {
  const window = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 760,
    minHeight: 540,
    title: 'Blob',
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });

  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', (event) => event.preventDefault());
  if (!app.isPackaged && process.argv.includes('--blob-dev')) {
    await window.loadURL('http://127.0.0.1:5173');
  } else {
    await window.loadFile(join(__dirname, '../dist/index.html'));
  }
};

app.whenReady().then(async () => {
  await createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      void createWindow().catch((error: unknown) => console.error('无法打开 Blob 窗口', error));
    }
  });
}).catch((error: unknown) => {
  console.error('Blob 启动失败', error);
  app.quit();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
