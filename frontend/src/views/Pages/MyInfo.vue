<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="내 정보 수정" />

    <div
        class="relative overflow-hidden rounded-2xl border border-gray-200 bg-white p-0
             dark:border-gray-800 dark:bg-white/[0.03]"
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
        <div class="min-w-0 pt-5 lg:pt-6 mb-10">
          <h2 class="truncate text-xl font-semibold text-gray-800 dark:text-white/90">
            {{ name || '사용자' }}
          </h2>
          <div class="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-500 dark:text-gray-400">
            <span>구분: <b class="text-gray-700 dark:text-gray-300">{{ roleLabel }}</b></span>
            <span>소속: <b class="text-gray-700 dark:text-gray-300">{{ orgLabel }}</b></span>
          </div>
          <span v-if="!isVerified" class="text-gray-500 dark:text-gray-400 text-sm" >이메일 인증 후 내 정보 수정이 가능합니다.</span>
        </div>

        <!-- 공통 2열 폼 그리드 -->
        <div class="form-grid grid grid-cols-[6rem,1fr] items-start gap-x-3 gap-y-3">

          <!-- 이메일 -->
          <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">이메일</div>
          <div class="col-start-2">
            <div class="flex items-center gap-3">
              <span
                  class="truncate flex-1 min-w-[12rem] text-gray-800 dark:text-gray-300 ml-2"
                  :title="email || '-'"
              >
                {{ email || '-' }}
              </span>
              <button
                  type="button"
                  class="h-11 px-4 rounded-lg text-sm font-medium text-white transition
               bg-brand-500 hover:bg-brand-600 disabled:opacity-50 ml-auto"
                  :disabled="sendingCode || isVerified"
                  @click="sendVerify"
              >
                {{ isVerified ? '인증완료' : (sendingCode ? '전송 중...' : (codeSent ? '&nbsp;&nbsp;재요청&nbsp;&nbsp;' : '인증요청')) }}
              </button>
            </div>
          </div>

          <!-- (2번째 화면) 인증코드 입력: codeSent && !isVerified -->
          <template v-if="codeSent && !isVerified">
            <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">인증번호</div>
            <div class="col-start-2">
              <div class="flex items-center gap-2 flex-wrap sm:flex-nowrap">
                <input
                    v-model="verifyCode"
                    placeholder="메일로 받은 6자리"
                    class="h-11 flex-1 min-w-[12rem] rounded-lg border px-3
                 bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                />
                <button
                    type="button"
                    class="h-11 px-4 rounded-lg text-sm font-medium
                 bg-gray-100 hover:bg-gray-200 text-gray-700
                 dark:bg-white/5 dark:text-white/90 dark:hover:bg-white/10"
                    :disabled="verifying"
                    @click="verifyNow"
                >
                  {{ verifying ? '확인 중...' : '인증하기' }}
                </button>
              </div>
            </div>

            <div class="text-sm font-medium text-gray-700 dark:text-gray-300">남은시간</div>
            <div class="col-start-2">
              <div class="flex items-center justify-between">
                <p class="text-sm text-blue-600">{{ mm }}:{{ ss }}</p>
                <button
                    type="button"
                    class="px-3 py-2 rounded-lg text-sm
                 text-gray-700 dark:text-gray-300
                 hover:text-brand-500 disabled:opacity-50"
                    :disabled="extendedOnce"
                    @click="extendTime"
                >
                  &nbsp;&nbsp;1회 연장&nbsp;&nbsp;
                </button>
              </div>
            </div>
          </template>

          <!-- (3번째 화면) 인증 완료 후: isVerified -->
          <template v-if="isVerified">
            <!-- 전화번호 -->
            <div class="text-sm font-medium text-gray-700 dark:text-gray-300 mt-2">전화번호</div>
            <div class="col-start-2">
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

            <!-- 구분선 -->
            <div class="col-span-2">
              <hr class="my-6 border-gray-200 dark:border-gray-700" />
            </div>

            <!-- 비밀번호 변경 제목 -->
            <div class="col-span-2">
              <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">비밀번호 변경</h3>
            </div>

            <!-- 현재 비밀번호 -->
            <div class="text-sm font-medium text-gray-700 dark:text-gray-300">현재 비밀번호</div>
            <div class="col-start-2">
              <div class="relative">
                <input
                    v-model="currentPassword"
                    :type="showCurrentPw ? 'text' : 'password'"
                    placeholder="현재 비밀번호"
                    class="h-11 w-full rounded-lg border px-3
                 bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                    @blur="validateCurrentPassword"
                />
                <span
                    @click="toggleCurrentPwVisibility"
                    class="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer"
                >
          <EyeIcon v-if="showCurrentPw" class="w-5 h-5 text-gray-500 dark:text-gray-400" />
          <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400" />
        </span>
              </div>
              <p v-if="currentPwError" class="text-sm text-error-500">{{ currentPwError }}</p>
            </div>

            <!-- 새 비밀번호 -->
            <div class="text-sm font-medium text-gray-700 dark:text-gray-300">새 비밀번호</div>
            <div class="col-start-2">
              <div class="relative">
                <input
                    v-model="newPassword"
                    :type="showNewPw ? 'text' : 'password'"
                    placeholder="새 비밀번호 (6자 이상)"
                    class="h-11 w-full rounded-lg border px-3
                 bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                    @blur="validateNewPassword"
                />
                <span
                    @click="togglePasswordVisibility"
                    class="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer"
                >
          <EyeIcon v-if="showNewPw" class="w-5 h-5 text-gray-500 dark:text-gray-400" />
          <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400" />
        </span>
              </div>
              <p v-if="newPwError" class="text-sm text-error-500">{{ newPwError }}</p>
            </div>

            <!-- 새 비밀번호 확인 -->
            <div class="text-sm font-medium text-gray-700 dark:text-gray-300">비밀번호 확인</div>
            <div class="col-start-2">
              <div class="relative">
                <input
                    v-model="newPassword2"
                    :type="showNewPw2 ? 'text' : 'password'"
                    placeholder="새 비밀번호 확인"
                    class="h-11 w-full rounded-lg border px-3
                 bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                    @blur="validateNewPasswordConfirm"
                />
                <span
                    @click="togglePassword2Visibility"
                    class="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer"
                >
          <EyeIcon v-if="showNewPw2" class="w-5 h-5 text-gray-500 dark:text-gray-400" />
          <EyeSlashIcon v-else class="w-5 h-5 text-gray-500 dark:text-gray-400" />
        </span>
              </div>
              <p v-if="newPw2Error" class="text-sm text-error-500">{{ newPw2Error }}</p>
            </div>

            <!-- 비밀번호 버튼 -->
            <div></div>
            <div class="col-start-2">
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

            <!-- SUPER 이메일 전용 -->
            <template v-if="isSuperEmail">
              <div class="col-span-2">
                <hr class="my-6 border-gray-200 dark:border-gray-700" />
              </div>

              <div class="col-span-2">
                <h3 class="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">Google 스프레드시트 연동</h3>
              </div>

              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">Spreadsheet ID</div>
              <div class="col-start-2">
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

              <div class="text-sm font-medium text-gray-700 dark:text-gray-300">시작 행</div>
              <div class="col-start-2">
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
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 spin-dark"
                />
                <p v-if="startRowError" class="mt-1 text-sm text-error-500">{{ startRowError }}</p>
              </div>

              <div></div>
              <div class="col-start-2">
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
            </template>
          </template>

          <!-- 바닥 여백 확보를 위한 구분선 -->
          <div class="col-span-2 mb-6">
<!--            <hr class="my-6 border-gray-200 dark:border-gray-700" />-->
          </div>

        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import CommonGridShape from '@/components/common/CommonGridShape.vue'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'
import axios from '@/plugins/axios.js'
import { useMailStore } from '@/stores/mail'
import { onBeforeRouteLeave } from 'vue-router'

/* ===== 기본 프로필 ===== */
const email = ref('')
const name  = ref('')
const phone = ref('')

const roleLabel = ref('-')
const orgLabel  = ref('-')

/* 전화번호 */
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

/* SUPER(스프레드시트) */
const isSuperEmail = ref(false)
const sheetId       = ref('')
const startRow      = ref(1)
const savingSheet   = ref(false)
const sheetIdError  = ref('')
const startRowError = ref('')

/* 비번 보기 토글 */
const showCurrentPw = ref(false)
const showNewPw     = ref(false)
const showNewPw2    = ref(false)
const toggleCurrentPwVisibility = () => { showCurrentPw.value = !showCurrentPw.value }
const togglePasswordVisibility   = () => { showNewPw.value = !showNewPw.value }
const togglePassword2Visibility  = () => { showNewPw2.value = !showNewPw2.value }

// 화면에 찍힐 남은 시간
const mail = useMailStore()

let ttlTimer = null
const extendedOnce = ref(false)
const mm = computed(() => String(Math.floor((mail.ttl ?? 0) / 60)).padStart(2, '0'))
const ss = computed(() => String((mail.ttl ?? 0) % 60).padStart(2, '0'))

function startTtlTimer() {
  stopTtlTimer()
  ttlTimer = setInterval(() => {
    if (mail.ttl > 0) {
      // Pinia 상태를 1초씩 감소 (UI가 즉시 반응)
      mail.ttl -= 1
    } else {
      // TTL 만료 시 입력 행 접고(선택), 타이머 종료
      stopTtlTimer()
      codeSent.value = false
    }
  }, 1000)
}

function stopTtlTimer() {
  if (ttlTimer) {
    clearInterval(ttlTimer)
    ttlTimer = null
  }
}
onUnmounted(() => stopTtlTimer())

onMounted(async () => {
  await loadMe()
  if (!mail.email) mail.email = email.value

  // 코드 요청 버튼 누른 상태(codeSent=true)일 때만 TTL 확인
  if (codeSent.value) {
    try {
      const t = await mail.getCodeTtl()
      if (t > 0 && !mail.codeVerified) {
        codeSent.value = true
        extendedOnce.value = false
        startTtlTimer()
      } else {
        codeSent.value = false
        stopTtlTimer()
      }
    } catch (e) {
      if (e?.response?.status === 410) {
        codeSent.value = false
        stopTtlTimer()
      } else {
        console.error('getCodeTtl 실패', e)
      }
    }
  }
})

/* ===== 메일 인증 (인라인) ===== */
const codeSent   = ref(false)
const sendingCode = ref(false)
const verifying   = ref(false)
const verifyCode  = ref('')

// TTL 확인용
const verified = ref(false)
const isVerified = computed(() => verified.value)

/* 서버에서 내 정보 로드 */
async function loadMe() {
  try {
    const { data } = await axios.get('/api/me', { withCredentials: true })
    name.value  = data.userName
    email.value = data.userEmail
    phone.value = data.userPhone
    mail.email  = data.userEmail

    if (data.userRole === 'SUPERADMIN') roleLabel.value = '관리자'
    else if (data.userRole === 'MANAGER') roleLabel.value = '센터장'
    else if (data.userRole === 'STAFF') roleLabel.value = '담당자'
    else roleLabel.value = '-'

    orgLabel.value   = data.centerName || '미할당'
    isSuperEmail.value = !!data.super

    phoneInput.value = phone.value || ''
  } catch (_) {
    /* 프로필 로드 실패는 조용히 무시 */
  }
}

/* 인증: 코드 전송 */
async function sendVerify() {
  if (!email.value) {
    alert('이메일 정보를 불러오지 못했습니다.')
    return
  }
  try {
    sendingCode.value = true
    mail.email = email.value
    const res = await mail.sendCode(email.value)
    if (!res) return
    codeSent.value = true
    extendedOnce.value = false
    startTtlTimer() // 전송 후 바로 카운트다운 시작
  } finally {
    sendingCode.value = false
  }
}

/* 인증: 코드 확인 */
async function verifyNow() {
  if (!verifyCode.value) {
    alert('인증코드를 입력하세요.')
    return
  }
  // store에 대상 이메일 지정
  if (!mail.email) mail.email = email.value
  const ok = await mail.verifyCode(verifyCode.value)
  if (!ok) return

  // 인증 성공 → 코드 입력행 숨김, TTL 타이머는 의미 없어졌으니 정지
  verified.value = true
  stopTtlTimer()
  codeSent.value = false
}

/* 인증: 1회 연장 */
async function extendTime() {
  if (extendedOnce.value) return
  if (!mail.email) mail.email = email.value
  const res = await mail.extendCode() // mail.ttl 갱신됨
  if (!res) return
  extendedOnce.value = true
  startTtlTimer() // 연장 후 다시 카운트다운 (TTL이 늘어났으니 초기화)
}

/* 인증: 뒤로가기 잔상 방지 */
function resetEmailFlow() {
  verified.value = false
  codeSent.value = false
  stopTtlTimer()
  // 선택: mail.reset?.()  // 스토어까지 비우고 싶으면
}
onBeforeRouteLeave(() => { resetEmailFlow() })
onUnmounted(() => { resetEmailFlow() })

/* ===== 전화번호 ===== */
const formatPhoneInput = () => {
  let digits = (phoneInput.value || '').replace(/\D/g, '')
  if (digits.length <= 3) phoneInput.value = digits
  else if (digits.length <= 7) phoneInput.value = `${digits.slice(0,3)}-${digits.slice(3)}`
  else phoneInput.value = `${digits.slice(0,3)}-${digits.slice(3,7)}-${digits.slice(7,11)}`
}
const validatePhoneInput = () => {
  if (!phoneInput.value) { phoneError.value = ''; return }
  phoneError.value = /^010-\d{4}-\d{4}$/.test(phoneInput.value)
      ? '' : '전화번호는 010-1234-5678 형식으로 입력해야 합니다.'
}
async function savePhone() {
  validatePhoneInput()
  if (phoneError.value) {
    alert(phoneError.value)
    return
  }
  try {
    savingPhone.value = true
    await axios.put('/api/me/phone', phoneInput.value, {
      headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
      withCredentials: true
    })
    phone.value = phoneInput.value
    alert('전화번호가 저장되었습니다.')
    resetIdleTimer() // 활동으로 간주하여 유휴타이머 리셋(선택)
  } catch (e) {
    alert(e?.response?.data || '전화번호 저장 실패')
  } finally {
    savingPhone.value = false
  }
}

/* ===== 비밀번호 ===== */
function resetPwForm() {
  currentPassword.value = ''
  newPassword.value = ''
  newPassword2.value = ''
  currentPwError.value = ''
  newPwError.value = ''
  newPw2Error.value = ''
}
const validateCurrentPassword = () => {
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
watch([newPassword, newPassword2], () => {
  if (!newPassword2.value) { newPw2Error.value = ''; return }
  newPw2Error.value = (newPassword.value !== newPassword2.value) ? '비밀번호가 일치하지 않습니다.' : ''
})
async function changePassword() {
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
      newPassword: newPassword.value
    }, { withCredentials: true })
    alert('비밀번호가 변경되었습니다. 다음 로그인부터 적용됩니다.')
    resetPwForm()
    resetIdleTimer() // 활동으로 간주하여 유휴타이머 리셋(선택)
  } catch (e) {
    if (e.response?.status === 400) {
      alert(e?.response?.data || '입력값 확인')
    } else if (e.response?.status === 401) {
      alert('다시 로그인 후 시도하세요.')
    } else {
      alert('변경 실패')
    }
  } finally {
    changingPw.value = false
  }
}

/* ===== 스프레드시트 ===== */
function validateSheetId() {
  const v = (sheetId.value || '').trim()
  sheetIdError.value = v ? '' : 'Spreadsheet ID를 입력하세요.'
}
function validateStartRow() {
  const n = Number(startRow.value || 0)
  startRowError.value = n >= 1 ? '' : '시작 행은 1 이상의 정수여야 합니다.'
}
function resetSheetForm() {
  sheetId.value = ''
  startRow.value = 1
  sheetIdError.value = ''
  startRowError.value = ''
}
async function saveSheetConfig() {
  validateSheetId()
  validateStartRow()
  if (sheetIdError.value || startRowError.value) {
    alert('입력값을 확인하세요.')
    return
  }
  try {
    savingSheet.value = true
    await axios.put('/api/me/sheet-settings', {
      sheetId: sheetId.value,
      startRow: startRow.value
    }, { withCredentials: true })
    alert('시트 설정이 저장되었습니다.')
    resetIdleTimer() // 활동으로 간주하여 유휴타이머 리셋(선택)
  } catch (e) {
    alert(e?.response?.data || '시트 정보 저장 실패')
  } finally {
    savingSheet.value = false
  }
}

/* ===== 인증 완료 후 3분 유휴 시 자동 새로고침 ===== */
const IDLE_REFRESH_MS = 3 * 60 * 1000
let idleTimer = null
let detachIdleListeners = null

function refreshNow() {
  window.location.reload()
}

function resetIdleTimer() {
  if (idleTimer) clearTimeout(idleTimer)
  idleTimer = setTimeout(refreshNow, IDLE_REFRESH_MS)
}

function startIdleWatch() {
  stopIdleWatch() // 중복 방지

  const handler = () => resetIdleTimer()

  const events = ['mousemove', 'mousedown', 'keydown', 'scroll', 'wheel', 'touchstart', 'input', 'visibilitychange']
  events.forEach(ev => window.addEventListener(ev, handler, { passive: true }))

  // 시작 즉시 타이머 설정
  resetIdleTimer()

  detachIdleListeners = () => {
    events.forEach(ev => window.removeEventListener(ev, handler))
  }
}

function stopIdleWatch() {
  if (idleTimer) { clearTimeout(idleTimer); idleTimer = null }
  if (detachIdleListeners) { detachIdleListeners(); detachIdleListeners = null }
}

/* 인증 상태가 바뀌면 유휴감시 on/off */
watch(isVerified, (ok) => {
  if (ok) startIdleWatch()
  else    stopIdleWatch()
}, { immediate: true })

onUnmounted(() => stopIdleWatch())
</script>

<style>
.fade-enter-active, .fade-leave-active { transition: opacity .15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 다크모드에서 네이티브 컨트롤(스피너 포함)을 다크 스킨으로 */
.dark .spin-dark { color-scheme: dark; }
.dark .spin-dark::-webkit-inner-spin-button,
.dark .spin-dark::-webkit-outer-spin-button {
  filter: invert(2) brightness(0.5) contrast(1.6) opacity(.2);
}

/* 슬라이드 전환 */
.slide-enter-active, .slide-leave-active {
  transition: max-height .28s ease, opacity .2s ease;
}
.slide-enter-from, .slide-leave-to {
  max-height: 0;
  opacity: 0;
}
.slide-enter-to, .slide-leave-from {
  max-height: 2000px;
  opacity: 1;
}
</style>
