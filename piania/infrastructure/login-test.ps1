$ErrorActionPreference = 'Stop'

$body = Get-Content -Raw -LiteralPath "$PSScriptRoot\login-test.json"

# Comando para inciar el test: powershell -ExecutionPolicy Bypass -File .\\piania\\infrastructure\\login-test.ps1

try {
    # Invoke-RestMethod avoids the interactive "web parsing" prompt in some PowerShell configurations.
    $resp = Invoke-RestMethod `
        -Method Post `
        -Uri 'http://localhost:8082/piania/auth/login' `
        -ContentType 'application/json' `
        -Body $body

    $respJson = $resp | ConvertTo-Json -Depth 10
    Write-Host "STATUS=200"
    Write-Host $respJson

    if ($resp.accessToken) {
        Set-Content -LiteralPath "$PSScriptRoot\token.txt" -Value $resp.accessToken
    }
    if ($resp.refreshToken) {
        Set-Content -LiteralPath "$PSScriptRoot\refresh-token.txt" -Value $resp.refreshToken
    }
}
catch {
    if ($_.Exception.Response) {
        $r = $_.Exception.Response
        Write-Host ("STATUS=" + [int]$r.StatusCode)
        $sr = New-Object System.IO.StreamReader($r.GetResponseStream())
        Write-Host $sr.ReadToEnd()
    } else {
        throw
    }
}
