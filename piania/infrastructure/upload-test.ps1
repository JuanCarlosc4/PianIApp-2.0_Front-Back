$ErrorActionPreference = 'Stop'

$tokenPath = Join-Path $PSScriptRoot 'token.txt'
if (-not (Test-Path -LiteralPath $tokenPath)) {
    throw "No existe token.txt. Ejecuta antes login-test.ps1"
}

$token = (Get-Content -Raw -LiteralPath $tokenPath).Trim()

$downloads = 'C:\Users\jcca3\Downloads'
$pdf = Get-ChildItem -LiteralPath $downloads -File -Filter 'Proyecto*DAM*(6).pdf' | Select-Object -First 1
if (-not $pdf) {
    throw "No se encontró el PDF en $downloads con patrón: Proyecto*DAM*(6).pdf"
}
$pdfPath = $pdf.FullName

# Subida multipart al gateway (que reescribe a core-service)
# -o guarda respuesta en un fichero temporal por si hay error
$tmpOut = Join-Path $env:TEMP 'piania-upload-response.txt'

# Usamos curl.exe para evitar alias de Invoke-WebRequest
$curl = (Get-Command curl.exe -ErrorAction Stop).Source

& $curl -s -S -v -X POST 'http://localhost:8090/piania/core/uploads' `
    -H "Authorization: Bearer $token" `
    -F "file=@$pdfPath" `
    -o $tmpOut `
    -w "`nHTTP=%{http_code}`n"

Write-Host "---- Response body (first 2000 chars) ----"
$body = Get-Content -Raw -LiteralPath $tmpOut -ErrorAction SilentlyContinue
if ($null -ne $body) {
    if ($body.Length -gt 2000) { $body = $body.Substring(0, 2000) }
    Write-Host $body
}
