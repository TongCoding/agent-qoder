/**
 * 与 UI 浮层相关的通用配置。
 *
 * 放在这里而不是各页面内部，是因为 Element Plus 的默认定位行为对
 * 「窄屏 + 菜单内含长文本」这一组合并不友好，多个列表页会踩到同一个坑。
 */

/**
 * 表格行内操作菜单的 popper 配置。
 *
 * Element Plus 内部把 `preventOverflow` 的 padding 设为四边全 0
 * （见 element-plus/es/components/popper/src/utils.ts 的 genModifiers），
 * 菜单因此允许完全贴住视口边缘——实测移动端展开「更多」菜单时，
 * popper 的 right 恰好等于视口宽度 375，余量为 0，
 * 菜单里的 hint 文案再长一点就会被推出屏幕。
 *
 * 这里留出左右各 8px 的安全边距。popper 的 mergeByName 对同名 modifier
 * 的 options 做浅合并，而 Element Plus 该项除 padding 外没有其他配置，
 * 因此覆盖是无损的；offset / flip / computeStyles 仍沿用内部生成的配置。
 */
export const ACTION_MENU_POPPER_OPTIONS = {
  modifiers: [
    {
      name: 'preventOverflow',
      options: { padding: { top: 0, bottom: 0, left: 8, right: 8 } },
    },
  ],
}
