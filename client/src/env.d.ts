/// <reference types="vite/client" />

interface BlobDesktopBootstrap {
  readonly apiBaseUrl?: string;
  readonly accessToken?: string;
}

interface Window {
  readonly blobDesktop?: BlobDesktopBootstrap;
}
