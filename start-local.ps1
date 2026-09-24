$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
$originalDatabasePassword = $env:BLACKJACK_DB_PASSWORD
$databasePassword = Read-Host 'MySQL開発用ユーザーのパスワード' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($databasePassword)
try {
    $env:BLACKJACK_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    & .\gradlew.bat bootRun '--args=--spring.profiles.active=local'
    if ($LASTEXITCODE -ne 0) { throw '起動に失敗しました。直前のGradleログを確認してください。' }
} finally {
    $env:BLACKJACK_DB_PASSWORD = $originalDatabasePassword
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    Pop-Location
}
