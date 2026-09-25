import { createApp, watchEffect } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import { loadExternalConfig } from './config/external-config';
import { branding, getApiBaseUrl, loadPublicBranding } from './stores/branding';
import { applyBrandDocument } from './utils/branding';
import './styles.css';
import './refinement.css';
import './story-console.css';

async function bootstrap() {
  await loadExternalConfig();

  const app = createApp(App);
  const pinia = createPinia();

  app.use(pinia);
  app.use(router);
  app.mount('#app');
  watchEffect(() => applyBrandDocument(document, branding, router.currentRoute.value.meta.title, getApiBaseUrl()));
  void loadPublicBranding();
}

bootstrap();
