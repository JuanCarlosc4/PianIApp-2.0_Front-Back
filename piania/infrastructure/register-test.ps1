$ErrorActionPreference = 'Stop'

# RegisterRequest != LoginRequest. We generate a proper payload for /register.
$payload = @{
    email       = "teacher@piania.com"
    password    = "test"
    fullName    = "Teacher User"
    accountType = "TEACHER"
} | ConvertTo-Json

try {
    $resp = Invoke-WebRequest `
        -Method Post `
        -Uri 'http://localhost:8082/piania/auth/register' `
        -ContentType 'application/json' `
        -Body $payload

    Write-Host ("STATUS=" + $resp.StatusCode)
    Write-Host $resp.Content
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
