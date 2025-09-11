import './assets/main.css'
import 'flatpickr/dist/flatpickr.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import flatpickr from "flatpickr"
import Korean from "flatpickr/dist/l10n/ko.js"

flatpickr.localize(Korean)

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
