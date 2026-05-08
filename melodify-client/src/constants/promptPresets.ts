/**
 * 创作页「规则 + 预设库」：纯前端数据，用于拼装 Suno 提示与自定义字段。
 * 不调用大模型；用户点击标签时在本地合并字符串。
 */

/** 展示在侧栏 / 折叠面板顶部的撰写规则（短句） */
export const PROMPT_COMPOSITION_RULES = [
  '简单模式：一句话里尽量包含「风格 + 情绪 + 场景 + 人声/乐器」中的至少三项，模型更容易对齐。',
  '自定义 + 人声：提示写在「创作描述」，逐行歌词只放在「精确歌词」，避免混在一处。',
  '自定义 + 纯器乐：标题、风格必填；氛围描述可写乐器、速度与情绪关键词。',
  '中英文混写可以，但主体建议用一种语言描述，减少模型跑偏。',
]

/** 整段可直接填入「创作描述」或器乐氛围的示例（简单模式 / 器乐） */
export const FULL_PROMPT_PRESETS = [
  {
    id: 'dream-pop-night',
    label: '梦幻流行 · 城市夏夜',
    text: '夏夜城市里的梦幻流行，女声，带一点电子氛围，副歌旋律好记、情绪层层推进',
  },
  {
    id: 'cafe-jazz',
    label: '咖啡馆 · 轻爵士',
    text: '适合咖啡馆背景的轻爵士，温暖松弛，钢琴与贝斯对话感强，鼓点轻柔不抢戏',
  },
  {
    id: 'guofeng-electronic',
    label: '国风电子',
    text: '国风与电子融合，古筝或琵琶类音色点缀，描写远山与月光，节奏由慢到快',
  },
  {
    id: 'indie-folk-dawn',
    label: '独立民谣 · 清晨',
    text: '独立民谣，男声叙述感，木吉他为主，主题是破晓前赶路的人，情绪克制含蓄',
  },
  {
    id: 'synthwave',
    label: '合成器浪潮',
    text: '80 年代合成器浪潮，驾驶夜景公路，鼓组紧实，贝斯线条明显，带少量人声切片',
  },
  {
    id: 'lofi-study',
    label: 'Lo-fi · 学习向',
    text: 'Lo-fi hip hop，低保真颗粒感，爵士和弦，适合当作背景，无刺耳高频',
  },
]

/** 按维度点击：拼接到当前描述末尾；每项为可点选片段 */
export interface PromptDimension {
  id: string
  title: string
  /** 标签文案 */
  tags: string[]
}

export const PROMPT_DIMENSIONS: PromptDimension[] = [
  {
    id: 'genre',
    title: '风格 / 流派',
    tags: [
      '梦幻流行',
      '独立摇滚',
      '爵士',
      '电子',
      '国风',
      '民谣',
      'R&B',
      '嘻哈',
      'Lo-fi',
      '后摇',
    ],
  },
  {
    id: 'mood',
    title: '情绪',
    tags: [
      '温暖',
      '克制',
      '忧郁',
      '治愈',
      '激昂',
      '空灵',
      '复古',
      '浪漫',
      '孤独感',
      '希望感',
    ],
  },
  {
    id: 'vocal',
    title: '人声',
    tags: ['女声主唱', '男声主唱', '双人和声', '气声为主', '力量型主唱', '轻声叙述', '无词人声垫'],
  },
  {
    id: 'instrument',
    title: '乐器 / 音色',
    tags: [
      '木吉他',
      '电吉他',
      '钢琴',
      '弦乐组',
      '合成器铺底',
      '贝斯线条明显',
      '鼓组紧实',
      '古筝点缀',
      '萨克斯',
    ],
  },
  {
    id: 'scene',
    title: '场景 / BPM 感',
    tags: [
      '城市夜景',
      '海边日落',
      '雨天室内',
      '公路驾驶',
      '清晨公园',
      '中速四拍',
      '慢板',
      '副歌加大编制',
    ],
  },
]

/** 自定义模式 ·「风格」快选：界面显示中文，填入仍为英文提示词（兼容 Suno） */
export type StyleQuickPreset = { label: string; value: string }

export const STYLE_QUICK_PRESETS: StyleQuickPreset[] = [
  {
    label: '梦幻流行 · 女声 · 合成铺底',
    value: 'Dream pop, female vocal, lush synth pads, 110 BPM',
  },
  {
    label: '民谣 · 木吉他 · 男声叙述',
    value: 'Acoustic folk, male vocal, fingerpicked guitar, intimate',
  },
  {
    label: '国风融合 · 古筝 · 现代鼓',
    value: 'Chinese guofeng fusion, guzheng plucks, modern drums, dramatic',
  },
  {
    label: 'Lo-fi · 低保真 · 松弛',
    value: 'Lo-fi hip hop, dusty drums, warm bass, chill',
  },
  {
    label: '独立摇滚 · 失真吉他',
    value: 'Indie rock, distorted guitars, driving drums, anthemic chorus',
  },
  {
    label: 'Neo-soul · 电钢琴 · 紧密律动',
    value: 'Neo-soul, Rhodes, tight groove, stacked harmonies',
  },
]

/** 标题灵感（填入成品标题） */
export const TITLE_IDEA_PRESETS = [
  '晚风回信',
  '未命名公路',
  '玻璃城市',
  '远山与潮汐',
  '凌晨三点半的咖啡',
  '给明天的草稿',
]

/** 自定义 + 人声：歌词骨架（占位符可改） */
export const LYRIC_SKELETONS = [
  {
    id: 'verse-chorus',
    label: '主歌-副歌',
    text: `[主歌]
（写画面与情绪，不必押韵太死）

[副歌]
（重复记忆点，一句核心 hook）

[主歌2]
（递进或换角度）

[副歌]
（可重复或微调）

[尾奏]
（一句收束或留白）`,
  },
  {
    id: 'short-pop',
    label: '短歌结构',
    text: `第一节 · 叙述
第二节 · 转折
副歌 · 情绪爆发
桥段 · 换一个意象
副歌 · 回归`,
  },
  {
    id: 'cn-gufeng',
    label: '国风向意象',
    text: `[起]
风起时 / 旧巷深处

[承]
月色 / 舟楫 / 一杯凉酒

[转]
有人折柳 / 也有人远行

[合]
灯火与远山 / 都成回响`,
  },
]

/** 将片段用中文逗号拼入现有文案；避免重复连续标点 */
export function appendPromptFragment(current: string, fragment: string): string {
  const f = fragment.trim()
  if (!f) return current.trim()
  const base = current.trim()
  if (!base) return f
  const endsPunct = /[，、；。！？⋯,\.!?]$/.test(base)
  const joiner = endsPunct ? '' : '，'
  return `${base}${joiner}${f}`
}
