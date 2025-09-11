<template>
  <admin-layout>
    <div class="grid grid-cols-12 gap-4 md:gap-6">
      <div class="col-span-12 space-y-6 xl:col-span-7">
        <!-- 총 회원수 등 간략화된 지표 -->
        <ecommerce-metrics />
      </div>

<!--      <div class="col-span-12 xl:col-span-5">-->
<!--        <customer-demographic />-->
<!--      </div>-->

<!--      <div class="col-span-12 xl:col-span-7">-->
<!--        <recent-orders />-->
<!--      </div>-->
    </div>
  </admin-layout>
</template>

<script>
import AdminLayout from '../components/layout/AdminLayout.vue'
import EcommerceMetrics from '../components/ecommerce/EcommerceMetrics.vue'
import CustomerDemographic from '../components/ecommerce/CustomerDemographic.vue'
import RecentOrders from '../components/ecommerce/RecentOrders.vue'
export default {
  components: {
    AdminLayout,
    EcommerceMetrics,
    CustomerDemographic,
    RecentOrders,
  },
  name: 'Ecommerce',
}
</script>
