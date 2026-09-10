<#
    CIVION Mobile - the Android runtime smoke run.

    One procedure, two callers, exactly like build-civion-mobile.ps1: run it by hand, or let
    the self-hosted runner call it. The reason it exists rather than a line in a README is the
    emulator flags below. Without them the run does not fail - it hangs, for twelve minutes,
    and then reports a bare TimeoutException from a task that never got as far as installing
    anything. That is not a thing anyone should have to rediscover.

    What it runs:
      Gradle's managed device `pixelApi36` - API 36, AOSP ATD, x86_64, Pixel 6 profile -
      through the full sequence: provision the AVD, boot it, install the application and the
      instrumentation APK, execute the tests in the application's own process, shut down.

    Examples:
      .\run-managed-device-tests.ps1
      .\run-managed-device-tests.ps1 -Edition integrated
#>
[CmdletBinding()]
param(
    [ValidateSet("standalone", "integrated")]
    [string] $Edition = "standalone",

    [ValidateSet("debug", "release")]
    [string] $Variant = "debug",

    [string] $JavaHome,
    [string] $AndroidSdk,

    # Minutes allowed for provisioning and first boot. The default is generous on purpose: a
    # cold image on a cold machine is slow once, and a timeout here costs a whole run.
    [int] $SetupTimeoutMinutes = 30
)

$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

function Write-Section([string] $Text) {
    Write-Output ""
    Write-Output "=== $Text"
}

# ---------------------------------------------------------------- toolchain ---

if (-not $JavaHome) {
    if ($env:JAVA_HOME) { $JavaHome = $env:JAVA_HOME } else { $JavaHome = "F:\Android\jdk-21" }
}
if (-not $AndroidSdk) {
    if ($env:ANDROID_HOME) { $AndroidSdk = $env:ANDROID_HOME }
    elseif ($env:ANDROID_SDK_ROOT) { $AndroidSdk = $env:ANDROID_SDK_ROOT }
    else { $AndroidSdk = "F:\Android\Sdk" }
}

if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
    throw "JDK 21 was not found at $JavaHome. Pass -JavaHome or set JAVA_HOME."
}
if (-not (Test-Path $AndroidSdk)) {
    throw "Android SDK was not found at $AndroidSdk. Pass -AndroidSdk or set ANDROID_HOME."
}

$emulator = Join-Path $AndroidSdk "emulator\emulator.exe"
if (-not (Test-Path $emulator)) {
    throw "The emulator was not found at $emulator. A managed device cannot be provisioned without it."
}

# The image the managed device is declared against. Checked here so a missing image is named
# as such, rather than surfacing later as a provisioning failure.
$systemImage = Join-Path $AndroidSdk "system-images\android-36\aosp_atd\x86_64"
if (-not (Test-Path $systemImage)) {
    throw "The API 36 AOSP ATD x86_64 system image was not found at $systemImage. Install 'system-images;android-36;aosp_atd;x86_64'."
}

$env:JAVA_HOME        = $JavaHome
$env:ANDROID_HOME     = $AndroidSdk
$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:Path             = "$JavaHome\bin;$AndroidSdk\platform-tools;$env:Path"

Write-Section "Toolchain"
Write-Output "java    : $JavaHome"
Write-Output "sdk     : $AndroidSdk"
Write-Output "image   : android-36 / aosp_atd / x86_64"

# ------------------------------------------------------ stale emulator check ---

# A previous run that was killed leaves a qemu process holding the console port and the AVD
# lock. The next run then boots a device that never comes online, and times out looking at it.
#
# Only qemu counts, and only its name is looked at.
#
# A running device is always a qemu-system process; emulator.exe is the launcher, and after a
# perfectly good run AGP leaves one `emulator -kill <pid> -sleep 1800` watchdog per device
# behind for half an hour. Those hold nothing. Telling them apart by command line worked when
# run by hand and failed on the runner, which executes as NETWORK SERVICE and cannot read the
# command line of a process owned by another account: the property came back empty, every
# watchdog looked like a device, and the step refused to start. Matching on the process name
# needs no such permission.
$live = @(Get-Process -Name "qemu-system-*" -ErrorAction SilentlyContinue)

if ($live.Count -ne 0) {
    Write-Section "Emulator already running"
    $live | ForEach-Object { Write-Output ("  {0} (pid {1})" -f $_.ProcessName, $_.Id) }
    throw "An emulator is already running. A managed device run needs the console port and the AVD lock to itself. Stop these processes, or wait for the run that owns them to finish."
}

# ------------------------------------------------------------------- device ---

$editionTitle = $Edition.Substring(0, 1).ToUpper() + $Edition.Substring(1)
$variantTitle = $Variant.Substring(0, 1).ToUpper() + $Variant.Substring(1)
$task = ":app-civion:pixelApi36$editionTitle${variantTitle}AndroidTest"

# swiftshader_indirect, not the default 'auto'.
#
# 'auto' tries to reach a host GPU. On a machine with no interactive session - a CI runner
# under a service account, or any headless invocation - that does not fail, it hangs: the
# emulator boots far enough for adb to report the device, and then never reaches
# sys.boot_completed. Measured on this project: 12 minutes to a bare TimeoutException with
# 'auto', 1 minute 49 seconds to a green run with software rendering.
#
# Nothing here looks at pixels, so a software renderer costs the run nothing.
$properties = @(
    "-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect"
    "-Pandroid.experimental.testOptions.managedDevices.setupTimeoutMinutes=$SetupTimeoutMinutes"
)

Write-Section "Managed device"
Write-Output "device  : pixelApi36 (Pixel 6, API 36, AOSP ATD, x86_64)"
Write-Output "task    : $task"
Write-Output "gpu     : swiftshader_indirect"
Write-Output "This provisions, boots, installs, runs and shuts down. It can take several minutes."

Push-Location $repositoryRoot
try {
    # --rerun on the device task itself, not on its dependencies.
    #
    # Gradle will call this task UP-TO-DATE whenever nothing it depends on changed, and skip it
    # - which is correct for a build and useless for this gate. The point of the run is to prove
    # that a device can be provisioned, booted and driven now; a skipped task proves that it
    # could be, once. Everything the task depends on is still built incrementally.
    $gradleCommand = '"{0}" {1} --rerun {2} --no-daemon --no-watch-fs' -f `
        (Join-Path $repositoryRoot "gradlew.bat"), $task, ($properties -join " ")

    $started = Get-Date
    & cmd.exe /d /c $gradleCommand
    $exitCode = $LASTEXITCODE
    $elapsed = (Get-Date) - $started

    # ------------------------------------------------------------- results ---

    # Gradle reports a failing instrumentation test as a failed build, but a run that installed
    # nothing and executed nothing can also end without a test result at all. Read the report
    # rather than trusting the exit code alone.
    $resultsRoot = Join-Path $repositoryRoot "app-civion\build\outputs\androidTest-results\managedDevice"
    $resultFile = Get-ChildItem $resultsRoot -Filter "*.xml" -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match "\\$Edition\\" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    Write-Section "Result"
    Write-Output ("elapsed : {0:mm}m {0:ss}s" -f $elapsed)

    if (-not $resultFile) {
        throw "No instrumentation result was written under $resultsRoot. The device may never have run the tests; the Gradle output above says how far it got."
    }

    # The result has to be from this run. A report left by an earlier one reads exactly like a
    # fresh pass, which is the one way this gate could report a working device without having
    # started one.
    if ($resultFile.LastWriteTime -lt $started) {
        throw ("The only instrumentation result is from {0}, before this run started at {1}. Nothing was executed on a device now, so this run proves nothing." -f $resultFile.LastWriteTime, $started)
    }

    [xml] $results = Get-Content $resultFile.FullName
    $suites = $results.testsuites
    Write-Output ("device  : pixelApi36")
    Write-Output ("edition : $Edition")
    Write-Output ("tests   : {0}, failures {1}, errors {2}, skipped {3}" -f `
            $suites.tests, $suites.failures, $suites.errors, $suites.skipped)
    Write-Output ("report  : $($resultFile.FullName)")

    if ([int] $suites.tests -eq 0) {
        throw "The instrumentation run executed no tests. A green build here would mean nothing."
    }
    if ([int] $suites.failures -ne 0 -or [int] $suites.errors -ne 0) {
        throw "Instrumentation tests failed on pixelApi36. See $($resultFile.FullName)."
    }
    if ($exitCode -ne 0) {
        throw "Gradle exited $exitCode."
    }

    Write-Section "ANDROID RUNTIME OK"
    Write-Output "$($suites.tests) tests executed on a real device, provisioned and shut down by Gradle."
}
finally {
    Pop-Location
}
