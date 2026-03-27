param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$UserId = "demo-user"
)

$ErrorActionPreference = "Stop"

function ConvertTo-Base64Url([byte[]]$Bytes) {
    [Convert]::ToBase64String($Bytes).TrimEnd("=") -replace "\+", "-" -replace "/", "_"
}

function New-DemoJwt([string]$Subject, [string]$Secret) {
    $headerJson = '{"alg":"HS256","typ":"JWT"}'
    $exp = [DateTimeOffset]::UtcNow.AddHours(1).ToUnixTimeSeconds()
    $payloadJson = '{"sub":"' + $Subject + '","exp":' + $exp + '}'

    $header = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($headerJson))
    $payload = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payloadJson))
    $unsignedToken = "$header.$payload"

    $secretBytes = [Convert]::FromBase64String($Secret)
    $hmac = [System.Security.Cryptography.HMACSHA256]::new($secretBytes)
    try {
        $signatureBytes = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsignedToken))
    }
    finally {
        $hmac.Dispose()
    }

    $signature = ConvertTo-Base64Url $signatureBytes
    return "$unsignedToken.$signature"
}

$jwtSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
$token = New-DemoJwt -Subject $UserId -Secret $jwtSecret
$idempotencyKey = [guid]::NewGuid().ToString()

$headers = @{
    Authorization = "Bearer $token"
    "X-Idempotency-Key" = $idempotencyKey
}

Write-Host "Checking service health..."
$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health"
$health | ConvertTo-Json -Depth 6

Write-Host ""
Write-Host "Creating demo order..."
$orderBody = @{
    amount = 499.99
    currency = "USD"
    description = "Resume demo payment order"
} | ConvertTo-Json

$createdOrder = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/api/v1/orders" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $orderBody

$createdOrder | ConvertTo-Json -Depth 6

Write-Host ""
Write-Host "Fetching created order..."
$fetchedOrder = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/api/v1/orders/$($createdOrder.id)" `
    -Headers @{ Authorization = "Bearer $token" }

$fetchedOrder | ConvertTo-Json -Depth 6

Write-Host ""
Write-Host "Listing current user's orders..."
$orders = Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/api/v1/orders" `
    -Headers @{ Authorization = "Bearer $token" }

$orders | ConvertTo-Json -Depth 6
