#!/usr/bin/env node
/**
 * npm postinstall：在 Windows 32 位 Node 下提示无法使用 Vite 8 / Rolldown（无 ia32 原生绑定）。
 * 不影响安装退出码；仅为开发者控制台告警。
 */
import { arch, platform } from 'node:process'

if (platform === 'win32' && arch === 'ia32') {
  console.warn(
    '\n[melodify-client] 当前为 32 位 Node.js (ia32)。Vite 8 / Rolldown 不提供 Windows ia32 原生模块。\n' +
      '请改用 64 位 Node.js：https://nodejs.org/\n',
  )
}
