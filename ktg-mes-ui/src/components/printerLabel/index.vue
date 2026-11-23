<template>
    <el-button
            size="mini"
            type="text"
            icon="el-icon-printer"
            @click="handleHiPrint()"
          >{{labelText}}</el-button>
</template>
<script>
import { printBarcodeLabel } from "@/api/print/print";
import {PRINT_DICT} from "@/utils/print";
import {getMdItem} from "../../api/mes/md/mdItem";
import {getByTemplateType, getTemplate} from "@/api/print/template";
import {getBatchconfig} from "@/api/mes/md/batchconfig";
import {getItemrecptline} from "../../api/mes/wm/itemrecptline";
import {getDefaultPrinter} from "../../api/print/printerconfig";
import {Message} from "element-ui";
import webSite from "@/config/website";

let hiprintTemplate
let hiprintLib = null
let providerLib = null
function loadPrintLibs() {
  return Promise.all([
    import(/* webpackChunkName: "chunk-hiprint" */ 'vue-plugin-hiprint'),
    import(/* webpackChunkName: "chunk-hiprint" */ '@/views/print/printtemplate/ProviderData')
  ]).then(([mod, providerModule]) => ({
    hiprint: mod.hiprint || mod.default || mod,
    provider: providerModule.default || providerModule
  }))
}
export default {
    name: 'printLabel',
    data(){
        return {

        }
    },
  mounted() {
    this.init()
  },
    props:{
        businessId: null,
        businessCode: null,
        businessType: null,
        labelText: {
            type: String,
            default: '打印'
        },
        printerCode: null
    },
    methods:{
        handlePrint(){

            var theCode;
            if(this.printerCode ==null){
                //是否有配置默认的打印机
                const key = 'defaultPrinter';
                let printerJson = localStorage.getItem(key);
                if(printerJson !=null){
                    const printer = JSON.parse(printerJson);
                    theCode = printer.printerCode;
                }else{
                    this.$modal.msgError("请在打印管理-打印机配置功能中指定默认打印机！");
                }
            }else{
                theCode = this.printerCode;
            }

            const param = { businessId: this.businessId, businessCode: this.businessCode, businessType: this.businessType, printerCode: theCode };
            printBarcodeLabel(param).then(response => {
              this.$modal.msgSuccess("打印成功");
            });
        },
      // 初始化打印插件
      async init() {
        if (!webSite.print_transfer_url) {
          console.warn('[print] 未配置 VUE_APP_PRINT_TRANSFER_URL，标签打印暂不可用');
          return;
        }
        try {
          const libs = await loadPrintLibs();
          hiprintLib = libs.hiprint;
          providerLib = libs.provider;
          hiprintLib.init({
            providers: [providerLib]
          });
          hiprintTemplate = new hiprintLib.PrintTemplate()
        } catch (e) {
          console.warn('[print] 打印插件加载失败：' + e.message);
        }
      },
      // 标签打印
      async handleHiPrint() {
        if (!hiprintTemplate) {
          this.$modal.msgWarning('打印服务未配置，标签打印暂不可用');
          return;
        }
        // 根据 businessType 类型查询具体打印数据
        let printData
        switch (this.businessType) {
          case 'ITEM':
            // 物料/产品
            await getMdItem(this.businessId).then(res => {
              printData = res.data
            })
            break;
          case 'ITEM_BATCH':
            // 物料-批次
            await getItemrecptline(this.businessId).then(res => {
              printData = res.data
            })
            break;
          default:
            break;
        }
        // 根据 templateType 查询打印模板
        getByTemplateType(this.businessType).then(res => {
          const data = res.data
          if (hiprintTemplate) {
            try {
              // 将打印模板封装
              hiprintTemplate.update(JSON.parse(data.templateJson));
              if (window.hiwebSocket.opened) {
                // 根据配置好的客户端查询相应的打印机
                const client = JSON.parse(localStorage.getItem("defaultClient"))
                getDefaultPrinter(client.clientId).then(res => {
                  if (res.code == 200) {
                    hiprintTemplate.print2(printData, {printer: res.data.printerName, title: 'hiprint测试打印'});
                    this.$message.success("打印成功")
                  }
                })
                return
              }
              this.$message.error('客户端未连接,无法直接打印')
            } catch (e) {
              this.$message.error(`更新失败: ${e}`)
            }
          }
        })
      },
    }
}


</script>
