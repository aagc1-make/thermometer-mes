# 实验二 主干必要单元测试

基线main@909b875。按用户收敛要求，保留现有主干测试，只新增8条质量数量基本路径，沿用已有Mockito自动拣货测试，不扩展其他模块或设置全项目覆盖门槛。

本机JDK8、Maven3.6.3；JUnit4.13.2/Jupiter5.7.2、Mockito3.9.0；JaCoCo0.8.12。IDEA2026.3的JUnit插件和运行库已核验。IDE自身JBR与项目JDK分开配置。

运行（项目根backend/ktg-mes）：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk1.8.0_261'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
& E:\dev\apache-maven-3.6.3\bin\mvn.cmd '-Dmaven.repo.local=E:\D\.m2\repo' -B -Plab2-coverage -pl ktg-admin -am verify
```

原37个测试类163项测试通过；新增后38类171项测试通过，失败/错误/跳过均0。其中业务模块165项、system模块6项。该命令运行Maven单元测试，不代表前端、接口和性能测试已全覆盖。

质量数量方法assertQualityQuantities：行7/7始终100%，分支从8/14提升到14/14，复杂度覆盖从2/8提升到8/8。P1—P7依次让一个短路条件首次为真，P8全部为假且正常返回；详细输入见QualityQuantityBasisPathTest。

全MES模块行覆盖3663/26647（13.75%），分支由894/7210（12.40%）升至900/7210（12.48%）。FormLinkagePolicy类行37/43（86.05%），分支33/52升至39/52（75%）。全模块覆盖率低如实保留，不为报告数字扩大测试。

Mockito示例直接沿用IssueAutoPickServiceTest的8项原测试，展示FIFO拆批、短缺不写入和重复拣货；ArgumentCaptor核对保存的批次/数量，verify核对调用行为，不连接业务数据库。

证据：evidence/lab2/baseline、after中有Maven日志、SurefireXML、JaCoCoHTML/XML和summary.json。after/surefire-report.html由maven-surefire-report-plugin2.22.2:report-only生成，静态CSS和图片使用Maven官方默认皮肤。三张实际浏览器截图用于原Word第二章。

原报告仍为E:/JavaCode/软件测试与与质量保证个人报告模版.docx。业务源码未修改，实验一遗留工单数量缺陷未在本轮修复。

参考：[JaCoCo计数器](https://www.jacoco.org/jacoco/trunk/doc/counters.html)、[JaCoCo Maven插件](https://www.jacoco.org/jacoco/trunk/doc/maven.html)。JaCoCo行覆盖是至少一条映射指令执行，不能笼统当作所有语句覆盖；基本路径由用例和控制流分析说明，复杂度计数作为佐证。
