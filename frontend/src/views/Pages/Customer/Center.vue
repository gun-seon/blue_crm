<template>
  <div>
    <button @click="downloadExcel"
            class="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600">
      📥 엑셀 다운로드
    </button>
  </div>
</template>

<script setup lang="ts">
import api from "@/plugins/axios"; // axios 인스턴스

const downloadExcel = async () => {
  const res = await api.get("/api/excel", {
    responseType: "blob"
  });

  const blob = new Blob([res.data], { type: res.headers["content-type"] });
  const link = document.createElement("a");
  link.href = window.URL.createObjectURL(blob);
  link.download = "data.xlsx";
  link.click();
};
</script>