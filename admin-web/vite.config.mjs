import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import runtimeConfig from './runtime-config.js';

const configDir = path.dirname(fileURLToPath(import.meta.url));
const externalConfigPath = path.resolve(configDir, runtimeConfig.DEFAULT_EXTERNAL_CONFIG_PATH);
const externalConfig = fs.existsSync(externalConfigPath)
  ? runtimeConfig.readExternalConfigFile(externalConfigPath)
  : {};
const proxyTarget = runtimeConfig.resolveAdminProxyTarget(process.env.VITE_PROXY_TARGET, {
  externalConfig
});

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5174,
    open: true,
    proxy: proxyTarget ? {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    } : {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
});
