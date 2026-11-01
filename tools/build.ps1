param([string[]]$Tasks = @('testDebugUnitTest','assembleRelease'))
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$oldWork = 'C:/Users/vantr/Documents/Codex/2026-10-23/h-y-cho-t-i-t/work'
if (-not $env:JAVA_HOME -and (Test-Path 'C:/Program Files/Java/jdk-21.0.11')) { $env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.11' }
if (-not $env:ANDROID_HOME -and (Test-Path "$oldWork/toolchain/sdk")) { $env:ANDROID_HOME = "$oldWork/toolchain/sdk" }
if (-not $env:GRADLE_USER_HOME -and (Test-Path "$oldWork/gradle-cache")) { $env:GRADLE_USER_HOME = "$oldWork/gradle-cache" }
Set-Location -LiteralPath $repo
if (Test-Path "$oldWork/toolchain/gradle-8.7/bin/gradle.bat") {
    & "$oldWork/toolchain/gradle-8.7/bin/gradle.bat" @Tasks --console=plain --no-daemon
} else {
    & "$repo/gradlew.bat" @Tasks --console=plain --no-daemon
}
exit $LASTEXITCODE
