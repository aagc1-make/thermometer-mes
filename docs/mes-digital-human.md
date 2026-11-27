# MES 智能助手数字人

在 Windows 的 MES 智能助手页头悬停或聚焦米粒的头像，可看到「呼出米粒」。点击后整个左侧导航区域显示米粒和城市夜景。右侧保留当前会话，在页头下方提供朗读、停止朗读和自动朗读。点击「收起米粒」可还原左侧导航。

## 准备本机模型与渲染库

在 MES 仓库根目录运行以下 PowerShell 命令。若解压目录不同，用 `-Source` 指定包含 `avatars`、`backgrounds` 和 `live2d-models` 的目录。

```powershell
.\scripts\prepare-mes-avatar.ps1
```

脚本将素材复制到 `ktg-mes-ui/public/mes-avatar-assets/`，并下载缺少的渲染运行库。如果 Cubism Core 无法在线下载，可从 [Cubism SDK for Web 官方下载页](https://www.live2d.com/en/sdk/download/web/) 取 Web SDK，在解压后的 `Core` 目录找到 `live2dcubismcore.min.js`，传入本机文件路径重跑。

```powershell
.\scripts\prepare-mes-avatar.ps1 -CubismCorePath 'C:\SDK\CubismWeb\Core\live2dcubismcore.min.js'
```

离线准备时，可把下面的文件放进 `ktg-mes-ui/public/mes-avatar-assets/vendor/`，然后用 `-AssetsOnly` 只复制素材。浏览器从本地加载模型和这三个脚本。

| 本地文件 | 来源 |
| --- | --- |
| `live2dcubismcore.min.js` | [Live2D Cubism SDK for Web](https://www.live2d.com/en/sdk/download/web/) 的 `Core` 目录 |
| `pixi.min.js` | [PixiJS 7.4.3](https://cdn.jsdelivr.net/npm/pixi.js@7.4.3/dist/pixi.min.js) |
| `cubism4.min.js` | [pixi-live2d-display-lipsyncpatch 0.5.0-ls-8](https://cdn.jsdelivr.net/npm/pixi-live2d-display-lipsyncpatch@0.5.0-ls-8/dist/cubism4.min.js) |

模型、头像和夜景来自本机的 Open-LLM-VTuber 源目录。Mao 的作者和使用条件见源目录的 `live2d-models/mao_pro/ReadMe.txt` 及 [Live2D 样例页](https://www.live2d.com/en/learn/sample/niziiro-mao/)。运行素材和运行库位于 Git 忽略目录。

## 启动语音服务

使用 Python 3.11。依赖装在仓库内的 `.venv-avatar\lib`（已被 Git 忽略），不污染全局 Python。首次安装：

```powershell
$mesAvatarPython = "$env:LOCALAPPDATA\Programs\Python\Python311\python.exe"
& $mesAvatarPython -m pip install --target .\.venv-avatar\lib -r .\scripts\mes-avatar-speech\requirements.txt
```

之后双击 `scripts\mes-avatar-speech\start-voice.bat` 启动（它设置 `PYTHONPATH` 和默认音色）。保持语音服务窗口运行，再用项目原有方式启动 MES 后端和前端。若前端已开着，重启前端使语音代理生效。

语音服务监听 `127.0.0.1:12394`。前端通过同源 `POST /mes-avatar/speech` 取回音频，开发环境由 Vue CLI 代理。若改端口，为 Python 服务设置 `MES_AVATAR_PORT`，同时在启动前端的终端把 `MES_AVATAR_SERVICE_URL` 改成对应的 `http://127.0.0.1:<端口>`。

服务复用 [Edge TTS](https://github.com/rany2/edge-tts) 合成引擎。默认音色为 `zh-CN-XiaoyiNeural`（晓伊，活泼少女感），语速 `-5%`、音调 `+8Hz`，可用 `MES_AVATAR_VOICE`、`MES_AVATAR_RATE`、`MES_AVATAR_PITCH` 覆盖。温柔姐姐系可改用 `zh-CN-XiaoxiaoNeural` 配 `-8%` / `+2Hz`；另有 `zh-CN-liaoning-XiaobeiNeural`（东北话）、`zh-CN-shaanxi-XiaoniNeural`（陕西话）。

合成需要联网，发送给语音引擎的内容是待朗读短句。页面从同一段播放音频的音量驱动米粒的嘴形。并将 `0.65` 等小数转换成完整读法。

**边合成边播**：Azure 分支返回的是分块音频流，前端用 `MediaSource` 边收边播，首包到就开口，不再等整段音频生成完。一段 280 字的回答整段合成要 6 秒以上，流式后 0.3~0.5 秒就能出声。配套两点：

- 服务用一条 keep-alive 连接复用 Azure 的 TLS 会话，并每 20 秒合成一个字做心跳；否则每次握手要多付 0.3~1 秒。
- 浏览器不支持 `MediaSource.isTypeSupported('audio/mpeg')`（如 Firefox）时自动退回整段解码，功能不受影响。

实测（`curl -N` 首字节）：冷连接 1.5~1.9 秒，热连接 0.27~0.38 秒。

两点已知限制：

- 服务启动时会清掉 `http_proxy` 等环境变量。edge-tts 与 `speech.platform.bing.com` 的 WebSocket 握手经系统代理会返回 403。
- Edge 免费通道是微软「大声朗读」的非公开接口，短时间大量请求会被限流成 403（实测连续十几次后触发）。2026-11 实测该通道在本机网络已持续 403，因此数字人改用 Azure。

### 使用 Azure 语音（推荐，音色与 edge 完全相同）

免费通道不可用时，把同一批神经音色换到 Azure 正式接口。已有 `Azure AI Foundry`（AIServices）资源同样可以调用 Speech，不必单独新建。

1. 在 `scripts\mes-avatar-speech\azure-key.txt` 写入两行（该文件已被 Git 忽略）：

   ```ini
   region=westus3
   key=你的订阅密钥
   ```

2. 先用 `check-azure.bat <key> <region>` 单独验证密钥，成功时会在当前目录生成 `azure-test.mp3`。

3. 双击 `scripts\mes-avatar-speech\start-voice-azure.bat` 启动服务；它读取上面的文件并以 `azure` 引擎运行同一个 `POST /mes-avatar/speech`。

Azure 分支用 SSML 调用 `https://{region}.tts.speech.microsoft.com/cognitiveservices/v1`，输出 24 kHz / 48 kbps mp3，`voice`、`rate`、`pitch` 与 edge 分支共用同一组环境变量。`MES_AVATAR_AZURE_ENDPOINT` 可整体覆盖 URL（例如 Azure 中国区 `https://{region}.tts.speech.azure.cn/cognitiveservices/v1`）。

当渲染库或 WebGL 不可用时，左侧显示静态形象，右侧提供「重试画面」。语音服务不可用时，右侧仍可查看问答、库存明细和引用，并提供「重试朗读」。

### 使用 Windows 本机中文语音

语音服务也支持 Windows 已安装的 `zh-CN` 音色。离线模式只使用 Python 标准库和系统语音，可在仓库根目录执行：

```powershell
$env:MES_AVATAR_ENGINE = 'windows'
$mesAvatarPython = "$env:LOCALAPPDATA\Programs\Python\Python311\python.exe"
& $mesAvatarPython .\scripts\mes-avatar-speech\service.py
```

离线模式沿用相同的端口和接口，返回 WAV。恢复 Edge TTS 时，在启动语音服务前将 `MES_AVATAR_ENGINE` 设为 `edge`。
