/**
 * 生成本地演示图片（全部 SVG，离线可用，不依赖任何外网图床）
 *
 * 输出目录：frontend/public/images
 *   brands/brand-{1..9}.svg      品牌 LOGO
 *   cars/car-{1..24}.svg         车型封面（与 seed 中 vehicle_model.id 对应）
 *   cars/gallery/*.svg           内饰 / 仪表 / 尾部通用图
 *   banners/banner-{1..3}.svg    首页轮播
 *   placeholder.svg              图片加载失败兜底
 *   avatar/default.svg           默认头像
 *
 * 运行：node scripts/generate-demo-images.mjs（在 frontend 目录）
 */
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(import.meta.dirname, '..')
const OUT = path.join(ROOT, 'public', 'images')

const BRANDS = [
  { id: 1, name: '大众', initial: 'V' },
  { id: 2, name: '丰田', initial: 'T' },
  { id: 3, name: '本田', initial: 'H' },
  { id: 4, name: '日产', initial: 'N' },
  { id: 5, name: '别克', initial: 'B' },
  { id: 6, name: '比亚迪', initial: 'B' },
  { id: 7, name: '特斯拉', initial: 'T' },
  { id: 8, name: '奥迪', initial: 'A' },
  { id: 9, name: '宝马', initial: 'B' }
]

const CATEGORY = {
  ECONOMY: { label: '经济型', color: '#8fa3b8' },
  COMFORT: { label: '舒适型', color: '#5b9bd5' },
  SUV: { label: 'SUV', color: '#63b37c' },
  LUXURY: { label: '豪华型', color: '#d6a354' },
  EV: { label: '新能源', color: '#3fbfa8' }
}

// body: sedan / suv / mpv / luxury / ev；paint 为车身主色
const MODELS = [
  { id: 1, brandId: 1, name: '朗逸 2023款', cat: 'ECONOMY', body: 'sedan', paint: '#d9dee6' },
  { id: 2, brandId: 1, name: '帕萨特 2023款', cat: 'COMFORT', body: 'sedan', paint: '#243447' },
  { id: 3, brandId: 1, name: '途观L 2022款', cat: 'SUV', body: 'suv', paint: '#5c6b7a' },
  { id: 4, brandId: 1, name: 'ID.4 2022款', cat: 'EV', body: 'ev', paint: '#8fb7c9' },
  { id: 5, brandId: 2, name: '卡罗拉 2023款', cat: 'ECONOMY', body: 'sedan', paint: '#e8e9ea' },
  { id: 6, brandId: 2, name: '凯美瑞 2022款', cat: 'COMFORT', body: 'sedan', paint: '#7d1f2b' },
  { id: 7, brandId: 2, name: 'RAV4荣放 2023款', cat: 'SUV', body: 'suv', paint: '#4a5b4c' },
  { id: 8, brandId: 2, name: 'bZ4X 2023款', cat: 'EV', body: 'ev', paint: '#b9c2c8' },
  { id: 9, brandId: 3, name: '雅阁 2023款', cat: 'COMFORT', body: 'sedan', paint: '#1c2a3a' },
  { id: 10, brandId: 3, name: 'CR-V 2023款', cat: 'SUV', body: 'suv', paint: '#8a9299' },
  { id: 11, brandId: 4, name: '轩逸 2022款', cat: 'ECONOMY', body: 'sedan', paint: '#e3e6e8' },
  { id: 12, brandId: 5, name: '英朗 2022款', cat: 'ECONOMY', body: 'sedan', paint: '#9b1c2e' },
  { id: 13, brandId: 5, name: '君威 2021款', cat: 'COMFORT', body: 'sedan', paint: '#2f3a45' },
  { id: 14, brandId: 5, name: '微蓝6 2022款', cat: 'EV', body: 'ev', paint: '#6f9fae' },
  { id: 15, brandId: 6, name: '宋PLUS 2023款', cat: 'SUV', body: 'suv', paint: '#334a63' },
  { id: 16, brandId: 6, name: '汉EV 2023款', cat: 'EV', body: 'ev', paint: '#8c1d2c' },
  { id: 17, brandId: 6, name: '秦PLUS EV 2023款', cat: 'EV', body: 'sedan', paint: '#dfe3e6' },
  { id: 18, brandId: 6, name: '海豹 2023款', cat: 'EV', body: 'ev', paint: '#1f5f74' },
  { id: 19, brandId: 7, name: 'Model 3 2023款', cat: 'EV', body: 'ev', paint: '#c9ced3' },
  { id: 20, brandId: 7, name: 'Model Y 2023款', cat: 'EV', body: 'suv', paint: '#2a3038' },
  { id: 21, brandId: 8, name: 'A6L 2023款', cat: 'LUXURY', body: 'luxury', paint: '#1a1e24' },
  { id: 22, brandId: 8, name: 'Q5L 2022款', cat: 'LUXURY', body: 'suv', paint: '#4a4f57' },
  { id: 23, brandId: 9, name: '5系 2023款', cat: 'LUXURY', body: 'luxury', paint: '#e6e8ea' },
  { id: 24, brandId: 9, name: '3系 2023款', cat: 'LUXURY', body: 'luxury', paint: '#2b4a7d' }
]

const CABIN = {
  sedan: 'M268 288 L326 214 Q341 196 366 196 H506 Q532 196 548 216 L600 288 Z',
  suv: 'M252 288 L300 194 Q312 174 340 174 H528 Q556 174 568 196 L614 288 Z',
  mpv: 'M248 288 L286 184 Q296 166 326 166 H558 Q586 166 596 190 L628 288 Z',
  luxury: 'M264 288 L332 224 Q347 208 374 208 H502 Q529 208 544 226 L596 288 Z',
  ev: 'M264 288 L320 204 Q335 186 365 186 H512 Q542 186 556 210 L604 288 Z'
}

const WINDOWS = {
  sedan: [
    'M292 276 L336 210 Q346 200 362 200 H424 V276 Z',
    'M448 208 H504 Q522 208 534 224 L562 276 H448 Z'
  ],
  suv: [
    'M280 276 L318 190 Q328 180 344 180 H422 V276 Z',
    'M446 186 H526 Q548 186 558 204 L584 276 H446 Z'
  ],
  mpv: [
    'M272 276 L304 178 Q312 168 334 168 H418 V276 Z',
    'M442 178 H556 Q580 178 588 200 L614 276 H442 Z'
  ],
  luxury: [
    'M294 276 L340 220 Q350 212 366 212 H426 V276 Z',
    'M450 220 H500 Q518 220 530 234 L556 276 H450 Z'
  ],
  ev: [
    'M286 276 L326 198 Q336 188 360 188 H424 V276 Z',
    'M448 198 H510 Q536 198 548 218 L576 276 H448 Z'
  ]
}

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

function carGroup(paint, body, { glass = '#101b26' } = {}) {
  const cabin = CABIN[body] || CABIN.sedan
  const [rear, front] = WINDOWS[body] || WINDOWS.sedan
  return `
  <g>
    <ellipse cx="400" cy="414" rx="292" ry="22" fill="#000" opacity=".22"/>
    <path d="${cabin}" fill="${paint}" stroke="#000" stroke-opacity=".12" stroke-width="2"/>
    <path d="${rear}" fill="${glass}"/>
    <path d="${front}" fill="${glass}"/>
    <rect x="426" y="${body === 'suv' || body === 'mpv' ? 176 : 198}" width="12" height="82" fill="${paint}" opacity=".9"/>
    <path d="M104 342 Q104 304 144 296 L206 286 H596 Q652 292 678 304 Q700 312 700 340 V354 Q700 372 678 372 H126 Q104 372 104 354 Z"
          fill="${paint}" stroke="#000" stroke-opacity=".14" stroke-width="2"/>
    <path d="M120 330 L680 330" stroke="#fff" stroke-opacity=".22" stroke-width="3"/>
    <path d="M430 292 V362" stroke="#000" stroke-opacity=".18" stroke-width="2"/>
    <path d="M510 292 V362" stroke="#000" stroke-opacity=".14" stroke-width="2"/>
    <rect x="392" y="306" width="30" height="7" rx="3.5" fill="#fff" opacity=".45"/>
    <rect x="472" y="306" width="30" height="7" rx="3.5" fill="#fff" opacity=".45"/>
    <rect x="646" y="310" width="40" height="15" rx="7.5" fill="#fff6cf"/>
    <rect x="110" y="312" width="30" height="13" rx="6.5" fill="#e5484d"/>
    <circle cx="214" cy="370" r="54" fill="#0b1017" opacity=".95"/>
    <circle cx="592" cy="370" r="54" fill="#0b1017" opacity=".95"/>
    <circle cx="214" cy="370" r="44" fill="#191d23" stroke="#3b434e" stroke-width="3"/>
    <circle cx="592" cy="370" r="44" fill="#191d23" stroke="#3b434e" stroke-width="3"/>
    <circle cx="214" cy="370" r="24" fill="#9aa4b0"/>
    <circle cx="592" cy="370" r="24" fill="#9aa4b0"/>
    <circle cx="214" cy="370" r="9" fill="#2b323b"/>
    <circle cx="592" cy="370" r="9" fill="#2b323b"/>
  </g>`
}

function carCover(model, index) {
  const cat = CATEGORY[model.cat] || CATEGORY.ECONOMY
  const brand = BRANDS.find((b) => b.id === model.brandId)
  const hue = 205 + ((index * 17) % 60)
  const dark = `hsl(${hue} 34% 18%)`
  const light = `hsl(${hue} 38% 30%)`
  return `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${dark}"/><stop offset="1" stop-color="${light}"/>
    </linearGradient>
    <linearGradient id="paint" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="${model.paint}" stop-opacity="1"/>
      <stop offset="1" stop-color="${model.paint}" stop-opacity=".72"/>
    </linearGradient>
    <linearGradient id="glass" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#0e1922"/><stop offset="1" stop-color="#28455c"/>
    </linearGradient>
    <radialGradient id="glow" cx=".5" cy=".5" r=".5">
      <stop offset="0" stop-color="#fff" stop-opacity=".18"/><stop offset="1" stop-color="#fff" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <circle cx="700" cy="80" r="170" fill="url(#glow)"/>
  <circle cx="84" cy="446" r="120" fill="#fff" opacity=".05"/>
  <g opacity=".08" stroke="#fff">
    <path d="M0 460 L800 300"/><path d="M0 500 L800 340"/><path d="M0 420 L800 260"/>
  </g>

  <text x="36" y="186" fill="#fff" fill-opacity=".07" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="180" font-weight="700">${esc(brand?.initial || '')}</text>

  ${carGroup('url(#paint)', model.body)}

  <text x="46" y="472" fill="#fff" fill-opacity=".52" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="13">${esc(brand?.name || '')} · ${cat.label}</text>
  <text x="754" y="472" fill="#fff" fill-opacity=".34" text-anchor="end" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="12">微租车 · 本地演示图</text>
</svg>`
}

function brandSvg(brand) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#2c5580"/><stop offset="1" stop-color="#1f3a5f"/>
    </linearGradient>
  </defs>
  <rect width="120" height="120" rx="24" fill="url(#g)"/>
  <circle cx="96" cy="22" r="34" fill="#fff" opacity=".06"/>
  <text x="60" y="63" text-anchor="middle" fill="#fff" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="46" font-weight="700">${esc(brand.initial)}</text>
  <text x="60" y="94" text-anchor="middle" fill="#fff" fill-opacity=".72" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="14">${esc(brand.name)}</text>
</svg>`
}

function interiorSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#1b2c3d"/><stop offset="1" stop-color="#3b5670"/>
    </linearGradient>
    <linearGradient id="dash" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#222d3a"/><stop offset="1" stop-color="#111820"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="#0b1118"/>
  <path d="M0 0 H800 V210 H0 Z" fill="url(#sky)"/>
  <path d="M0 210 H800 L760 400 H40 Z" fill="url(#dash)"/>
  <path d="M0 210 H800 L780 250 H20 Z" fill="#0d151d" opacity=".8"/>
  <circle cx="252" cy="338" r="112" fill="none" stroke="#2c3746" stroke-width="26"/>
  <circle cx="252" cy="338" r="58" fill="#1a2431" stroke="#3a4758" stroke-width="4"/>
  <circle cx="252" cy="338" r="16" fill="#4a5a6d"/>
  <path d="M252 282 V338 M196 338 H308" stroke="#3a4758" stroke-width="14" stroke-linecap="round"/>
  <rect x="412" y="250" width="244" height="132" rx="14" fill="#0a1119" stroke="#3a4a5d" stroke-width="3"/>
  <rect x="424" y="262" width="220" height="26" rx="6" fill="#14212e"/>
  <text x="440" y="281" fill="#7fb2d8" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="14">微租车 · 智能座舱</text>
  <text x="440" y="330" fill="#e8eef5" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="26" font-weight="700">信用风控</text>
  <text x="440" y="360" fill="#8fa3b8" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="14">动态押金 · 押金低至 8 折</text>
  <rect x="640" y="250" width="80" height="132" rx="10" fill="#141d27" stroke="#2f3c4c"/>
  <circle cx="680" cy="290" r="20" fill="none" stroke="#4b6076" stroke-width="3"/>
  <path d="M680 290 L680 274" stroke="#7fb2d8" stroke-width="3"/>
  <rect x="660" y="330" width="40" height="8" rx="4" fill="#2b3a4a"/>
  <rect x="660" y="348" width="40" height="8" rx="4" fill="#2b3a4a"/>
  <text x="400" y="470" fill="#fff" fill-opacity=".35" text-anchor="middle" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="13">内饰示意 · 本地演示图</text>
</svg>`
}

function dashboardSvg() {
  const ticks = Array.from({ length: 24 }, (_, i) => {
    const a = (Math.PI * 2 * i) / 24 - Math.PI / 2
    const x1 = 270 + Math.cos(a) * 78
    const y1 = 250 + Math.sin(a) * 78
    const x2 = 270 + Math.cos(a) * 90
    const y2 = 250 + Math.sin(a) * 90
    return `<line x1="${x1.toFixed(1)}" y1="${y1.toFixed(1)}" x2="${x2.toFixed(1)}" y2="${y2.toFixed(1)}" stroke="#5b7288" stroke-width="2"/>`
  }).join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <radialGradient id="bg" cx=".5" cy=".4" r=".8">
      <stop offset="0" stop-color="#1b2734"/><stop offset="1" stop-color="#080d12"/>
    </radialGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <circle cx="270" cy="250" r="132" fill="none" stroke="#263443" stroke-width="10"/>
  <circle cx="270" cy="250" r="102" fill="#0b131b" stroke="#314152" stroke-width="3"/>
  ${ticks}
  <path d="M270 250 L320 190" stroke="#67c23a" stroke-width="5" stroke-linecap="round"/>
  <circle cx="270" cy="250" r="8" fill="#67c23a"/>
  <text x="270" y="300" text-anchor="middle" fill="#8fa3b8" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="14">续航里程</text>
  <circle cx="560" cy="250" r="132" fill="none" stroke="#263443" stroke-width="10"/>
  <circle cx="560" cy="250" r="102" fill="#0b131b" stroke="#314152" stroke-width="3"/>
  <text x="560" y="240" text-anchor="middle" fill="#e8eef5" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="64" font-weight="700">88</text>
  <text x="560" y="278" text-anchor="middle" fill="#8fa3b8" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="15">km/h</text>
  <rect x="250" y="392" width="300" height="44" rx="22" fill="#111b25" stroke="#2c3b4c"/>
  <text x="400" y="421" text-anchor="middle" fill="#67c23a" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="16">信用优秀 · 押金 8 折</text>
  <text x="400" y="476" text-anchor="middle" fill="#fff" fill-opacity=".3" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="12">仪表示意 · 本地演示图</text>
</svg>`
}

function rearSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#232f3d"/><stop offset="1" stop-color="#3f5367"/>
    </linearGradient>
    <linearGradient id="body" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#d7dce2"/><stop offset="1" stop-color="#9aa4b0"/>
    </linearGradient>
  </defs>
  <rect width="800" height="500" fill="url(#bg)"/>
  <ellipse cx="400" cy="424" rx="250" ry="20" fill="#000" opacity=".3"/>
  <path d="M200 300 L230 220 Q240 196 268 194 H532 Q560 196 570 220 L600 300 V370 Q600 392 578 392 H222 Q200 392 200 370 Z" fill="url(#body)"/>
  <path d="M252 226 H548 Q562 226 566 240 L578 286 H222 L234 240 Q238 226 252 226 Z" fill="#16222e"/>
  <rect x="206" y="300" width="60" height="26" rx="8" fill="#e5484d"/>
  <rect x="534" y="300" width="60" height="26" rx="8" fill="#e5484d"/>
  <rect x="330" y="338" width="140" height="36" rx="6" fill="#1c2735"/>
  <text x="400" y="363" text-anchor="middle" fill="#dfe7ef" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="18" letter-spacing="2">京A·8888</text>
  <path d="M200 372 H600" stroke="#000" stroke-opacity=".18" stroke-width="2"/>
  <circle cx="270" cy="392" r="34" fill="#111820"/>
  <circle cx="530" cy="392" r="34" fill="#111820"/>
  <text x="400" y="470" text-anchor="middle" fill="#fff" fill-opacity=".34" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="13">车辆尾部 · 本地演示图</text>
</svg>`
}

function bannerSvg(index) {
  const themes = [
    { title: '信用越好 押金越低', sub: '优秀档押金 8 折 · 良好档 9 折 · 全程可查信用明细', from: '#1f3a5f', to: '#276b5c', tag: '信用风控' },
    { title: '新用户首单立减 50', sub: '注册即得 100 信用分，在线选车 · 模拟支付 · 网点取还', from: '#243b55', to: '#3f5f8a', tag: '新人专享' },
    { title: '新能源专区 低至 219/天', sub: '纯电与混动车型集中展示，支持异地还车', from: '#1d4a45', to: '#2f7d6b', tag: '新能源' }
  ]
  const t = themes[index - 1]
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="400" viewBox="0 0 1200 400">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${t.from}"/><stop offset="1" stop-color="${t.to}"/>
    </linearGradient>
    <radialGradient id="glow" cx=".5" cy=".5" r=".5">
      <stop offset="0" stop-color="#fff" stop-opacity=".16"/><stop offset="1" stop-color="#fff" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="1200" height="400" fill="url(#bg)"/>
  <circle cx="1020" cy="60" r="220" fill="url(#glow)"/>
  <circle cx="120" cy="380" r="150" fill="#fff" opacity=".04"/>
  <g opacity=".08" stroke="#fff"><path d="M0 400 L1200 220"/><path d="M0 360 L1200 180"/></g>
  <rect x="72" y="60" width="112" height="32" rx="16" fill="#fff" fill-opacity=".16"/>
  <text x="128" y="82" text-anchor="middle" fill="#fff" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="14">${t.tag}</text>
  <text x="72" y="168" fill="#fff" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="52" font-weight="700">${t.title}</text>
  <text x="72" y="218" fill="#fff" fill-opacity=".72" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="18">${t.sub}</text>
  <rect x="72" y="258" width="150" height="44" rx="22" fill="#fff"/>
  <text x="147" y="286" text-anchor="middle" fill="${t.from}" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="15" font-weight="600">立即查看</text>
  <g transform="translate(650,30) scale(0.66)">${carGroup('#e8edf3', 'sedan', { glass: '#1a2a3a' })}</g>
  <text x="1128" y="368" text-anchor="end" fill="#fff" fill-opacity=".4" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="13">微租车 · 本地演示图</text>
</svg>`
}

function placeholderSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <rect width="800" height="500" fill="#eef2f6"/>
  <g stroke="#c3ccd6" stroke-width="6" fill="none" stroke-linecap="round">
    <rect x="290" y="180" width="220" height="150" rx="12"/>
    <circle cx="350" cy="235" r="18"/>
    <path d="M300 315 L370 250 L420 295 L450 270 L500 315"/>
  </g>
  <text x="400" y="382" text-anchor="middle" fill="#8b98a8" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="18">图片加载失败</text>
  <text x="400" y="410" text-anchor="middle" fill="#a9b4c0" font-family="'Microsoft YaHei','PingFang SC',sans-serif" font-size="13">请检查 /public/images 下的本地静态资源</text>
</svg>`
}

function avatarSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
  <rect width="120" height="120" rx="60" fill="#dfe7ef"/>
  <circle cx="60" cy="46" r="22" fill="#9fb0c2"/>
  <path d="M18 112 Q22 74 60 74 Q98 74 102 112 Z" fill="#9fb0c2"/>
</svg>`
}

function write(rel, content) {
  const file = path.join(OUT, rel)
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, content, 'utf8')
}

BRANDS.forEach((b) => write(`brands/brand-${b.id}.svg`, brandSvg(b)))
MODELS.forEach((m, i) => write(`cars/car-${m.id}.svg`, carCover(m, i)))
write('cars/gallery/interior.svg', interiorSvg())
write('cars/gallery/dashboard.svg', dashboardSvg())
write('cars/gallery/rear.svg', rearSvg())
;[1, 2, 3].forEach((i) => write(`banners/banner-${i}.svg`, bannerSvg(i)))
write('placeholder.svg', placeholderSvg())
write('avatar/default.svg', avatarSvg())

console.log(`已生成 ${BRANDS.length} 个品牌、${MODELS.length} 张车型图、3 张通用图、3 张轮播图、占位图与头像`)
