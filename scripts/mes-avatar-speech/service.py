import asyncio
import json
import logging
import os
import re
import subprocess
import time
from pathlib import Path
from socketserver import ThreadingMixIn
from wsgiref.simple_server import (
    WSGIRequestHandler,
    WSGIServer,
    make_server,
)

logger = logging.getLogger("uvicorn.error")
speech_path = "/mes-avatar/speech"
max_text_length = 320
engine = os.environ.get("MES_AVATAR_ENGINE", "edge").strip().lower()
if engine not in {"edge", "windows", "azure"}:
    raise ValueError("MES_AVATAR_ENGINE must be 'edge', 'azure' or 'windows'")
voice = os.environ.get("MES_AVATAR_VOICE", "zh-CN-XiaoyiNeural")
rate = os.environ.get("MES_AVATAR_RATE", "-5%")
pitch = os.environ.get("MES_AVATAR_PITCH", "+8Hz")
if not re.fullmatch(r"[+-]\d{1,3}%", rate):
    raise ValueError("MES_AVATAR_RATE must look like '+0%' or '-5%'")
if not re.fullmatch(r"[+-]\d{1,3}Hz", pitch):
    raise ValueError("MES_AVATAR_PITCH must look like '+0Hz' or '+8Hz'")
decimal_number = re.compile(r"(?<![\d.])([+-]?)(\d+)\.(\d+)(?!\d|\.\d)")
chinese_digits = str.maketrans("0123456789", "零一二三四五六七八九")


def pronounce_decimal(match: re.Match) -> str:
    sign, whole, fraction = match.groups()
    if len(whole) == 1:
        whole = whole.translate(chinese_digits)
    prefix = {"-": "负", "+": "正"}.get(sign, "")
    return f"{prefix}{whole}点{fraction.translate(chinese_digits)}"


def bypass_system_proxy() -> None:
    """edge-tts opens a WebSocket to speech.platform.bing.com; a system proxy makes the
    handshake fail with HTTP 403. This process only talks to Bing, so drop proxy settings."""
    for name in ("http_proxy", "https_proxy", "all_proxy", "HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY"):
        os.environ.pop(name, None)
    os.environ["no_proxy"] = "*"
    os.environ["NO_PROXY"] = "*"


async def synthesize(text: str) -> bytes:
    import edge_tts

    # The free Edge endpoint occasionally drops a connection or throttles a burst with 403.
    # One silent retry covers the transient cases; genuine rate limiting still surfaces as 503.
    last_error: Exception | None = None
    for _ in range(2):
        try:
            audio = bytearray()
            async for chunk in edge_tts.Communicate(text, voice=voice, rate=rate, pitch=pitch).stream():
                if chunk["type"] == "audio":
                    audio.extend(chunk["data"])
            if audio:
                return bytes(audio)
            last_error = RuntimeError("No audio returned")
        except Exception as error:  # noqa: BLE001
            last_error = error
        await asyncio.sleep(0.8)
    raise last_error if last_error else RuntimeError("No audio returned")


def azure_ssml(text: str) -> str:
    escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")
    return (
        '<speak version="1.0" xml:lang="zh-CN" '
        'xmlns="http://www.w3.org/2001/10/synthesis">'
        f'<voice name="{voice}"><prosody rate="{rate}" pitch="{pitch}">{escaped}</prosody></voice>'
        "</speak>"
    )


def azure_target() -> tuple[str, dict]:
    """Azure Speech endpoint and headers; validated before any streaming starts."""
    key = os.environ.get("MES_AVATAR_AZURE_KEY", "").strip()
    if not key:
        raise RuntimeError("MES_AVATAR_AZURE_KEY is not set")
    url = os.environ.get("MES_AVATAR_AZURE_ENDPOINT", "").strip() or (
        f"https://{os.environ.get('MES_AVATAR_AZURE_REGION', 'eastasia').strip()}.tts.speech.microsoft.com"
        "/cognitiveservices/v1"
    )
    headers = {
        "Ocp-Apim-Subscription-Key": key,
        "Content-Type": "application/ssml+xml",
        "X-Microsoft-OutputFormat": "audio-24khz-48kbitrate-mono-mp3",
        "User-Agent": "mes-avatar-speech",
    }
    return url, headers


_http_session = None


def http_session():
    """One keep-alive session to Azure. A fresh TLS handshake costs 0.3~1s and is the
    difference between "语音立刻开口" and a visible pause before every sentence."""
    global _http_session
    import aiohttp

    if _http_session is None or _http_session.closed:
        _http_session = aiohttp.ClientSession(
            timeout=aiohttp.ClientTimeout(total=None, sock_connect=8, sock_read=20),
            # aiohttp 默认 15 秒就回收空闲连接，比心跳间隔还短，等于每次都重新握手；
            # 拉长到 3 分钟，保活交给 20 秒心跳。
            connector=aiohttp.TCPConnector(limit=8, keepalive_timeout=180),
            # trust_env keeps corporate HTTP proxies working for the plain HTTPS call to Azure.
            trust_env=True,
        )
    return _http_session


async def reset_http_session() -> None:
    global _http_session
    session, _http_session = _http_session, None
    if session is not None and not session.closed:
        await session.close()


class AzureAudioStream:
    """Azure 边合成边下行：首包一到就能开播，不必等整段音频生成完。

    整段等待是数字人"答案出来好几秒才开口"的主要原因：一段 280 字的回答光合成就要 6 秒以上，
    而首包通常 0.5~1.0 秒就到。失败只在首包之前重试，避免半句话重复。
    """

    def __init__(self, text: str):
        self.text = text
        self.url, self.headers = azure_target()
        self.response = None
        self.chunks = None
        self.first = b""

    async def open(self) -> bytes:
        last_error: Exception | None = None
        for attempt in range(2):
            session = http_session()
            try:
                response = await session.post(self.url, data=azure_ssml(self.text).encode("utf-8"),
                                              headers=self.headers)
                if response.status != 200:
                    await response.read()
                    raise RuntimeError(f"Azure Speech returned {response.status}")
                chunks = response.content.iter_any()
                first = await chunks.__anext__()
                if not first:
                    raise RuntimeError("Azure Speech returned no audio")
            except Exception as error:  # noqa: BLE001
                last_error = error
                # 连接被对端回收时立刻换一条新连接重试，不睡眠：睡眠只会把空白拉长。
                # 业务错误（401/429）等一会儿再试没有意义，直接交给上层回 503。
                if isinstance(error, RuntimeError):
                    await asyncio.sleep(0.4)
                else:
                    await reset_http_session()
                continue
            self.response, self.chunks, self.first = response, chunks, first
            return first
        raise last_error if last_error else RuntimeError("Azure Speech unavailable")

    async def body(self):
        try:
            yield self.first
            async for chunk in self.chunks:
                if chunk:
                    yield chunk
        finally:
            await self.close()

    async def close(self) -> None:
        if self.response is not None:
            self.response.release()
            self.response = None


def synthesize_windows(text: str) -> bytes:
    powershell = Path(os.environ["SystemRoot"], "System32", "WindowsPowerShell", "v1.0", "powershell.exe")
    command = Path(__file__).with_name("windows-speech.ps1").read_text(encoding="utf-8")
    result = subprocess.run(
        [str(powershell), "-NoProfile", "-NonInteractive", "-Command", command],
        input=text.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        timeout=24,
        creationflags=subprocess.CREATE_NO_WINDOW,
    )
    audio = result.stdout
    if result.returncode or audio[:4] != b"RIFF" or audio[8:12] != b"WAVE":
        raise RuntimeError("Windows did not return WAV audio")
    return audio


def validated_text(value: object) -> str:
    text = value.strip() if isinstance(value, str) else ""
    if not 1 <= len(text) <= max_text_length:
        raise ValueError("朗读文本为空或过长。")
    return decimal_number.sub(pronounce_decimal, text)


async def synthesize_audio(text: str) -> bytes:
    return await synthesize(text)


def create_edge_app():
    from contextlib import asynccontextmanager

    from fastapi import FastAPI, HTTPException
    from fastapi.responses import Response, StreamingResponse
    from pydantic import BaseModel, Field

    @asynccontextmanager
    async def lifespan(_app):
        # 保持到 Azure 的长连接。一次冷的 TLS 握手要 0.3~1 秒，正好落在"答案出来等几秒才开口"
        # 那段空白里；20 秒心跳把它摊掉，代价只有每次一个字。
        heartbeat = asyncio.create_task(keep_warm()) if engine == "azure" else None
        try:
            yield
        finally:
            if heartbeat is not None:
                heartbeat.cancel()
            await reset_http_session()

    api = FastAPI(docs_url=None, redoc_url=None, openapi_url=None, lifespan=lifespan)

    class SpeechInput(BaseModel):
        text: str = Field(min_length=1, max_length=max_text_length)

    @api.post(speech_path)
    async def speak(request: SpeechInput) -> Response:
        try:
            text = validated_text(request.text)
        except ValueError as error:
            raise HTTPException(status_code=422, detail=str(error)) from None
        touch_activity()
        if engine == "azure":
            # 首包先拿到再返回 200，失败仍能回 503；之后边收边发，浏览器可以立刻开播。
            stream = AzureAudioStream(text)
            try:
                await asyncio.wait_for(stream.open(), timeout=24)
            except Exception as error:  # noqa: BLE001
                await stream.close()
                logger.warning("MES speech synthesis failed (%s)", type(error).__name__)
                raise HTTPException(status_code=503, detail="语音暂不可用。") from None
            return StreamingResponse(stream.body(), media_type="audio/mpeg",
                                     headers={"Cache-Control": "no-store"})
        try:
            audio = await asyncio.wait_for(synthesize_audio(text), timeout=24)
            if not audio:
                raise ValueError("No audio returned")
        except Exception as error:  # noqa: BLE001
            logger.warning("MES speech synthesis failed (%s)", type(error).__name__)
            raise HTTPException(status_code=503, detail="语音暂不可用。") from None
        return Response(content=audio, media_type="audio/mpeg", headers={"Cache-Control": "no-store"})

    return api


async def warm_connection() -> None:
    """合成一个字并读完响应，让连接完整回到连接池。"""
    url, headers = azure_target()
    async with http_session().post(url, data=azure_ssml("好").encode("utf-8"), headers=headers) as response:
        await response.read()
        if response.status != 200:
            raise RuntimeError(f"Azure Speech returned {response.status}")


last_activity = 0.0


def touch_activity() -> None:
    global last_activity
    last_activity = time.monotonic()


async def keep_warm(interval: float = 20.0, idle_after: float = 300.0) -> None:
    """有人用的时候才保活：连到 Azure 的 20 秒心跳能让首包稳定在 0.3 秒左右，
    idle_after 秒内没人朗读就停掉，避免空闲时白跑合成。"""
    while True:
        await asyncio.sleep(interval)
        if time.monotonic() - last_activity > idle_after:
            continue
        try:
            await warm_connection()
        except asyncio.CancelledError:
            raise
        except Exception as error:  # noqa: BLE001
            logger.warning("MES speech warm-up failed (%s)", type(error).__name__)
            await reset_http_session()


def windows_app(environ, start_response):
    def reply(status, content_type, content):
        start_response(status, [
            ("Content-Type", content_type),
            ("Content-Length", str(len(content))),
            ("Cache-Control", "no-store"),
        ])
        return [content]

    if environ["PATH_INFO"] != speech_path:
        return reply("404 Not Found", "text/plain", b"Not found.")
    if environ["REQUEST_METHOD"] != "POST":
        return reply("405 Method Not Allowed", "text/plain", b"Use POST.")
    try:
        size = int(environ.get("CONTENT_LENGTH") or 0)
        if not 1 <= size <= 4096:
            raise ValueError("Invalid speech request length")
        data = json.loads(environ["wsgi.input"].read(size))
        text = validated_text(data.get("text") if isinstance(data, dict) else None)
    except ValueError:
        return reply("422 Unprocessable Content", "text/plain", b"Invalid speech text.")
    try:
        audio = synthesize_windows(text)
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        logger.warning("MES speech synthesis failed (%s)", type(error).__name__)
        return reply("503 Service Unavailable", "text/plain", b"Speech service unavailable.")
    return reply("200 OK", "audio/wav", audio)


class WindowsSpeechServer(ThreadingMixIn, WSGIServer):
    daemon_threads = True

    def get_request(self):
        request, address = super().get_request()
        request.settimeout(10)
        return request, address


class QuietSpeechHandler(WSGIRequestHandler):
    def log_message(self, format, *args):
        pass


def free_listen_port(port: int) -> None:
    """Stop a previous copy of this service that still holds the port (Windows only).

    Without this the batch launcher dies on a "port already in use" error and the console
    window closes before anyone can read it."""
    if os.name != "nt":
        return
    try:
        listing = subprocess.run(["netstat", "-ano", "-p", "tcp"], capture_output=True, text=True, timeout=8,
                                 creationflags=subprocess.CREATE_NO_WINDOW).stdout
    except (OSError, subprocess.SubprocessError):
        return
    for line in listing.splitlines():
        fields = line.split()
        if len(fields) < 5 or fields[3] != "LISTENING" or f":{port}" not in fields[1]:
            continue
        pid = fields[4]
        if not pid.isdigit() or int(pid) == os.getpid():
            continue
        try:
            subprocess.run(["taskkill", "/PID", pid, "/F"], capture_output=True, timeout=8,
                           creationflags=subprocess.CREATE_NO_WINDOW)
            print(f"MES speech service: stopped previous listener on port {port} (pid {pid})", flush=True)
            time.sleep(0.6)
        except (OSError, subprocess.SubprocessError):
            pass


if engine == "edge":
    bypass_system_proxy()

app = create_edge_app() if engine != "windows" else windows_app


if __name__ == "__main__":
    port = int(os.environ.get("MES_AVATAR_PORT", "12394"))
    free_listen_port(port)
    if engine == "windows":
        with make_server("127.0.0.1", port, app, WindowsSpeechServer, QuietSpeechHandler) as server:
            print(f"MES Windows speech service listening on http://127.0.0.1:{port}", flush=True)
            server.serve_forever()
    else:
        import uvicorn

        print(f"MES {engine} speech service listening on http://127.0.0.1:{port} "
              f"(voice={voice} rate={rate} pitch={pitch})", flush=True)
        uvicorn.run(app, host="127.0.0.1", port=port, access_log=False)
