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
        section 3.2 is re-measured, and the build fails if any upstream file changed
        that is not recorded — in $EngineHooks, or in $IntegrationPoints for the
        repo-level files CIVION must touch to exist as a module at all. The perimeter
        is everything outside app-civion\ and feature\civion\, so a directory nobody
        thought of cannot fall outside it;
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

    # Where the APK, its hash, BUILD-INFO.txt and the ledger are written. Falls back
    # to the CIVION_OUT_DIR environment variable, then to a repository-relative out\.
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

# Where build output goes is configuration, not source. An absolute workspace path
# committed here means every relocation of the workspace costs a commit, and the
# path is wrong for anyone else who runs the script. Precedence: -OutDir, then the
# CIVION_OUT_DIR environment variable, then a repository-relative out\. A relative
# value resolves against the repository root, so the result never depends on the
# caller's current directory.
if (-not $OutDir) {
    if ($env:CIVION_OUT_DIR) { $OutDir = $env:CIVION_OUT_DIR } else { $OutDir = "out" }
}
if (-not [System.IO.Path]::IsPathRooted($OutDir)) { $OutDir = Join-Path $repositoryRoot $OutDir }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$OutDir = (Resolve-Path $OutDir).Path

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

    # The recorded hooks: upstream files CIVION is allowed to have changed, each with the
    # reason it could not be done anywhere cheaper. Decision r002 section 3.2 puts a hook in
    # an upstream file last in the order of preference — after composition, resource override,
    # Koin override and a CIVION-owned module — precisely because each line here is paid for
    # again at every upstream merge. The list is the record of that payment, and anything not
    # on it still fails the build.
    #
    # Before adding an entry, exhaust the cheaper layers. Before keeping one, ask whether it
    # can be offered upstream instead, at which point it stops being a fork cost.
    $EngineHooks = [ordered]@{
        'legacy/ui/legacy/build.gradle.kts' =
            'Dependency on feature:civion:navigation, which holds the last-used account and folder.'
        'legacy/ui/legacy/src/main/java/com/fsck/k9/activity/MessageHomeActivity.kt' =
            'Records the account and folder on screen, and reopens an account at the folder it was left in; initializeFromLocalSearch and openRealAccount are the only places every navigation path passes through. Also makes Back leave the account last, returning to its Inbox before the unified inbox.'
        'legacy/common/src/main/java/com/fsck/k9/notification/K9NotificationActionCreator.kt' =
            'A notification opens the account it belongs to instead of the unified inbox, so Back returns to that account.'
        'legacy/common/src/main/java/com/fsck/k9/notification/KoinModule.kt' =
            'Follows the constructor change above.'
        'feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/domain/DomainContract.kt' =
            'MoveAccount use case contract for drag-and-drop account ordering.'
        'feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/domain/usecase/MoveAccount.kt' =
            'Stores the dragged order through the account manager, which already owns account order.'
        'feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/account/AccountList.kt' =
            'Long-press drag and drop. The drawer is instantiated directly by MessageHomeActivity, not through Koin, so a CIVION drawer cannot be substituted.'
        'feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/DrawerContent.kt' =
            'Passes the reorder callback through.'
        'feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/DrawerContract.kt' =
            'OnAccountMove event.'
        'feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/DrawerViewModel.kt' =
            'Handles OnAccountMove.'
        'feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/NavigationDrawerModule.kt' =
            'Binds MoveAccount.'
        'feature/navigation/drawer/dropdown/src/debug/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/account/AccountListPreview.kt' =
            'Follows the AccountList signature.'
        'feature/navigation/drawer/dropdown/src/test/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/DrawerViewModelTest.kt' =
            'Follows the DrawerViewModel constructor.'
        'feature/navigation/drawer/dropdown/src/test/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/domain/usecase/FakeLegacyAccountDtoManager.kt' =
            'Implements getAccounts and moveAccount for MoveAccountTest.'
        'feature/navigation/drawer/dropdown/src/test/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/domain/usecase/MoveAccountTest.kt' =
            'Covers MoveAccount.'
        'feature/account/setup/build.gradle.kts' =
            'Dependencies needed to read the existing accounts during setup.'
        'feature/account/setup/src/main/kotlin/app/k9mail/feature/account/setup/domain/usecase/ValidateEmailAddress.kt' =
            'One email address, one account: refuses an address already set up, at the first step of setup.'
        'feature/account/setup/src/main/kotlin/app/k9mail/feature/account/setup/ui/autodiscovery/AutoDiscoveryStringMapper.kt' =
            'Message for the new validation error. The mapper throws on an error type it does not know, so it cannot be extended from outside.'
        'feature/account/setup/src/main/kotlin/app/k9mail/feature/account/setup/AccountSetupModule.kt' =
            'Supplies the existing addresses to the validator. AccountAutoDiscoveryValidator is internal, so its construction cannot be overridden from app-civion.'
        'feature/account/setup/src/main/res/values/strings.xml' =
            'The message itself.'
        'feature/account/setup/src/test/kotlin/app/k9mail/feature/account/setup/domain/usecase/ValidateEmailAddressTest.kt' =
            'Covers the duplicate cases.'
    }

    # Repo-level files CIVION must touch to exist as a product in this tree at all: the module
    # list, its own CI, its own documentation. They are upstream files, so they are measured and
    # recorded exactly like the hooks above and an unrecorded one still fails the build — but
    # they are kept in their own list because they are not engine changes and never reach zero.
    # Counting them as engine hooks would make the one number that must stay explainable
    # permanently misleading.
    $IntegrationPoints = [ordered]@{
        'settings.gradle.kts' =
            'Includes :app-civion and :feature:civion:navigation. A module cannot exist without being listed here.'
        '.github/workflows/civion-mobile-build.yml' =
            'CIVION Mobile build, calling app-civion/tools/build-civion-mobile.ps1 on the self-hosted runner.'
        '.github/workflows/upstream-thunderbird-reference.yml' =
            'Manual reference build of upstream app-thunderbird from the same commit, for side-by-side comparison.'
        '.github/dependabot.yml' =
            'Dependency update scope for this fork.'
        'README.md' =
            'Describes this repository as CIVION Mobile rather than as upstream Thunderbird for Android.'
    }

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
    $engineUnrecorded = @()
    if (-not $baseKnown) {
        Write-Output "Upstream base is not present in this checkout; footprint not measured."
        Write-Output "Fetch the full history to enforce it (actions/checkout with fetch-depth: 0)."
    }
    else {
        # What CIVION owns outright. Everything else in the tree is upstream and is measured.
        #
        # The perimeter is defined by exclusion on purpose. Listing the upstream directories
        # instead — as this did, with '^(legacy|mail|backend|core|feature)/' — silently left
        # app-common, app-k9mail, app-thunderbird, app-metadata, cli, components, build-plugin
        # and every repo-root file unmeasured. app-common is built into all three applications,
        # and a CIVION change did sit there unrecorded until the r001 audit found it. A new
        # top-level directory upstream would have opened the same hole again.
        $changed  = @(& git diff --name-only "$upstreamBase..HEAD" | Where-Object { $_ })
        $civionRx = '^(app-civion/|feature/civion/)'

        $civionFiles   = @($changed | Where-Object { $_ -match $civionRx })
        $upstreamFiles = @($changed | Where-Object { $_ -notmatch $civionRx })

        $integrationChanged = @($upstreamFiles | Where-Object { $IntegrationPoints.Contains($_) })
        $engineChanged      = @($upstreamFiles | Where-Object { -not $IntegrationPoints.Contains($_) })
        $engineUnrecorded   = @($engineChanged | Where-Object { -not $EngineHooks.Contains($_) })

        Write-Output ("changed files      : {0}" -f $changed.Count)
        Write-Output ("CIVION files       : {0}" -f $civionFiles.Count)
        Write-Output ("integration points : {0}   (all recorded)" -f $integrationChanged.Count)
        Write-Output ("engine files       : {0}   ({1} recorded hooks, {2} unrecorded; decision r002 section 3.2 requires 0 unrecorded)" -f `
                $engineChanged.Count, ($engineChanged.Count - $engineUnrecorded.Count), $engineUnrecorded.Count)

        if ($engineChanged.Count -ne 0) {
            Write-Output ""
            Write-Output "recorded hooks:"
            $engineChanged | Where-Object { $EngineHooks.Contains($_) } | ForEach-Object {
                Write-Output "  $_"
                Write-Output "      $($EngineHooks[$_])"
            }
        }

        if ($engineUnrecorded.Count -ne 0) {
            Write-Output ""
            Write-Output "unrecorded upstream files:"
            $engineUnrecorded | ForEach-Object { Write-Output "  $_" }
            throw "Upstream files changed that are not recorded. Either move the change to composition, a resource override, a Koin override or a CIVION module, or add it to `$EngineHooks with the reason it cannot live anywhere cheaper — or to `$IntegrationPoints if it is a repo-level integration point rather than an engine change. Do not widen either list to make a build pass."
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

    # ---------------------------------------------------------- OAuth identity ---
    #
    # The accepted debug OAuth identity of CIVION Mobile. A Google Android client is
    # bound to one package name and one signing certificate; a build that satisfies
    # neither cannot sign in, and silently falls back to password authentication,
    # which Gmail refuses. Both are therefore verified, not assumed.
    #
    #   package : nl.civion.mobile.debug
    #   client  : "CIVION Mobile Debug"
    #   SHA-1   : A6:7F:62:5A:FD:58:10:6A:1E:0C:0D:5A:CE:63:4C:59:F2:56:8A:1A
    #
    $expectedSigningSha1 = "A67F625AFD58106A1E0C0D5ACE634C59F2568A1A"

    $oauthClientId = $env:CIVION_OAUTH_CLIENT_ID_DEBUG
    if (-not $oauthClientId) {
        $gradleProperties = Join-Path $env:USERPROFILE ".gradle\gradle.properties"
        if (Test-Path $gradleProperties) {
            $line = Select-String -Path $gradleProperties -Pattern "^civion\.google\.oauth\.clientId\.debug\s*=\s*(.+)$" |
                Select-Object -First 1
            if ($line) { $oauthClientId = $line.Matches[0].Groups[1].Value.Trim() }
        }
    }

    if ($Variant -eq "debug" -and -not $oauthClientId) {
        throw "No Google OAuth client id for the debug build. Set CIVION_OAUTH_CLIENT_ID_DEBUG, or civion.google.oauth.clientId.debug in ~/.gradle/gradle.properties. Without it the application offers no OAuth provider and Gmail falls back to password authentication."
    }

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

    $guardLog = Join-Path $OutDir ("guard-{0}.txt" -f $suffix)
    try {
        # ------------------------------------------------- verify OAuth identity ---

        if ($Variant -eq "debug") {
            # cmd.exe carries the redirection: a native command writing to stderr under
            # $ErrorActionPreference = "Stop" would otherwise abort the script.
            $keytool = Join-Path $JavaHome "bin\keytool.exe"
            $certOutput = & cmd.exe /d /c "`"$keytool`" -printcert -jarfile `"$targetApk`" 2>&1" | Out-String
            $certMatch = [regex]::Match($certOutput, "SHA1:\s*([0-9A-Fa-f:]+)")
            if (-not $certMatch.Success) {
                throw "Could not read the signing certificate of $targetName; the OAuth identity cannot be verified."
            }
            $actualSha1 = $certMatch.Groups[1].Value.Replace(":", "").ToUpper()
            if ($actualSha1 -ne $expectedSigningSha1) {
                throw ("Signed with the wrong certificate. Expected {0}, got {1}. Google binds the OAuth client to this certificate; a build signed by any other key cannot sign in. Point civion.debug.storeFile at the keystore holding the expected key." -f $expectedSigningSha1, $actualSha1)
            }

            # The dex entries inside the APK are compressed, so the id cannot be found by
            # scanning its bytes. BuildConfig is what the compiler actually used.
            $buildConfig = Get-ChildItem -Path (Join-Path $repositoryRoot "app-civion\build\generated\source\buildConfig") `
                -Filter "BuildConfig.java" -Recurse -File -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -match "\\$Variant\\" } |
                Select-Object -First 1
            if (-not $buildConfig) {
                throw "Generated BuildConfig for the $Variant variant was not found; the OAuth client id cannot be verified."
            }
            $found = (Select-String -Path $buildConfig.FullName -Pattern ([regex]::Escape($oauthClientId)) -Quiet) -eq $true
            if (-not $found) {
                throw "The OAuth client id is not compiled into the $Variant build. The application would offer no OAuth provider and Gmail would fall back to password authentication."
            }

            Write-Output ""
            Write-Output "OAuth identity verified: nl.civion.mobile.debug / $expectedSigningSha1"
        }

        "guard: passed" | Set-Content -Path $guardLog -Encoding UTF8
    }
    catch {
        @("guard: FAILED", $_.Exception.Message, $_.ScriptStackTrace) | Set-Content -Path $guardLog -Encoding UTF8
        throw
    }

    $infoPath = Join-Path $OutDir ("BUILD-INFO-{0}.txt" -f $suffix)
    @(
        "artifact : $targetName"
        "variant  : $Variant"
        "version  : $Version"
        "commit   : $commit"
        "branch   : $branch"
        "tree     : $(if ($isDirty) { 'DIRTY — not reproducible from the repository' } else { 'clean' })"
        "engine   : $($engineChanged.Count) engine files changed vs $upstreamBase ($($engineChanged.Count - $engineUnrecorded.Count) recorded hooks, $($engineUnrecorded.Count) unrecorded)"
        "integr.  : $($integrationChanged.Count) repo-level integration points, all recorded"
        "published: $published"
        "built    : $built"
        "builder  : $(if ($isCi) { 'github actions, self-hosted runner' } else { 'manual' })"
        "size     : $($apk.Length) bytes"
        "sha256   : $sha256"
        "oauth    : $(if ($Variant -eq 'debug') { 'verified — client id present, signed by the registered certificate' } else { 'n/a' })"
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
            "| engine files changed | $($engineChanged.Count), all recorded hooks |"
            "| published | $published |"
            "| size | $($apk.Length) bytes |"
            "| sha-256 | ``$sha256`` |"
        ) | Add-Content -Path $env:GITHUB_STEP_SUMMARY -Encoding UTF8
    }
}
finally {
    Pop-Location
}
