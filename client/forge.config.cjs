const { join } = require('node:path');

module.exports = {
  packagerConfig: {
    name: 'Blob',
    executableName: 'Blob',
    appBundleId: 'dev.blob.desktop',
    appCategoryType: 'public.app-category.productivity',
    asar: true,
    prune: true,
    extraResource: ['web', 'server', 'runtime', 'manifest.json'].map(name => join(__dirname, 'resources', name)),
    ignore: path => path !== '' && !/^\/(?:dist-electron(?:\/|$)|package\.json$)/.test(path),
  },
  makers: [
    { name: '@electron-forge/maker-dmg', config: { name: 'Blob', format: 'UDZO', overwrite: true }, platforms: ['darwin'] },
    { name: '@electron-forge/maker-zip', platforms: ['darwin'] },
  ],
};
