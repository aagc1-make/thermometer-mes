import provider from "@/views/print/printtemplate/ProviderData";
import {PRINT_DICT} from "../utils/print";
import webSite from "@/config/website";

// 性能：vue-plugin-hiprint 体积较大且只在打印相关页面用到，
// 改为挂载时动态引入，避免进入首屏 chunk。引入后缓存 Promise，多次挂载只加载一次。
let hiprintPromise = null;
function loadHiprint() {
  if (!hiprintPromise) {
    hiprintPromise = import(/* webpackChunkName: "chunk-hiprint" */ 'vue-plugin-hiprint')
      .then(mod => mod.hiprint || mod.default || mod);
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
      const hiprint = await loadHiprint();
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
