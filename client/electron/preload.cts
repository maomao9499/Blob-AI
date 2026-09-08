import { contextBridge, ipcRenderer } from 'electron';
import type { BlobDesktopApi } from './desktop-api.cjs';

const api: BlobDesktopApi = Object.freeze({
  getBootstrap: () => ipcRenderer.invoke('blob:get-bootstrap'),
  saveSettings: (input: Parameters<BlobDesktopApi['saveSettings']>[0]) => ipcRenderer.invoke('blob:save-settings', input),
  startBackend: () => ipcRenderer.invoke('blob:start-backend'),
  getDependencyStatus: () => ipcRenderer.invoke('blob:get-dependency-status'),
  onBackendExit: (listener: (message: string) => void) => {
    if (typeof listener !== 'function') throw new Error('回调格式不正确');
    const handler = (_event: Electron.IpcRendererEvent, message: unknown): void => { if (typeof message === 'string') listener(message); };
    ipcRenderer.on('blob:backend-exit', handler);
    return () => { ipcRenderer.removeListener('blob:backend-exit', handler); };
  },
});

contextBridge.exposeInMainWorld('blobDesktop', api);
