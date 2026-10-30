/**
 * ECharts 按需引入
 *
 * 只注册用到的图表与组件，打包体积比全量引入小一半以上。
 * 后续要加图表（如雷达图、热力图）在这里补一行即可。
 */
import * as echarts from 'echarts/core'
import { BarChart, HeatmapChart, LineChart, PieChart, ScatterChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  HeatmapChart,
  LineChart,
  PieChart,
  ScatterChart,
  GridComponent,
  LegendComponent,
  // 预测图的"今天"分界线用的是 markLine
  MarkLineComponent,
  TitleComponent,
  TooltipComponent,
  // 热力图的颜色映射
  VisualMapComponent,
  CanvasRenderer
])

export default echarts
