import type { UserConfigExport } from '@tarojs/cli';
export default {
  mini: {
    // 生产包不生成 source map，避免占用 2MB 主包体积
    enableSourceMap: false,
  },
  h5: {},
} satisfies UserConfigExport<'webpack5'>;
