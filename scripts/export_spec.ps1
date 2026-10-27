$ErrorActionPreference = "Continue"
$src = "D:\My_project\测温枪\红外测温枪MES系统_软件需求规格说明书1 (1).doc"
$log = "D:\My_project\测温枪\runtime\doc_export.log"
$html = "D:\My_project\测温枪\runtime\spec_export.html"
$txt = "D:\My_project\测温枪\runtime\spec_export.txt"

"start $(Get-Date -Format o)" | Out-File -FilePath $log -Encoding UTF8
try {
    $app = New-Object -ComObject Word.Application
    $app.Visible = $false
    $app.DisplayAlerts = 0
    "app ok" | Out-File -FilePath $log -Append -Encoding UTF8
    $doc = $app.Documents.Open($src, $false, $true)
    "opened paragraphs=" + $doc.Paragraphs.Count | Out-File -FilePath $log -Append -Encoding UTF8
    $doc.SaveAs2($html, 10)
    "html saved" | Out-File -FilePath $log -Append -Encoding UTF8
    $doc.SaveAs2($txt, 7)
    "txt saved" | Out-File -FilePath $log -Append -Encoding UTF8
    $doc.Close($false)
    $app.Quit()
    "done" | Out-File -FilePath $log -Append -Encoding UTF8
}
catch {
    "ERROR: " + $_.Exception.Message | Out-File -FilePath $log -Append -Encoding UTF8
}
