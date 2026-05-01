/** 登录态失效时由 axios 拦截器 reject，避免再弹错误提示（将跳转登录页）。 */
export class SilentSessionRedirect extends Error {
  constructor() {
    super('SilentSessionRedirect')
    this.name = 'SilentSessionRedirect'
  }
}
