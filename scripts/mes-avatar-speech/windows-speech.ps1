$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$synthClass = [Windows.Media.SpeechSynthesis.SpeechSynthesizer,Windows.Media.SpeechSynthesis,ContentType=WindowsRuntime]
$streamClass = [Windows.Media.SpeechSynthesis.SpeechSynthesisStream,Windows.Media.SpeechSynthesis,ContentType=WindowsRuntime]
$asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and
    $_.GetGenericArguments().Length -eq 1 -and $_.GetParameters().Length -eq 1 -and
    $_.GetParameters()[0].ParameterType.Name.StartsWith('IAsyncOperation')
} | Select-Object -First 1
$voice = $synthClass::AllVoices | Where-Object Language -eq 'zh-CN' | Select-Object -First 1
if (!$voice) { throw 'No Windows zh-CN voice is installed.' }
$synth = [Activator]::CreateInstance($synthClass)
try {
    $synth.Voice = $voice
    $operation = $synth.SynthesizeTextToStreamAsync([Console]::In.ReadToEnd())
    $task = $asTask.MakeGenericMethod($streamClass).Invoke($null, @($operation))
    $audio = $task.GetAwaiter().GetResult()
    $stream = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($audio.GetInputStreamAt(0))
    try { $stream.CopyTo([Console]::OpenStandardOutput()) } finally { $stream.Dispose(); $audio.Dispose() }
} finally {
    $synth.Dispose()
}
