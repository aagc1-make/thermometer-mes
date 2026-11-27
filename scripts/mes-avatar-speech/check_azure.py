"""Standalone Azure Speech smoke test.

Usage:  python check_azure.py <subscription-key> [region]

Sends one SSML request with the voice this project uses and reports what came back.
Exit codes: 0 audio returned, 2 auth refused, 3 other HTTP error, 4 network error.
"""
import asyncio
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

VOICE = "zh-CN-XiaoyiNeural"
TEXT = "这是一次 Azure 语音测试，三号产线工单已完成报工。"


def ssml() -> str:
    return (
        '<speak version="1.0" xml:lang="zh-CN" xmlns="http://www.w3.org/2001/10/synthesis">'
        f'<voice name="{VOICE}"><prosody rate="-5%" pitch="+8Hz">{TEXT}</prosody></voice>'
        "</speak>"
    )


async def main() -> int:
    if len(sys.argv) < 2 or not sys.argv[1].strip():
        print("usage: python check_azure.py <subscription-key> [region]")
        return 1
    key = sys.argv[1].strip()
    region = (sys.argv[2] if len(sys.argv) > 2 else "eastasia").strip() or "eastasia"

    import aiohttp

    url = f"https://{region}.tts.speech.microsoft.com/cognitiveservices/v1"
    print(f"KEY    : {key[:4]}...{key[-4:]} ({len(key)} chars)")
    print(f"REGION : {region}")
    print(f"URL    : {url}")
    headers = {
        "Ocp-Apim-Subscription-Key": key,
        "Content-Type": "application/ssml+xml",
        "X-Microsoft-OutputFormat": "audio-24khz-48kbitrate-mono-mp3",
    }
    try:
        async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=25)) as session:
            async with session.post(url, data=ssml().encode("utf-8"), headers=headers) as response:
                body = await response.read()
                if response.status == 200:
                    open("azure-test.mp3", "wb").write(body)
                    print(f"OK: {len(body)} bytes saved to azure-test.mp3")
                    print("播放 azure-test.mp3 就能听到最终效果。")
                    return 0
                print(f"HTTP {response.status}")
                print("DETAIL:", body[:400].decode("utf-8", "replace"))
                if response.status in (401, 403):
                    print("提示：key 或区域不匹配，或者这个资源不含语音能力。")
                    return 2
                if response.status == 429:
                    print("提示：超出配额，去门户看用量。")
                return 3
    except Exception as error:  # noqa: BLE001
        print(f"NETWORK ERROR: {type(error).__name__}: {error}")
        print("提示：这台机器的网络到不了 Azure（代理/防火墙），换网络或配置代理。")
        return 4


raise SystemExit(asyncio.run(main()))
