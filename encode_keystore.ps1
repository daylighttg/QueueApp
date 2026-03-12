# Usage: .\encode_keystore.ps1 release.keystore
# Then copy the output and paste it into your GitHub secret KEYSTORE_BASE64
param([string]$keystorePath)
if (-not $keystorePath -or -not (Test-Path $keystorePath)) {
    Write-Error "Error: keystore file not found: '$keystorePath'"
    Write-Host "Usage: .\encode_keystore.ps1 <path-to-keystore>"
    exit 1
}
[Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
Write-Host "Copy the line above into your GitHub secret KEYSTORE_BASE64"
