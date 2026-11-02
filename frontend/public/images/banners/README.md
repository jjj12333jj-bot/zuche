# 首页轮播图

首页第一屏是前端固定的「信用越好，押金越低」说明（蓝调旅行实景图打底 + 蓝色遮罩），
第二、三屏读取 `banner` 表，图片就在本目录。全部为**实拍摄影**，非 AI 生成。

| 文件 | 尺寸 | 内容 | 用在 |
| --- | --- | --- | --- |
| `banner-credit.jpg` | 1920×560 | 大西洋海滨公路，白色轿车行驶在弧形桥上 | 第一屏「信用越好，押金越低」背景 |
| `banner-credit-sm.jpg` | 900×1100 | 同上，竖版重裁 | 第一屏窄屏（≤640px） |
| `banner-1.jpg` | 1920×560 | 白色 SUV 停在崖顶，远处是海 | 第二屏「山海之间，一路向南」 |
| `banner-1-sm.jpg` | 900×1100 | 同上，竖版重裁 | 窄屏（≤640px） |
| `banner-2.jpg` | 1920×560 | 海岸悬崖公路上行驶的三辆车 | 第三屏「周末短途，说走就走」 |
| `banner-2-sm.jpg` | 900×1100 | 同上，竖版重裁 | 窄屏（≤640px） |

## 来源与许可

图片来自 [Pexels](https://www.pexels.com)，按 [Pexels License](https://www.pexels.com/license/) 可免费商用、无需署名。

| 本目录文件 | Pexels 照片 |
| --- | --- |
| `banner-credit*.jpg` | [#811034](https://www.pexels.com/photo/white-car-on-the-road-811034/) |
| `banner-1*.jpg` | [#27497631](https://www.pexels.com/photo/a-white-suv-parked-on-the-side-of-a-cliff-overlooking-the-ocean-27497631/) |
| `banner-2*.jpg` | [#26241525](https://www.pexels.com/photo/cars-on-a-road-by-the-shore-26241525/) |

处理方式：sharp 裁切 + `mozjpeg` 压缩（桌面质量 82、竖版 78），不做调色与合成。

## 第一屏的遮罩为什么是蓝色

第一屏文字和右侧「信用等级与押金系数」面板都是白字，直接压在照片上会花。
所以 `.slide-credit::before` 铺了一层从左上到右下由深到浅的蓝色遮罩
（`rgba(6,30,60,.95)` → `rgba(10,52,102,.34)`），既保证左侧正文对比度够，
又让右侧桥与车露出来；面板本身改成深蓝半透明 + 毛玻璃，压在照片任何位置都能读。

## 换图怎么做

1. 把新图放到本目录，桌面版建议 1920×560 左右（宽高比约 3.4:1）；
2. 登录系统管理员 → 平台管理 → 轮播图管理 → 编辑，改「图片地址」为 `/images/banners/xxx.jpg`；
3. 同时把标题、副标题改成和新图匹配的文案。

窄屏用的 `-sm.jpg` 是按文件名约定推导的：`/images/banners/banner-N.jpg` 在 ≤640px 时自动换成
`banner-N-sm.jpg`。换文件名时记得同时准备一张 `xxx-sm.jpg`，否则窄屏上会只显示纯蓝底色。

## 为什么桌面和手机用两套裁切

横图在手机竖屏上被 `cover` 放大后只剩中间一条，车容易整个被裁掉。所以窄屏单独裁一张竖版，
文字也从居中改为压在底部、遮罩改成从下往上压暗。
