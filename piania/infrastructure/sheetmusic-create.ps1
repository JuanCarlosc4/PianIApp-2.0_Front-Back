$ErrorActionPreference = 'Stop'
$token = (Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'token.txt')).Trim()

$bodyPath = Join-Path $PSScriptRoot 'sheetmusic-create.json'
if (-not (Test-Path -LiteralPath $bodyPath)) {
    throw "No existe $bodyPath"
}

$curl = (Get-Command curl.exe -ErrorAction Stop).Source

& $curl -s -S -v -X POST 'http://localhost:8090/piania/core/sheet-music' `
    -H "Authorization: Bearer $token" `
    -H "Content-Type: application/json" `
    --data-binary "@$bodyPath"
