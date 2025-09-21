<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="내 정보 수정" />

    <div
        class="relative overflow-hidden rounded-2xl border border-gray-200 bg-white p-0
             dark:border-gray-800 dark:bg-white/[0.03] "
    >
      <!-- 가운데 정렬 컨테이너 -->
      <div class="relative z-10 mx-auto w-full max-w-2xl">
        <!-- 장식 그리드 -->
        <div
            class="pointer-events-none absolute inset-y-0 left-1/2 -translate-x-1/2 w-[250%] max-w-none select-none"
            aria-hidden="true"
        >
          <CommonGridShape
              src="/images/shape/grid-01.svg"
              mode="mask"
              size="w-56 h-56"
              position="-top-1 -right-4"
              opacity="opacity-[0.06] dark:opacity-[0.10]"
              lightColor="bg-gray-900"
              darkColor="dark:bg-gray-900"
          />
        </div>

        <!-- 헤더 -->
        <div class="min-w-0 pt-5 lg:pt-6">
          <h2 class="truncate text-xl font-semibold text-gray-800 dark:text-white/90">
            {{ name || '사용자' }}
          </h2>
          <div class="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-500 dark:text-gray-400">
            <span>구분: <b class="text-gray-700 dark:text-gray-300">{{ roleLabel }}</b></span>
            <span>소속: <b class="text-gray-700 dark:text-gray-300">{{ orgLabel }}</b></span>
          </div>
        </div>

        <!-- 이메일 / 전화번호 -->
        <div class="mt-6 grid gap-3">
          <!-- 이메일 (읽기 전용) -->
          <div class="flex items-center gap-3">
            <label class="w-24 shrink-0 text-sm font-medium text-gray-700 dark:text-gray-300">이메일</label>
            <div
                class="h-11 w-full px-2 flex items-center bg-transparent border-0
           text-gray-800 dark:text-gray-300 truncate select-text"
                :title="email || '-'"
            >
              <span class="truncate">{{ email || '-' }}</span>
            </div>
          </div>

          <!-- 전화번호 -->
          <div class="flex items-start gap-3">
            <label class="w-24 shrink-0 text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">전화번호</label>
            <div class="w-full">
              <div class="flex gap-3">
                <input
                    v-model="phoneInput"
                    :disabled="savingPhone"
                    placeholder="010-1234-5678"
                    @input="formatPhoneInput"
                    @blur="validatePhoneInput"
                    class="h-11 w-full rounded-lg border px-3
                         bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                         dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                />
                <button
                    type="button"
                    class="h-11 shrink-0 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-50"
                    :disabled="savingPhone || phoneInput === phone"
                    @click="savePhone"
                >
                  {{ savingPhone ? '저장 중...' : '저장' }}
                </button>
              </div>
              <p v-if="phoneError" class="mt-1 text-sm text-error-500">{{ phoneError }}</p>
            </div>
          </div>
        </div>

        <hr class="my-6 border-gray-200 dark:border-gray-700" />

        <!-- 비밀번호 변경 -->
        <div>
          <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">비밀번호 변경</h3>

          <div class="space-y-3">
            <!-- 현재 비밀번호 -->
            <div class="relative">
              <input
                  v-model="currentPassword"
                  :type="showCurrentPw ? 'text' : 'password'"
                  placeholder="현재 비밀번호"
                  class="h-11 w-full rounded-lg border px-3
                         bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                         dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
              />
              <span
                  @click="toggleCurrentPwVisibility"
                  class="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer"
              >
                <EyeIcon v-if="showCurrentPw" class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
              </span>
            </div>

            <!-- 새 비밀번호 -->
            <div class="relative">
              <input
                  v-model="newPassword"
                  :type="showNewPw ? 'text' : 'password'"
                  placeholder="새 비밀번호 (6자 이상)"
                  class="h-11 w-full rounded-lg border px-3
                         bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                         dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
              />
              <span
                  @click="togglePasswordVisibility"
                  class="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer"
              >
                <EyeIcon v-if="showNewPw" class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
              </span>
            </div>

            <!-- 새 비밀번호 확인 -->
            <div class="relative">
              <input
                  v-model="newPassword2"
                  :type="showNewPw2 ? 'text' : 'password'"
                  placeholder="새 비밀번호 확인"
                  class="h-11 w-full rounded-lg border px-3
                         bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                         dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
              />
              <span
                  @click="togglePassword2Visibility"
                  class="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer"
              >
                <EyeIcon v-if="showNewPw2" class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
                <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400"/>
              </span>
            </div>

            <div class="flex justify-end gap-2 pt-1">
              <button
                  type="button"
                  class="h-10 rounded-lg bg-gray-200 px-4 text-gray-800 hover:bg-gray-100 dark:bg-gray-700 dark:text-gray-100"
                  :disabled="changingPw"
                  @click="resetPwForm"
              >
                취소
              </button>
              <button
                  type="button"
                  class="h-10 rounded-lg bg-brand-500 px-4 text-white hover:bg-brand-600 disabled:opacity-50"
                  :disabled="changingPw"
                  @click="changePassword"
              >
                {{ changingPw ? '변경 중...' : '변경' }}
              </button>
            </div>
          </div>

          <hr class="my-6 border-gray-200 dark:border-gray-700" />

          <!-- SUPER 이메일 전용 스프레드시트 설정 (비밀번호 변경과 동일 스타일) -->
          <div v-if="isSuperEmail" class="mt-6">
            <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
              Google 스프레드시트 연동
            </h3>

            <div class="space-y-3">
              <!-- Spreadsheet ID -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Spreadsheet ID
                </label>
                <input
                    v-model="sheetId"
                    :disabled="savingSheet"
                    placeholder="예) 1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
                    @blur="validateSheetId"
                    class="h-11 w-full rounded-lg border px-3
                     bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                />
                <p v-if="sheetIdError" class="mt-1 text-sm text-error-500">{{ sheetIdError }}</p>
              </div>

              <!-- 시작 행 -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  시작 행
                </label>
                <input
                    v-model.number="startRow"
                    :disabled="savingSheet"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="예) 2"
                    @blur="validateStartRow"
                    class="h-11 w-full rounded-lg border px-3
                     bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 spin-dark" />
                <p v-if="startRowError" class="mt-1 text-sm text-error-500">{{ startRowError }}</p>
              </div>

              <!-- 액션 버튼 (비번 변경 섹션과 동일) -->
              <div class="flex justify-end gap-2 pt-1">
                <button
                    type="button"
                    class="h-10 rounded-lg bg-gray-200 px-4 text-gray-800 hover:bg-gray-100
               dark:bg-gray-700 dark:text-gray-100"
                    :disabled="savingSheet"
                    @click="resetSheetForm"
                >
                  초기화
                </button>
                <button
                    type="button"
                    class="h-10 rounded-lg bg-brand-500 px-4 text-white hover:bg-brand-600 disabled:opacity-50"
                    :disabled="savingSheet"
                    @click="saveSheetConfig"
                >
                  {{ savingSheet ? '저장 중...' : '저장' }}
                </button>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import CommonGridShape from '@/components/common/CommonGridShape.vue'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'
import axios from '@/plugins/axios.js'

const email = ref('')
const name  = ref('')
const phone = ref('')

const roleLabel = ref('-')
const orgLabel  = ref('-')

const phoneInput  = ref('')
const savingPhone = ref(false)
const phoneError  = ref('')

/* 비밀번호 */
const currentPassword = ref('')
const newPassword     = ref('')
const newPassword2    = ref('')
const changingPw      = ref(false)

const currentPwError = ref('')
const newPwError     = ref('')
const newPw2Error    = ref('')

// super 권한 확인
const isSuperEmail = ref(false)

/** 서버에서 내 정보 로드 */
async function loadMe() {
  try {
    const { data } = await axios.get('/api/me', { withCredentials: true })
    // console.debug('[MyInfo] /api/me raw:', data)

    name.value  = data.userName
    email.value = data.userEmail
    phone.value = data.userPhone
    if (data.userRole === 'SUPERADMIN') roleLabel.value = '관리자';
    else if (data.userRole === 'MANAGER') roleLabel.value = '센터장';
    else if (data.userRole === 'STAFF') roleLabel.value = '담당자';
    else roleLabel.value = '-';
    orgLabel.value  = data.centerName

    if (data.super) {
      isSuperEmail.value = true
    }

    phoneInput.value = phone.value || ''
  } catch (e) {
    // console.error('[MyInfo] /api/me error:', e?.response?.status, e?.response?.data || e)
    // alert('프로필을 불러오지 못했습니다.')
  }
}
onMounted(loadMe)

/** 전화번호 입력: 항상 010-xxxx-xxxx 형식으로 자동 포맷 */
const formatPhoneInput = () => {
  let digits = (phoneInput.value || '').replace(/\D/g, '')
  if (digits.length <= 3) phoneInput.value = digits
  else if (digits.length <= 7) phoneInput.value = `${digits.slice(0,3)}-${digits.slice(3)}`
  else phoneInput.value = `${digits.slice(0,3)}-${digits.slice(3,7)}-${digits.slice(7,11)}`
}

/** 전화번호 검증 (blur 시) */
const validatePhoneInput = () => {
  if (!phoneInput.value) { phoneError.value = ''; return }
  phoneError.value = /^010-\d{4}-\d{4}$/.test(phoneInput.value)
      ? '' : '전화번호는 010-1234-5678 형식으로 입력해야 합니다.'
}

/** 전화번호 저장 */
async function savePhone() {
  validatePhoneInput()
  if (phoneError.value) {
    alert(phoneError.value) // 회원가입과 동일하게 제출 시에는 alert로도 막기
    return
  }
  try {
    savingPhone.value = true
    await axios.put('/api/me/phone', phoneInput.value, {
      headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
      withCredentials: true,
    });
    phone.value = phoneInput.value
    alert('전화번호가 저장되었습니다.')
  } catch (e) {
    console.error('전화번호 저장 실패:', e?.response?.status, e?.response?.data || e)
    alert('전화번호 저장 실패')
  } finally {
    savingPhone.value = false
  }
}

/** 비밀번호 보기 아이콘 토글 */
const showCurrentPw = ref(false)
const showNewPw = ref(false)
const showNewPw2 = ref(false)

const toggleCurrentPwVisibility = () => { showCurrentPw.value = !showCurrentPw.value }
const togglePasswordVisibility   = () => { showNewPw.value = !showNewPw.value }
const togglePassword2Visibility  = () => { showNewPw2.value = !showNewPw2.value }

/** 비밀번호 폼 리셋 */
function resetPwForm() {
  currentPassword.value = ''
  newPassword.value = ''
  newPassword2.value = ''
  currentPwError.value = ''
  newPwError.value = ''
  newPw2Error.value = ''
}

/** 비밀번호 검증 (blur 시) */
const validateCurrentPassword = () => {
  // 비워두면 메시지 감춤
  currentPwError.value = currentPassword.value ? '' : ''
}
const validateNewPassword = () => {
  if (!newPassword.value) { newPwError.value = ''; return }
  newPwError.value = newPassword.value.length < 6 ? '비밀번호는 최소 6자리 이상이어야 합니다.' : ''
}
const validateNewPasswordConfirm = () => {
  if (!newPassword2.value) { newPw2Error.value = ''; return }
  newPw2Error.value = (newPassword.value !== newPassword2.value) ? '비밀번호가 일치하지 않습니다.' : ''
}

// 입력 중에도 즉시 불일치 문구 업데이트
watch([newPassword, newPassword2], () => {
  if (!newPassword2.value) { newPw2Error.value = ''; return }
  newPw2Error.value = (newPassword.value !== newPassword2.value) ? '비밀번호가 일치하지 않습니다.' : ''
})

/** 비밀번호 변경 */
async function changePassword() {
  // 회원가입과 동일하게 제출 시에도 한 번 더 게이트 + alert
  if (!currentPassword.value || !newPassword.value) {
    currentPwError.value = !currentPassword.value ? '현재 비밀번호를 입력하세요.' : ''
    newPwError.value = !newPassword.value ? '새 비밀번호를 입력하세요.' : newPwError.value
    alert('비밀번호를 입력하세요.')
    return
  }
  if (newPassword.value.length < 6) {
    newPwError.value = '비밀번호는 최소 6자리 이상이어야 합니다.'
    alert('새 비밀번호는 6자 이상이어야 합니다.')
    return
  }
  if (newPassword.value !== newPassword2.value) {
    newPw2Error.value = '비밀번호가 일치하지 않습니다.'
    alert('새 비밀번호가 일치하지 않습니다.')
    return
  }

  try {
    changingPw.value = true
    await axios.put('/api/me/password', {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    }, { withCredentials: true })
    alert('비밀번호가 변경되었습니다. 다음 로그인부터 적용됩니다.')
    resetPwForm()
  } catch (e) {
    console.error('비밀번호 변경 실패 :', e?.response?.status, e?.response?.data || e)
    console.error(e)
    if (e.response?.status === 400) {
      alert(e?.response?.data || '')
    } else if (e.response?.status === 401) {
      alert('다시 로그인 후 시도하세요.')
    } else {
      alert('변경 실패')
    }
  } finally {
    changingPw.value = false
  }
}

// 슈퍼 전용 입력칸
const sheetId = ref('')
const startRow = ref(1)

async function saveSheetSettings() {
  try {
    await axios.put('/api/me/sheet-settings', {
      sheetId: sheetId.value,
      startRow: startRow.value,
    }, { withCredentials: true })
    alert('시트 설정이 저장되었습니다.')
  } catch (e) {
    console.error('시트 정보 저장 실패 :', e)
    alert('저장 실패')
  }
}
</script>

<style>
.fade-enter-active, .fade-leave-active { transition: opacity .15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 다크모드에서 네이티브 컨트롤(스피너 포함)을 다크 스킨으로 */
.dark .spin-dark {
  color-scheme: dark;
}

.dark .spin-dark::-webkit-inner-spin-button,
.dark .spin-dark::-webkit-outer-spin-button {
  filter: invert(2) brightness(0.5) contrast(1.6) opacity(.2);
}
</style>
