/** 会话失效后由 axios 拦截器 reject，不向用户弹出错误文案（已由路由跳转登录）。 */
export class SilentSessionRedirect extends Error {
  constructor() {
    super('SilentSessionRedirect')
    this.name = 'SilentSessionRedirect'
  }
}
