import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

/**
 * Vite 构建配置。
 *
 * 使用函数式写法以便根据 mode 读取对应的 .env.[mode] 文件：
 * - 开发环境通过 server.proxy 把 /api 前缀的请求转发到后端，规避浏览器跨域限制；
 * - 生产环境直接输出静态资源，由 Nginx 等反向代理与后端同域部署。
 *
 * @see https://vite.dev/config/
 */
export default defineConfig(({ mode }) => {
  // 第三个参数传空串，表示不按 VITE_ 前缀过滤，方便读取 VITE_PROXY_TARGET 等自定义变量
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],

    resolve: {
      alias: {
        // 使用 @ 指向 src，避免多层相对路径 ../../..
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },

    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_PORT) || 5173,
      // 端口被占用时直接报错退出，而不是静默切换到其他端口，避免前后端代理配置错位
      strictPort: true,
      open: false,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          // 后端接口本身就以 /api 开头，因此不做路径重写
        },
      },
    },

    preview: {
      host: '0.0.0.0',
      port: 4173,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },

    css: {
      preprocessorOptions: {
        scss: {
          // 统一使用 Sass 现代编译器 API，消除 legacy JS API 的弃用告警
          api: 'modern-compiler',
          charset: false,
        },
      },
    },

    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      // 生产包默认不输出 sourcemap（体积与源码泄露的权衡），需要排查线上问题时置为 true
      sourcemap: env.VITE_BUILD_SOURCEMAP === 'true',
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          /**
           * 手动拆包：把体积较大的第三方库独立成 chunk，利用浏览器长缓存。
           *
           * 注意：Vite 8 的类型只接受**函数形式**的 manualChunks（对象形式已不被类型支持），
           * 因此这里按模块路径判断所属依赖。未命中的模块交回 Vite 默认策略，
           * 不做「全部塞进 vendor」的粗粒度拆分，以免破坏按需加载与模块初始化顺序。
           */
          manualChunks(id: string) {
            if (!id.includes('node_modules')) {
              return
            }
            // 路径中含 element-plus 的模块，同时覆盖 @element-plus/icons-vue
            if (id.includes('element-plus')) {
              return 'element'
            }
            if (/[\\/]node_modules[\\/](vue|@vue|vue-router|pinia)[\\/]/.test(id)) {
              return 'vue'
            }
          },
        },
      },
    },
  }
})
