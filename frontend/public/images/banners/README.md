# 首页轮播图

首页第一屏是前端固定的「信用越好，押金越低」说明（纯蓝底），第二、三屏读取 `banner` 表，
图片就在本目录。全部为**实拍摄影**，非 AI 生成。

| 文件 | 尺寸 | 内容 | 用在 |
| --- | --- | --- | --- |
| `banner-1.jpg` | 1920×560 | 白色 SUV 停在崖顶，远处是海 | 第二屏「山海之间，一路向南」 |
| `banner-1-sm.jpg` | 900×1100 | 同上，竖版重裁 | 窄屏（≤640px） |
| `banner-2.jpg` | 1920×560 | 海岸悬崖公路上行驶的三辆车 | 第三屏「周末短途，说走就走」 |
| `banner-2-sm.jpg` | 900×1100 | 同上，竖版重裁 | 窄屏（≤640px） |

## 来源与许可

图片来自 [Pexels](https://www.pexels.com)，按 [Pexels License](https://www.pexels.com/license/) 可免费商用、无需署名。

| 本目录文件 | Pexels 照片 |
| --- | --- |
| `banner-1*.jpg` | [#27497631](https://www.pexels.com/photo/a-white-suv-parked-on-the-side-of-a-cliff-overlooking-the-ocean-27497631/) |
| `banner-2*.jpg` | [#26241525](https://www.pexels.com/photo/cars-on-a-road-by-the-shore-26241525/) |

处理方式：sharp 裁切 + `mozjpeg` 压缩（桌面质量 82、竖版 78），不做调色与合成。

## 换图怎么做

1. 把新图放到本目录，桌面版建议 1920×560 左右（宽高比约 3.4:1）；
2. 登录系统管理员 → 平台管理 → 轮播图管理 → 编辑，改「图片地址」为 `/images/banners/xxx.jpg`；
3. 同时把标题、副标题改成和新图匹配的文案。

窄屏用的 `-sm.jpg` 是按文件名约定推导的：`/images/banners/banner-N.jpg` 在 ≤640px 时自动换成
`banner-N-sm.jpg`。换文件名时记得同时准备一张 `xxx-sm.jpg`，否则窄屏上会只显示纯蓝底色。

## 为什么桌面和手机用两套裁切

横图在手机竖屏上被 `cover` 放大后只剩中间一条，车容易整个被裁掉。所以窄屏单独裁一张竖版，
文字也从居中改为压在底部、遮罩改成从下往上压暗。
