import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

/**
 * 路由结构：
 *   /            前台（普通用户）：首页、车辆列表、车辆详情、我的订单等
 *   /admin       系统管理员工作台（Layout 侧边栏由 /api/auth/menus 动态渲染）
 *   /store       网点管理员工作台（同一套 Layout，菜单前缀不同）
 *
 * 说明：后台菜单的子路由使用 :pathMatch 兜底，S1 只实现框架与看板，
 * 未实现模块进入 Placeholder 页面，后续阶段逐个替换为真实页面即可，无需改路由。
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layout/PortalLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/portal/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'vehicles',
        name: 'VehicleList',
        component: () => import('@/views/portal/VehicleList.vue'),
        meta: { title: '车辆租赁' }
      },
      {
        path: 'vehicles/:id',
        name: 'VehicleDetail',
        component: () => import('@/views/portal/VehicleDetail.vue'),
        meta: { title: '车辆详情' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/portal/OrderList.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/portal/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'checkout/:vehicleId',
        name: 'Checkout',
        component: () => import('@/views/portal/Checkout.vue'),
        meta: { title: '确认订单', requiresAuth: true }
      },
      {
        path: 'cashier/:orderId',
        name: 'Cashier',
        component: () => import('@/views/portal/Cashier.vue'),
        meta: { title: '收银台', requiresAuth: true }
      },
      {
        path: 'credit',
        name: 'Credit',
        component: () => import('@/views/portal/Credit.vue'),
        meta: { title: '信用中心', requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/portal/ComingSoon.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/portal/ComingSoon.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['SYS_ADMIN'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'credit/rules',
        name: 'AdminCreditRules',
        component: () => import('@/views/admin/credit/RuleConfig.vue'),
        meta: { title: '信用规则' }
      },
      {
        path: 'credit/users',
        name: 'AdminCreditUsers',
        component: () => import('@/views/admin/credit/UserCredit.vue'),
        meta: { title: '用户信用' }
      },
      {
        path: 'credit/appeals',
        name: 'AdminCreditAppeals',
        component: () => import('@/views/admin/credit/AppealList.vue'),
        meta: { title: '信用申诉' }
      },
      {
        path: 'credit/blacklist',
        name: 'AdminCreditBlacklist',
        component: () => import('@/views/admin/credit/Blacklist.vue'),
        meta: { title: '风控名单' }
      },
      {
        path: 'dispatch',
        name: 'AdminDispatch',
        component: () => import('@/views/admin/DispatchAudit.vue'),
        meta: { title: '调车审批' }
      },
      {
        // 车辆资产是菜单目录，本身没有页面，进来先落到车辆档案
        path: 'vehicle',
        redirect: '/admin/vehicle/list'
      },
      {
        path: 'vehicle/brands',
        name: 'AdminVehicleBrands',
        component: () => import('@/views/admin/vehicle/BrandManage.vue'),
        meta: { title: '品牌管理' }
      },
      {
        path: 'vehicle/models',
        name: 'AdminVehicleModels',
        component: () => import('@/views/admin/vehicle/ModelManage.vue'),
        meta: { title: '车型管理' }
      },
      {
        path: 'vehicle/list',
        name: 'AdminVehicleList',
        component: () => import('@/views/admin/vehicle/VehicleArchive.vue'),
        meta: { title: '车辆档案' }
      },
      {
        path: 'vehicle/maintenance',
        name: 'AdminVehicleMaintenance',
        component: () => import('@/views/admin/vehicle/MaintenanceLedger.vue'),
        meta: { title: '保养维修' }
      },
      {
        path: 'vehicle/insurance',
        name: 'AdminVehicleInsurance',
        component: () => import('@/views/admin/vehicle/InsuranceList.vue'),
        meta: { title: '保险记录' }
      },
      {
        path: ':pathMatch(.*)*',
        name: 'AdminPlaceholder',
        component: () => import('@/views/admin/Placeholder.vue'),
        meta: { title: '模块建设中' }
      }
    ]
  },
  {
    path: '/store',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['STORE_ADMIN'] },
    children: [
      { path: '', redirect: '/store/overview' },
      {
        path: 'overview',
        name: 'StoreOverview',
        component: () => import('@/views/store/Overview.vue'),
        meta: { title: '网点概览' }
      },
      {
        path: 'vehicles',
        name: 'StoreVehicles',
        component: () => import('@/views/store/VehicleManage.vue'),
        meta: { title: '网点车辆' }
      },
      {
        path: 'dispatch',
        name: 'StoreDispatch',
        component: () => import('@/views/store/DispatchApply.vue'),
        meta: { title: '调车申请' }
      },
      {
        path: 'orders',
        name: 'StoreOrders',
        component: () => import('@/views/store/OrderManage.vue'),
        meta: { title: '网点订单' }
      },
      {
        path: ':pathMatch(.*)*',
        name: 'StorePlaceholder',
        component: () => import('@/views/admin/Placeholder.vue'),
        meta: { title: '网点工作台' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

function homeOf(roleCode) {
  if (roleCode === 'SYS_ADMIN') return '/admin/dashboard'
  if (roleCode === 'STORE_ADMIN') return '/store/overview'
  return '/'
}

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 微租车` : '微租车'

  const token = localStorage.getItem('zuche_token')
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!token) return true

  const userStore = useUserStore()
  if (!userStore.menuLoaded) {
    try {
      await userStore.loadMenus()
    } catch (e) {
      // token 过期或后端不可用：清空登录态，避免反复 401
      userStore.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.roles && !to.meta.roles.includes(userStore.roleCode)) {
    ElMessage.error('当前账号无权访问该工作台')
    return homeOf(userStore.roleCode)
  }
  return true
})

export default router
