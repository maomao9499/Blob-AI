/// <reference types="vite/client" />

interface Window {
  readonly blobDesktop?: import('./types/desktop.types').BlobDesktopApi;
}
