import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import { loadExternalConfig } from './config/external-config';
import './styles.css';
import './refinement.css';

async function bootstrap() {
  await loadExternalConfig();

  const app = createApp(App);
  const pinia = createPinia();

  app.use(pinia);
  app.use(router);
  app.mount('#app');
}

bootstrap();
