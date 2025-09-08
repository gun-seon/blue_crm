import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
    const loading = ref(false)

    function setLoading(value) {
        loading.value = value
    }

    return { loading, setLoading }
})