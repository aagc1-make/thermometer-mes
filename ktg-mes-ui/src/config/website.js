

/**
 * 全局配置文件
 */
export default {
  reportUrl: 'http://localhost:8080/ureport',
  // 可选打印服务。未配置时打印页面不建立 socket 连接，避免反复重试 17521/17522。
  print_transfer_url: process.env.VUE_APP_PRINT_TRANSFER_URL || '',
  print_transfer_token: process.env.VUE_APP_PRINT_TRANSFER_TOKEN || '',
}
