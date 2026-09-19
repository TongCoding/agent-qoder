/// <reference types="vite/client" />

/**
 * 环境变量类型声明。
 *
 * 声明后在代码中使用 `import.meta.env.VITE_XXX` 可获得类型提示与拼写校验。
 * 注意：只有以 VITE_ 开头的变量会被 Vite 注入到客户端 bundle，
 * 切勿在此放置任何密钥类信息。
 */
interface ImportMetaEnv {
  /** 页面标题 */
  readonly VITE_APP_TITLE: string
  /** Axios 基础路径，例如 /api/v1 */
  readonly VITE_API_BASE_URL: string
  /** 开发服务器代理目标（仅构建期使用，不会注入客户端） */
  readonly VITE_PROXY_TARGET?: string
  /** 开发服务器端口（仅构建期使用） */
  readonly VITE_PORT?: string
  /** 后端 Swagger UI 地址，为空时前端隐藏入口 */
  readonly VITE_SWAGGER_URL?: string
  /** 构建时是否输出 sourcemap，仅 vite build 阶段生效 */
  readonly VITE_BUILD_SOURCEMAP?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
