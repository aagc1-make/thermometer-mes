param (
    [string]$Source = (Join-Path $env:USERPROFILE 'Downloads\shuziren-main\shuziren-main'),
    [string]$CubismCorePath,
    [switch]$AssetsOnly
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$Source = (Resolve-Path -LiteralPath $Source).Path
$assets = Join-Path (Split-Path -Parent $PSScriptRoot) 'ktg-mes-ui\public\mes-avatar-assets'
$model = Join-Path $assets 'mao'
$vendor = Join-Path $assets 'vendor'
$sourceModel = Join-Path $Source 'live2d-models\mao_pro\runtime'

$null = New-Item -ItemType Directory -Force -Path $assets, $model, $vendor
Get-ChildItem -LiteralPath $sourceModel | Copy-Item -Destination $model -Recurse -Force
Copy-Item -LiteralPath (Join-Path $Source 'avatars\mao.png') -Destination (Join-Path $assets 'mao.png') -Force
Copy-Item -LiteralPath (Join-Path $Source 'backgrounds\ceiling-window-room-night.jpeg') -Destination (Join-Path $assets 'night.jpg') -Force

if (!$AssetsOnly) {
    $core = Join-Path $vendor 'live2dcubismcore.min.js'
    if ($CubismCorePath) {
        Copy-Item -LiteralPath $CubismCorePath -Destination $core -Force
    }

    $downloads = [ordered]@{
        'live2dcubismcore.min.js' = 'https://cubism.live2d.com/sdk-web/cubismcore/live2dcubismcore.min.js'
        'pixi.min.js' = 'https://cdn.jsdelivr.net/npm/pixi.js@7.4.3/dist/pixi.min.js'
        'cubism4.min.js' = 'https://cdn.jsdelivr.net/npm/pixi-live2d-display-lipsyncpatch@0.5.0-ls-8/dist/cubism4.min.js'
    }
    foreach ($entry in $downloads.GetEnumerator()) {
        $target = Join-Path $vendor $entry.Key
        if (!(Test-Path -LiteralPath $target -PathType Leaf)) {
            $pending = $target + '.download'
            Invoke-WebRequest -UseBasicParsing -Uri $entry.Value -OutFile $pending -TimeoutSec 20
            Move-Item -LiteralPath $pending -Destination $target -Force
        }
    }
}

Write-Output $assets
