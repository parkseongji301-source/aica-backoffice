# Run the development server from a stable working directory.
# -Bootstrap prompts for the first SUPER_ADMIN; existing accounts are never replaced.
[CmdletBinding()]
param([switch]$Bootstrap)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJavaRoot = $env:JAVA_HOME
if (-not $taskJavaRoot) {
    $taskLocalJdk = Join-Path $taskRoot '.tools/jdk'
    if (Test-Path -LiteralPath $taskLocalJdk) {
        $taskJdk = Get-ChildItem -LiteralPath $taskLocalJdk -Directory |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe') } |
            Select-Object -First 1
        if ($taskJdk) { $taskJavaRoot = $taskJdk.FullName }
    }
}
if (-not $taskJavaRoot) { throw 'Install JDK 17 and set JAVA_HOME first.' }
$taskJava = Join-Path $taskJavaRoot 'bin/java.exe'
$taskJar = Join-Path $taskRoot 'target/backoffice-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $taskJar)) { throw 'Build first: .\scripts\mvn-local.ps1 -Pegov43-probe clean verify' }
$taskPreviousEnabled = $env:BACKOFFICE_BOOTSTRAP_ENABLED
$taskPreviousEmail = $env:BACKOFFICE_BOOTSTRAP_EMAIL
$taskPreviousPassword = $env:BACKOFFICE_BOOTSTRAP_PASSWORD
Push-Location $taskRoot
try {
    $env:BACKOFFICE_BOOTSTRAP_ENABLED = 'false'
    if ($Bootstrap) {
        $env:BACKOFFICE_BOOTSTRAP_EMAIL = Read-Host 'First SUPER_ADMIN email'
        $taskSecurePassword = Read-Host 'Initial password (at least 12 characters)' -AsSecureString
        $taskPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($taskSecurePassword)
        try {
            $env:BACKOFFICE_BOOTSTRAP_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($taskPasswordPointer)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($taskPasswordPointer)
            $taskSecurePassword.Dispose()
        }
        $env:BACKOFFICE_BOOTSTRAP_ENABLED = 'true'
    }
    & $taskJava -jar $taskJar '--spring.profiles.active=dev'
    $taskExitCode = $LASTEXITCODE
} finally {
    $env:BACKOFFICE_BOOTSTRAP_ENABLED = $taskPreviousEnabled
    $env:BACKOFFICE_BOOTSTRAP_EMAIL = $taskPreviousEmail
    $env:BACKOFFICE_BOOTSTRAP_PASSWORD = $taskPreviousPassword
    Pop-Location
}
exit $taskExitCode
