# 首页「推荐出行方案」配图

五张图对应五个用车场景，全部为**实拍摄影**，非 AI 生成。
布局是「一张大图 + 四张小图」的宫格，大图（自驾旅行）文案压在左上角，
因为红色房车在画面右下，压底部会把车挡住。

| 文件 | 尺寸 | 内容 | 对应场景 | 跳转 |
| --- | --- | --- | --- | --- |
| `travel-drive.jpg` | 1100×825 | 冰岛荒原公路上的红色房车，远处雪山 | 自驾旅行 | `/vehicles` |
| `travel-commute.jpg` | 800×534 | 清晨的城市街道与车流 | 日常通勤 | `/vehicles?category=ECONOMY` |
| `travel-business.jpg` | 800×534 | 西装男士坐在车内，窗外是城市 | 商务接待 | `/vehicles?category=LUXURY` |
| `travel-family.jpg` | 800×534 | 父母带着孩子坐在车里 | 亲子出游 | `/vehicles?category=SUV` |
| `travel-visit.jpg` | 800×534 | 往 SUV 后备箱放行李箱 | 探亲访友 | `/vehicles?category=COMFORT` |

## 来源与许可

图片来自 [Pexels](https://www.pexels.com)，按 [Pexels License](https://www.pexels.com/license/) 可免费商用、无需署名。

| 本目录文件 | Pexels 照片 |
| --- | --- |
| `travel-drive.jpg` | [#37066071](https://www.pexels.com/photo/scenic-drive-through-the-icelandic-highlands-37066071) |
| `travel-commute.jpg` | [#10678623](https://www.pexels.com/photo/cars-parked-on-the-side-of-the-road-in-the-city-10678623/) |
| `travel-business.jpg` | [#9520553](https://www.pexels.com/photo/man-in-black-suit-jacket-9520553) |
| `travel-family.jpg` | [#6182096](https://www.pexels.com/photo/parents-inside-a-car-with-their-child-6182096) |
| `travel-visit.jpg` | [#12675294](https://www.pexels.com/photo/beautiful-woman-holding-her-red-luggage-12675294/) |

处理方式：sharp 裁切 + `mozjpeg` 压缩（大图质量 82、小图 80），不做调色与合成。

## 换图怎么做

1. 按现有尺寸替换同名文件即可，CSS 不用动（`object-fit: cover` 自动裁切）；
2. 大图文字在左上角、小图文字在左下角，选图时注意这两个区域别太亮、别压住主体；
3. 文案在 `frontend/src/views/portal/Home.vue` 的 `travelPlans` 数组里，改价格/车型时
   记得和 `sql/02-seed.sql` 的 `vehicle_model` 对齐。
