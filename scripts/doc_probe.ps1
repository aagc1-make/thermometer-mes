$ErrorActionPreference = "Continue"
$log = "D:\My_project\测温枪\runtime\doc_probe.log"
"probe start" | Out-File -FilePath $log -Encoding UTF8
foreach ($id in @("Word.Application", "KWPS.Application")) {
    try {
        $app = New-Object -ComObject $id
        "$id => OK version " + $app.Version | Out-File -FilePath $log -Append -Encoding UTF8
        $app.Quit()
    }
    catch {
        "$id => FAIL " + $_.Exception.Message | Out-File -FilePath $log -Append -Encoding UTF8
    }
}
"probe end" | Out-File -FilePath $log -Append -Encoding UTF8
