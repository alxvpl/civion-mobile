$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$javaHome = "F:\Android\jdk-21"
$androidSdk = "F:\Android\Sdk"
$sourceApk = Join-Path $repositoryRoot "app-civion\build\outputs\apk\debug\app-civion-debug.apk"
$targetApk = "F:\CIVION-Mobile-0.1.0-alpha.apk"
$logPath = "F:\CIVION-Mobile-build.log"

if (-not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
    throw "JDK 21 was not found at $javaHome"
}

if (-not (Test-Path $androidSdk)) {
    throw "Android SDK was not found at $androidSdk"
}

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk
$env:Path = "$javaHome\bin;$env:Path"

Push-Location $repositoryRoot
try {
    Write-Output "Building CIVION Mobile. This can take several minutes..."

    $gradleCommand = '"{0}" :app-civion:clean :app-civion:assembleDebug --no-daemon --no-watch-fs > "{1}" 2>&1' -f (Join-Path $repositoryRoot "gradlew.bat"), $logPath

    & cmd.exe /d /c $gradleCommand
    $gradleExitCode = $LASTEXITCODE

    if ($gradleExitCode -ne 0) {
        Write-Output "BUILD FAILED"
        Write-Output "Log: $logPath"
        Get-Content $logPath -Tail 80
        exit $gradleExitCode
    }

    if (-not (Test-Path $sourceApk)) {
        throw "Gradle succeeded but the APK was not found at $sourceApk"
    }

    Copy-Item -Force $sourceApk $targetApk
    $apk = Get-Item $targetApk
    $sha256 = (Get-FileHash -Algorithm SHA256 $targetApk).Hash

    Write-Output "BUILD SUCCESS"
    Write-Output "APK: $targetApk"
    Write-Output "Size: $($apk.Length) bytes"
    Write-Output "Timestamp: $($apk.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss zzz'))"
    Write-Output "SHA-256: $sha256"
}
finally {
    Pop-Location
}
