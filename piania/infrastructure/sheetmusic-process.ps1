$ErrorActionPreference = 'Stop'
$token = (Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'token.txt')).Trim()
$sheetId = 16

$curl = (Get-Command curl.exe -ErrorAction Stop).Source

& $curl -s -S -v -X POST "http://localhost:8090/piania/core/sheet-music/$sheetId/process" `
    -H "Authorization: Bearer $token" `
    -w "`nHTTP=%{http_code}`n"
