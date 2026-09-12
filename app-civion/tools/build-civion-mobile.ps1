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
      - the artifact is checked against the edition it claims: the dex must carry that
        edition's marker package and none of the other one's, so a standalone build
        cannot contain CIVION integration code;
      - the build number in the artifact name and the versionCode inside the APK are
        computed independently and compared;
      - a SHA-256 is written next to the APK and appended to an append-only ledger.

    Examples:
      .\build-civion-mobile.ps1
      .\build-civion-mobile.ps1 -Edition integrated
      .\build-civion-mobile.ps1 -Variant release -Version 0.2.0-alpha
      .\build-civion-mobile.ps1 -AllowDirty        # marks the artifact +dirty
#>
[CmdletBinding()]
param(
    [ValidateSet("debug", "release")]
    [string] $Variant = "debug",

    # Which edition to build. Both carry the same application id and signing identity; they
    # differ in whether the CIVION integration layer was compiled in. The artifact is checked
    # against its edition after it is built, so a mislabelled one fails rather than ships.
    [ValidateSet("standalone", "integrated")]
    [string] $Edition = "standalone",

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

# Whether a CIVION module is one of the two allowed to name Thunderbird types - either the
# module itself, or something nested under it.
function Test-UpstreamFacing([string] $ModuleName, [string[]] $CrossingPoints) {
    foreach ($crossingPoint in $CrossingPoints) {
        if ($ModuleName -eq $crossingPoint -or $ModuleName -like "$crossingPoint\*") { return $true }
    }
    return $false
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
            'Constructs Android Mail''s drawer instead of the upstream one - the drawer is built here rather than resolved, so this is the only place it can be substituted, and owning it removed eleven hooks from feature/navigation/drawer/dropdown. Also records the account and folder on screen and reopens an account at the folder it was left in (initializeFromLocalSearch and openRealAccount are the only places every navigation path passes through), and makes Back leave the account last, returning to its Inbox before the unified inbox.'
        'legacy/ui/legacy/src/main/java/com/fsck/k9/ui/messageview/MessageViewFragment.kt' =
            'Connects reply all, forward and attachments on the action bar of CIVION Mail''s message header, which is a resource override of message_view_header (mockup screen 04). The fragment inflates the layout, so nothing else can reach those views; they are looked up by tag rather than by id, because ids generated in app-civion are not visible here, and looked up rather than required, so K-9''s and Thunderbird''s layouts are unaffected.'
        'legacy/common/src/main/java/com/fsck/k9/notification/K9NotificationActionCreator.kt' =
            'A notification opens the account it belongs to instead of the unified inbox, so Back returns to that account.'
        'legacy/common/src/main/java/com/fsck/k9/notification/KoinModule.kt' =
            'Follows the constructor change above.'
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
            throw "Upstream files changed that are not recorded. Either move the change to composition, a resource override, a Koin override or a CIVION module, or add it to `$EngineHooks with the reason it cannot live anywhere cheaper, or to `$IntegrationPoints if it is a repo-level integration point rather than an engine change. Do not widen either list to make a build pass."
        }
    }

    # -------------------------------------------------------- upstream boundary ---

    # Thunderbird upstream -> named crossing points -> CIVION's own layers.
    #
    # The footprint above measures what CIVION changed in upstream. This measures the opposite
    # direction: how far upstream types have travelled into CIVION.
    #
    # Two modules may name one, and only two:
    #
    #   adapter - resolves engine types and hands them on as feature\civion\core types.
    #   ui      - screens that replace upstream ones. A replacement has to implement the
    #             interface the host constructs, render what the engine owns, and use the shared
    #             theme. Routing that through an adapter would produce a layer whose whole job is
    #             copying display types, which buys nothing.
    #
    # Everything else CIVION owns — core, navigation, integration, brand and whatever storage and
    # intelligence come later — speaks core's types, so an upstream rename or a moved class stops
    # at a crossing point instead of being a change in every consumer.
    #
    # app-civion is not scanned: it is the composition layer, and wiring Koin bindings and
    # launching upstream activities is exactly what it is for.

    Write-Section "Upstream boundary"

    $upstreamFacing = @("adapter", "ui")
    $upstreamImportRx = '^\s*import\s+(com\.fsck\.k9|net\.thunderbird|app\.k9mail)\.'
    $civionRoot = Join-Path $repositoryRoot "feature\civion"
    $boundaryViolations = @()

    # Modules are found by looking for a src\ directory at any depth, not by listing the
    # children of feature\civion. Nesting is normal here - :feature:civion:integration:impl and
    # :noop live one level further down - and a scan that only looked at direct children
    # reported them as one unscanned "integration" module while quietly checking neither.
    $modules = @(Get-ChildItem $civionRoot -Recurse -Directory -Filter "src" -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -notmatch '\\build\\' } |
        ForEach-Object { $_.Parent })

    foreach ($module in $modules) {
        $moduleName = $module.FullName.Substring($civionRoot.Length + 1)
        if (Test-UpstreamFacing $moduleName $upstreamFacing) { continue }

        # core is a plain JVM module and holds the contracts; it may not reach Android either,
        # or the contracts stop being testable without a device.
        $forbidden = if ($moduleName -eq "core") { $upstreamImportRx, '^\s*import\s+android(x)?\.' } else { , $upstreamImportRx }

        $sources = @(Get-ChildItem (Join-Path $module.FullName "src") -Recurse -File -Include *.kt -ErrorAction SilentlyContinue)
        foreach ($source in $sources) {
            $lines = Get-Content $source.FullName
            for ($i = 0; $i -lt $lines.Count; $i++) {
                foreach ($rx in $forbidden) {
                    if ($lines[$i] -match $rx) {
                        $relative = $source.FullName.Substring($repositoryRoot.Length + 1)
                        $boundaryViolations += "{0}:{1}: {2}" -f $relative, ($i + 1), $lines[$i].Trim()
                    }
                }
            }
        }
    }

    $scanned = @($modules |
            ForEach-Object { $_.FullName.Substring($civionRoot.Length + 1) } |
        Where-Object { -not (Test-UpstreamFacing $_ $upstreamFacing) })

    Write-Output ("crossing points    : {0}" -f ($upstreamFacing -join ", "))
    Write-Output ("modules checked    : {0} ({1})" -f $scanned.Count, ($scanned -join ", "))
    Write-Output ("violations         : {0}" -f $boundaryViolations.Count)

    if ($boundaryViolations.Count -ne 0) {
        Write-Output ""
        $boundaryViolations | ForEach-Object { Write-Output "  $_" }
        throw "Thunderbird types reached a CIVION module that is not a crossing point. Put the upstream call in feature\civion\adapter and hand the result on as a feature\civion\core type, or - if it is a screen replacing an upstream one - put it in feature\civion\ui. Adding a third crossing point defeats the boundary it exists to hold."
    }

    # ------------------------------------------------------------------ build ---

    # Build identity.
    #
    # The number is the commit count reachable from HEAD, so it is the same for a given commit
    # no matter who builds it or where the output lands. It used to be one more than the highest
    # numbered APK already sitting in $OutDir, which made identity a property of a directory: a
    # fresh CI workspace started again at 1 and produced an artifact claiming a number an
    # earlier, different build already carried.
    #
    # Gradle derives the same number for versionCode, and is given this one so the name on the
    # file and the versionCode inside it cannot disagree. Two editions share one applicationId,
    # so that versionCode is what orders installs and upgrades between them.
    #
    # The commit count alone does not identify a commit - two branches can reach the same count -
    # so the short sha is part of the artifact name too. Together they order builds and name one
    # exactly.
    $baseVersion = ($Version -split "-")[0]
    $buildNumber = [int](& git rev-list --count HEAD).Trim()
    if ($buildNumber -le 0) {
        throw "Could not establish the build number: 'git rev-list --count HEAD' produced '$buildNumber'. A shallow clone is not enough; fetch the full history."
    }

    $suffix = "$baseVersion.$buildNumber-$Edition"
    if ($Variant -ne "debug") { $suffix = "$suffix-$Variant" }
    $suffix = "$suffix-$short"
    if ($isDirty) { $suffix = "$suffix-dirty" }

    $editionTitle = $Edition.Substring(0,1).ToUpper() + $Edition.Substring(1)
    $variantTitle = $Variant.Substring(0,1).ToUpper() + $Variant.Substring(1)

    $logPath   = Join-Path $OutDir ("gradle-{0}.log" -f $suffix)
    $assemble  = "assemble$editionTitle$variantTitle"
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
    $extraProperties = @("-Pcivion.build.number=$buildNumber")
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

    $apkDir = Join-Path $repositoryRoot "app-civion\build\outputs\apk\$Edition\$Variant"
    $source = Get-ChildItem -Path $apkDir -Filter *.apk -File -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $source) { throw "Gradle succeeded but no APK was found under $apkDir" }

    # $suffix already carries version, build number, edition, non-debug variant, short sha and
    # a dirty marker, in that order.
    # The product's name, so the file someone is about to install is not named after something
    # else. Only the prefix changed; the identity after it - version, build number, edition,
    # variant, short sha - is the same one, derived the same way.
    $targetName = "CIVION-Mail-{0}.apk" -f $suffix
    $targetApk  = Join-Path $OutDir $targetName
    Copy-Item -Force $source.FullName $targetApk

    $apk    = Get-Item $targetApk
    $sha256 = (Get-FileHash -Algorithm SHA256 $targetApk).Hash
    $built  = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")

    $guardLog = Join-Path $OutDir ("guard-{0}.txt" -f $suffix)
    try {
        # What the compiler actually used for this edition and variant. Both parts of the path
        # matter: a stale BuildConfig from an earlier build sits beside this one, and matching
        # on the variant alone would happily read the wrong edition's.
        $buildConfigFile = (Get-ChildItem -Path (Join-Path $repositoryRoot "app-civion\build\generated\source\buildConfig") `
                -Filter "BuildConfig.java" -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match "\\$Edition\\$Variant\\" } |
            Select-Object -First 1)
        if (-not $buildConfigFile) {
            throw "Generated BuildConfig for the $Edition $Variant variant was not found; the build identity and OAuth client id cannot be verified."
        }
        $buildConfigFile = $buildConfigFile.FullName

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
            $found = (Select-String -Path $buildConfigFile -Pattern ([regex]::Escape($oauthClientId)) -Quiet) -eq $true
            if (-not $found) {
                throw "The OAuth client id is not compiled into the $Edition $Variant build. The application would offer no OAuth provider and Gmail would fall back to password authentication."
            }

            Write-Output ""
            Write-Output "OAuth identity verified: nl.civion.mobile.debug / $expectedSigningSha1"
        }

        # ------------------------------------------------ verify build identity ---

        # The name on the file and the versionCode inside it are computed twice - here from
        # git, and by Gradle - so they are compared rather than assumed. Two editions share
        # one application id, and versionCode is what decides whether one installs over the
        # other, so an artifact whose name and versionCode disagree is a trap.
        $versionCodeMatch = [regex]::Match(
            (Get-Content $buildConfigFile -Raw),
            'VERSION_CODE\s*=\s*(\d+)')
        if (-not $versionCodeMatch.Success) {
            throw "Could not read VERSION_CODE from $buildConfigFile; the build identity cannot be verified."
        }
        $actualVersionCode = [int]$versionCodeMatch.Groups[1].Value
        if ($actualVersionCode -ne $buildNumber) {
            throw ("Build identity disagrees: the artifact is named for build {0}, but versionCode is {1}. The -Pcivion.build.number handed to Gradle did not reach the build." -f $buildNumber, $actualVersionCode)
        }

        # ----------------------------------------------------- verify edition ---

        # What actually separates the editions is which code was compiled in, so that is what
        # is checked - in the built artifact, not in the build files that were meant to produce
        # it. Runtime flags, Koin bindings and the shrinker are all irrelevant here: the
        # question is whether the classes are in the dex.
        #
        # The check is two-sided on purpose. Asking only whether the integration is absent from
        # a standalone APK passes just as happily when the tool read nothing at all - a wrong
        # path, an unreadable dex, a silently failing apkanalyzer. Requiring the edition's own
        # marker package to be present as well means a vacuous pass is impossible.
        $editionPackages = @{
            standalone = @{ present = "nl.civion.mobile.integration.noop"; absent = "nl.civion.mobile.integration.impl" }
            integrated = @{ present = "nl.civion.mobile.integration.impl"; absent = "nl.civion.mobile.integration.noop" }
        }
        $expectPresent = $editionPackages[$Edition].present
        $expectAbsent  = $editionPackages[$Edition].absent

        $apkanalyzer = Get-ChildItem (Join-Path $AndroidSdk "cmdline-tools") -Filter "apkanalyzer.bat" -Recurse -File -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if (-not $apkanalyzer) {
            throw "apkanalyzer was not found under $AndroidSdk\cmdline-tools. It is what proves the standalone artifact carries no integration code; install the Android SDK command-line tools."
        }

        $dexPackages = & cmd.exe /d /c "`"$($apkanalyzer.FullName)`" dex packages --defined-only `"$targetApk`" 2>&1"
        if ($LASTEXITCODE -ne 0) {
            throw "apkanalyzer could not read $targetName; the edition boundary cannot be verified.`n$($dexPackages -join "`n")"
        }

        $presentCount = @($dexPackages | Where-Object { $_ -match [regex]::Escape($expectPresent) }).Count
        $absentCount  = @($dexPackages | Where-Object { $_ -match [regex]::Escape($expectAbsent) }).Count

        if ($presentCount -eq 0) {
            throw ("The {0} artifact carries nothing from {1}. Either the wrong artifact was inspected or the edition wiring is gone; in both cases the absence check below would have passed for the wrong reason." -f $Edition, $expectPresent)
        }
        if ($absentCount -ne 0) {
            throw ("The {0} artifact contains {1} classes ({2} dex entries). Edition separation is a compile-time dependency boundary; something has made the other edition's code reachable from this one." -f $Edition, $expectAbsent, $absentCount)
        }

        Write-Output "Edition verified: $Edition carries $expectPresent ($presentCount dex entries) and no $expectAbsent"

        "guard: passed" | Set-Content -Path $guardLog -Encoding UTF8
    }
    catch {
        @("guard: FAILED", $_.Exception.Message, $_.ScriptStackTrace) | Set-Content -Path $guardLog -Encoding UTF8
        throw
    }

    $infoPath = Join-Path $OutDir ("BUILD-INFO-{0}.txt" -f $suffix)
    @(
        "artifact : $targetName"
        "edition  : $Edition (verified against the built dex)"
        "variant  : $Variant"
        "version  : $Version"
        "build    : $buildNumber (commits reachable from HEAD; also the Android versionCode)"
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

    # The ledger lives in _ledger\ under the output directory, one file per edition. Beside the
    # APKs it was one more file among fifty; and a single file could not hold both editions
    # without either mixing rows a reader has to filter or, worse, appending rows of this
    # header to a ledger written with an older one.
    $ledgerDir = Join-Path $OutDir "_ledger"
    New-Item -ItemType Directory -Force -Path $ledgerDir | Out-Null
    $ledger = Join-Path $ledgerDir ("build-history-{0}.csv" -f $Edition)
    if (-not (Test-Path $ledger)) {
        "timestamp,commit,branch,edition,variant,version,build,dirty,builder,sha256,artifact" | Set-Content -Path $ledger -Encoding UTF8
    }
    ("{0},{1},{2},{3},{4},{5},{6},{7},{8},{9},{10}" -f $built, $commit, $branch, $Edition, $Variant, $Version, $buildNumber, $isDirty, $(if ($isCi) { "ci" } else { "manual" }), $sha256, $targetName) |
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
