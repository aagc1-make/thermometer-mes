import {PRINT_DICT} from "../utils/print";
import webSite from "@/config/website";

// 性能：vue-plugin-hiprint 体积较大且只在打印相关页面用到，
// 改为使用时动态引入，避免进入首屏 chunk，也避免未配置打印服务时建立 socket。
let hiprintPromise = null;
let printWarned = false;
function loadHiprint() {
  if (!hiprintPromise) {
    hiprintPromise = Promise.all([
      import(/* webpackChunkName: "chunk-hiprint" */ 'vue-plugin-hiprint'),
      import(/* webpackChunkName: "chunk-hiprint" */ '@/views/print/printtemplate/ProviderData')
    ]).then(([mod, providerModule]) => ({
      hiprint: mod.hiprint || mod.default || mod,
      provider: providerModule.default || providerModule
    }));
  }
  return hiprintPromise;
}

/**
 * hiprint初始化（异步）
 */
export const hiprintMixin = {
  data() {
    return {
      hiprintTemplate: null,
      hiprintThis: null
    };
  },
  mounted() {
    this.initHiprint();
  },
  methods: {
    async initHiprint() {
      if (!webSite.print_transfer_url) {
        if (!printWarned) {
          printWarned = true;
          console.warn('[print] 未配置 VUE_APP_PRINT_TRANSFER_URL，已跳过打印服务连接；标签打印暂不可用');
        }
        return;
      }
      const { hiprint, provider } = await loadHiprint();
      hiprint.init({
        providers: [provider],
        host: webSite.print_transfer_url,
        token: webSite.print_transfer_token
      });
      this.hiprintThis = hiprint;
      this.hiprintTemplate = new hiprint.PrintTemplate();
      // 供页面在模板加载完成后再执行打印逻辑
      this.$emit && this.$emit('hiprint-ready', hiprint);
    }
  }
};
