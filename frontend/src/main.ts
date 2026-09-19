import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'

// Element Plus 样式必须在全局样式之前引入，保证自定义覆盖具有更高优先级
import 'element-plus/dist/index.css'
import '@/styles/index.scss'

const app = createApp(App)

/**
 * 全局注册 Element Plus 图标。
 *
 * 示例项目采用全量注册以换取模板书写的简洁（可直接写 `<el-icon><List /></el-icon>`）。
 * 对包体积敏感的生产项目应改为按需引入：
 * `import { List } from '@element-plus/icons-vue'` 后在组件内局部注册，
 * 或使用 unplugin-vue-components + unplugin-auto-import 自动导入。
 */
Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
  app.component(name, component)
})

app.use(createPinia())
app.use(router)
// 中文语言包：分页、日期选择器、表格空状态等内置文案随之本地化
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
