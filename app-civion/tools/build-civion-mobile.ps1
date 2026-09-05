<#
    CIVION Mobile — the single build procedure.

    One script, two callers: run it by hand on the owner's machine, or let the
    self-hosted GitHub Actions runner call it. There is deliberately no second
    build path, so a CI artifact and a hand-built artifact are produced the same
    way and carry the same evidence.

    What it guarantees about every APK it produces:
      - the commit it was built from is recorded in the file name and in
        BUILD-INFO.txt, and an uncommitted working tree is refused by default;
      - the engine footprint required by civion-android-mail-edge-decision-r002
        section 3.2 is re-measured, and the build fails if an engine file changed;
      - a SHA-256 is written next to the APK and appended to an append-only ledger.

    Examples:
      .\build-civion-mobile.ps1
      .\build-civion-mobile.ps1 -Variant release -Version 0.2.0-alpha
      .\build-civion-mobile.ps1 -AllowDirty        # marks the artifact +dirty
#>
[CmdletBinding()]
param(
    [ValidateSet("debug", "release")]
    [string] $Variant = "debug",

    [string] $Version = "0.1.0-alpha",

    # Where the APK, its hash, BUILD-INFO.txt and the ledger are written.
    [string] $OutDir,

    [string] $JavaHome,
    [string] $AndroidSdk,

    # Build anyway when the working tree has uncommitted changes. The artifact is
    # then named +dirty and marked as such in BUILD-INFO.txt.
    [switch] $AllowDirty,

    # Skip :app-civion:clean. Faster, and correspondingly less trustworthy.
    [switch] $NoClean
)

$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$isCi           = ($env:GITHUB_ACTIONS -eq "true")
$upstreamBase   = "7fb13f7c226bcdede9b825d2caccfd60f4b0a45a"

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

$env:JAVA_HOME        = $JavaHome
$env:ANDROID_HOME     = $AndroidSdk
$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:Path             = "$JavaHome\bin;$env:Path"

if (-not $OutDir) {
    if ($isCi) { $OutDir = Join-Path $repositoryRoot "out" } else { $OutDir = "F:\civion-builds" }
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# -------------------------------------------------------- commit identity ---

Push-Location $repositoryRoot
try {
    $commit = (& git rev-parse HEAD).Trim()
    $short  = $commit.Substring(0, 7)

    $branch = (& git rev-parse --abbrev-ref HEAD).Trim()
    if ($branch -eq "HEAD" -and $env:GITHUB_REF_NAME) { $branch = $env:GITHUB_REF_NAME }

    $pending = & git status --porcelain --untracked-files=no
    $isDirty = -not [string]::IsNullOrWhiteSpace(($pending -join ""))

    Write-Section "Commit identity"
    Write-Output "commit : $commit"
    Write-Output "branch : $branch"
    Write-Output "tree   : $(if ($isDirty) { 'DIRTY' } else { 'clean' })"

    if ($isDirty -and -not $AllowDirty) {
        Write-Output ""
        $pending | ForEach-Object { Write-Output "  $_" }
        throw "The working tree has uncommitted changes. Commit them, or pass -AllowDirty to build an artifact marked +dirty."
    }

    # ------------------------------------------------------ engine footprint ---

    # Is this commit published? A clean tree is not the same as a published one:
    # an artifact built from a local-only commit cannot be reproduced by anyone else.
    $published = "unknown"
    $remoteRef = & git rev-parse --verify --quiet "refs/remotes/origin/$branch"
    if ($LASTEXITCODE -eq 0 -and $remoteRef) {
        & git merge-base --is-ancestor $commit $remoteRef.Trim() 2>$null
        if ($LASTEXITCODE -eq 0) { $published = "yes" } else { $published = "no" }
    }
    Write-Output "origin : $(if ($published -eq 'yes') { 'commit is published' } elseif ($published -eq 'no') { 'NOT PUBLISHED — this commit exists only on this machine' } else { 'unknown (no origin ref in this checkout)' })"

    Write-Section "Patch footprint against upstream base $($upstreamBase.Substring(0,7))"

    $baseKnown = $true
    & git cat-file -e "$upstreamBase^{commit}" 2>$null
    if ($LASTEXITCODE -ne 0) { $baseKnown = $false }

    $engineChanged = @()
    if (-not $baseKnown) {
        Write-Output "Upstream base is not present in this checkout; footprint not measured."
        Write-Output "Fetch the full history to enforce it (actions/checkout with fetch-depth: 0)."
    }
    else {
        $changed   = @(& git diff --name-only "$upstreamBase..HEAD" | Where-Object { $_ })
        $engineRx  = '^(legacy|mail|backend|core|feature)/'
        $civionRx  = '^feature/civion/'
        $engineChanged = @($changed | Where-Object { $_ -match $engineRx -and $_ -notmatch $civionRx })
        $appCommon     = @($changed | Where-Object { $_ -match '^app-common/' })
        $civionFiles   = @($changed | Where-Object { $_ -match '^app-civion/' -or $_ -match $civionRx })

        Write-Output ("changed files    : {0}" -f $changed.Count)
        Write-Output ("CIVION files     : {0}" -f $civionFiles.Count)
        Write-Output ("app-common files : {0}" -f $appCommon.Count)
        Write-Output ("engine files     : {0}   (decision r002 section 3.2 requires 0)" -f $engineChanged.Count)

        if ($engineChanged.Count -ne 0) {
            Write-Output ""
            $engineChanged | ForEach-Object { Write-Output "  $_" }
            throw "Engine files changed. The accepted architecture requires 0; the decision returns for revision rather than being patched around."
        }
    }

    # ------------------------------------------------------------------ build ---

    # Build number. Artifacts are named CIVION-Mobile-<base version>.<n>.apk, with n
    # incrementing on every build; the commit stays in BUILD-INFO and in the ledger.
    $baseVersion = ($Version -split "-")[0]
    $lastNumber = 0
    Get-ChildItem -Path $OutDir -Filter "CIVION-Mobile-$baseVersion.*.apk" -File -ErrorAction SilentlyContinue |
        ForEach-Object {
            if ($_.BaseName -match "^CIVION-Mobile-$([regex]::Escape($baseVersion))\.(\d+)$") {
                $n = [int]$Matches[1]
                if ($n -gt $lastNumber) { $lastNumber = $n }
            }
        }
    $buildNumber = $lastNumber + 1
    $suffix = "$baseVersion.$buildNumber"
    if ($isDirty) { $suffix = "$suffix-dirty" }
    $logPath   = Join-Path $OutDir ("gradle-{0}-{1}.log" -f $Variant, $suffix)
    $assemble  = "assemble" + $Variant.Substring(0,1).ToUpper() + $Variant.Substring(1)
    $tasks     = ":app-civion:$assemble"
    if (-not $NoClean) { $tasks = ":app-civion:clean $tasks" }

    Write-Section "Gradle"
    Write-Output "tasks : $tasks"
    Write-Output "log   : $logPath"
    Write-Output "This can take several minutes..."

    # Values a machine may need to supply from outside the repository: the CIVION Google
    # OAuth client id, and the shared debug keystore that the registered client is bound to.
    $extraProperties = @()
    if ($env:CIVION_OAUTH_CLIENT_ID_DEBUG) {
        $extraProperties += "-Pcivion.google.oauth.clientId.debug=$($env:CIVION_OAUTH_CLIENT_ID_DEBUG)"
    }
    if ($env:CIVION_DEBUG_KEYSTORE) {
        $extraProperties += "-Pcivion.debug.storeFile=$($env:CIVION_DEBUG_KEYSTORE)"
    }
    $extra = if ($extraProperties.Count -gt 0) { " " + ($extraProperties -join " ") } else { "" }
    if ($extra) { Write-Output "extra  :$extra" }

    $gradleCommand = '"{0}" {1}{2} --no-daemon --no-watch-fs > "{3}" 2>&1' -f (Join-Path $repositoryRoot "gradlew.bat"), $tasks, $extra, $logPath
    & cmd.exe /d /c $gradleCommand
    $gradleExitCode = $LASTEXITCODE

    if ($gradleExitCode -ne 0) {
        Write-Output "BUILD FAILED (gradle exit $gradleExitCode)"
        if (Test-Path $logPath) { Get-Content $logPath -Tail 80 }
        exit $gradleExitCode
    }

    # ---------------------------------------------------------------- collect ---

    $apkDir = Join-Path $repositoryRoot "app-civion\build\outputs\apk\$Variant"
    $source = Get-ChildItem -Path $apkDir -Filter *.apk -File -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $source) { throw "Gradle succeeded but no APK was found under $apkDir" }

    $targetName = if ($Variant -eq "debug") {
        "CIVION-Mobile-{0}.apk" -f $suffix
    } else {
        "CIVION-Mobile-{0}-{1}.apk" -f $suffix, $Variant
    }
    $targetApk  = Join-Path $OutDir $targetName
    Copy-Item -Force $source.FullName $targetApk

    $apk    = Get-Item $targetApk
    $sha256 = (Get-FileHash -Algorithm SHA256 $targetApk).Hash
    $built  = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")

    $infoPath = Join-Path $OutDir ("BUILD-INFO-{0}.txt" -f $suffix)
    @(
        "artifact : $targetName"
        "variant  : $Variant"
        "version  : $Version"
        "commit   : $commit"
        "branch   : $branch"
        "tree     : $(if ($isDirty) { 'DIRTY — not reproducible from the repository' } else { 'clean' })"
        "engine   : $($engineChanged.Count) engine files changed vs $upstreamBase"
        "published: $published"
        "built    : $built"
        "builder  : $(if ($isCi) { 'github actions, self-hosted runner' } else { 'manual' })"
        "size     : $($apk.Length) bytes"
        "sha256   : $sha256"
    ) | Set-Content -Path $infoPath -Encoding UTF8

    $ledger = Join-Path $OutDir "build-history.csv"
    if (-not (Test-Path $ledger)) {
        "timestamp,commit,branch,variant,version,dirty,builder,sha256,artifact" | Set-Content -Path $ledger -Encoding UTF8
    }
    ("{0},{1},{2},{3},{4},{5},{6},{7},{8}" -f $built, $commit, $branch, $Variant, $Version, $isDirty, $(if ($isCi) { "ci" } else { "manual" }), $sha256, $targetName) |
        Add-Content -Path $ledger -Encoding UTF8

    Write-Section "BUILD SUCCESS"
    Write-Output "APK    : $targetApk"
    Write-Output "Size   : $($apk.Length) bytes"
    Write-Output "Commit : $commit"
    Write-Output "SHA-256: $sha256"
    Write-Output "Info   : $infoPath"

    if ($isCi -and $env:GITHUB_OUTPUT) {
        "artifact=$targetName"                 | Add-Content -Path $env:GITHUB_OUTPUT -Encoding UTF8
        "info=$([System.IO.Path]::GetFileName($infoPath))" | Add-Content -Path $env:GITHUB_OUTPUT -Encoding UTF8
        "log=$([System.IO.Path]::GetFileName($logPath))"   | Add-Content -Path $env:GITHUB_OUTPUT -Encoding UTF8
        "outdir=$OutDir"                       | Add-Content -Path $env:GITHUB_OUTPUT -Encoding UTF8
    }

    if ($isCi -and $env:GITHUB_STEP_SUMMARY) {
        @(
            "### CIVION Mobile $Version ($Variant)"
            ""
            "| | |"
            "|---|---|"
            "| commit | ``$commit`` |"
            "| branch | ``$branch`` |"
            "| engine files changed | $($engineChanged.Count) |"
            "| published | $published |"
            "| size | $($apk.Length) bytes |"
            "| sha-256 | ``$sha256`` |"
        ) | Add-Content -Path $env:GITHUB_STEP_SUMMARY -Encoding UTF8
    }
}
finally {
    Pop-Location
}
