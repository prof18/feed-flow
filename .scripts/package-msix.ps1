<#
.SYNOPSIS
  Packs the Store MSIX from the jpackage app image with makeappx.

.DESCRIPTION
  The MSIX used to be produced by running the MSI under the MSIX Packaging Tool,
  which requires the Msix.PackagingTool.Driver feature-on-demand installed
  through DISM. That FOD stopped installing on the hosted Windows image in
  August 2026 (`dism /add-capability` returns error 183 after ~25 minutes), and
  the packaging tool has a hard 10-minute internal timeout on its own DISM call,
  so the failure was terminal.

  Nothing is lost by skipping the MSI as an input: its payload is a verbatim
  jpackage app image under %LOCALAPPDATA%\FeedFlow, and every MSI-derived
  manifest value (Start-menu shortcut, tile logos, declared languages, target
  versions) was already being thrown away and rewritten from the checked-in
  templates immediately afterwards.

  Run .\gradlew.bat packageReleaseMsi createReleaseDistributable first.

.EXAMPLE
  .\.scripts\package-msix.ps1 -Version 1.16.0 -PublisherName "CN=Example, O=Example, C=DE"

  The resulting package is unsigned, which is what Partner Center wants — it
  signs on ingestion. To install it locally for testing you must sign it with a
  certificate whose subject matches -PublisherName exactly and trust that
  certificate; see the repo docs.
#>
[CmdletBinding()]
param(
    [string]$Version = $env:VERSION,
    [string]$PublisherName = $env:MSIX_PUBLISHER_DISPLAY_NAME,
    [string]$OutputPath = $env:RELEASE_PATH_MSIX,
    # Override the packaging tool. Normally left unset so the highest installed
    # Windows SDK is picked up; useful for a non-standard SDK location, and it
    # is how the packaging flow can be exercised off Windows.
    [string]$MakeAppxPath
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    if (-not $Version) {
        throw "No version given. Pass -Version or set the VERSION environment variable."
    }
    if (-not $PublisherName) {
        throw "No publisher given. Pass -PublisherName or set MSIX_PUBLISHER_DISPLAY_NAME. " +
              "It must be the certificate subject Partner Center has on file for this app."
    }
    if (-not $OutputPath) {
        $OutputPath = "desktopApp/build/release/main-release/msix/FeedFlow-$Version.msix"
    }

    $appImage = "desktopApp/build/release/main-release/app/FeedFlow"
    if (-not (Test-Path $appImage)) {
        Write-Host "App image not found at $appImage. Contents of the release dir:"
        Get-ChildItem "desktopApp/build/release/main-release" -Recurse -Depth 1 -ErrorAction SilentlyContinue |
            Select-Object FullName | Out-String | Write-Host
        throw "App image not found at $appImage. Run: .\gradlew.bat createReleaseDistributable"
    }

    # makeappx ships with the Windows SDK; take the highest installed version.
    if ($MakeAppxPath) {
        if (-not (Test-Path $MakeAppxPath)) {
            throw "No packaging tool at the given -MakeAppxPath: $MakeAppxPath"
        }
        $makeAppx = (Resolve-Path -LiteralPath $MakeAppxPath).Path
    }
    else {
        $makeAppx = (Get-ChildItem -Path @(
            "C:\Program Files (x86)\Windows Kits\10\bin\*\x64\makeappx.exe",
            "C:\Program Files\Windows Kits\10\bin\*\x64\makeappx.exe"
        ) -ErrorAction SilentlyContinue |
            Sort-Object -Property FullName -Descending | Select-Object -First 1).FullName

        if (-not $makeAppx) {
            throw "makeappx.exe not found. Install the Windows SDK."
        }
    }
    Write-Host "Using makeappx: $makeAppx"

    # Stage the package layout: app image at the root, icons under Assets\.
    $staging = "desktopApp/build/release/main-release/msix-staging"
    if (Test-Path $staging) { Remove-Item -Path $staging -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $staging | Out-Null
    Copy-Item -Path "$appImage\*" -Destination $staging -Recurse -Force

    $assetsDir = Join-Path $staging "Assets"
    New-Item -ItemType Directory -Force -Path $assetsDir | Out-Null
    Copy-Item -Path ".github/msix-assets/*.png" -Destination $assetsDir -Force
    Write-Host "Staged $((Get-ChildItem $assetsDir -Filter '*.png').Count) icon assets"

    if (-not (Test-Path (Join-Path $staging "FeedFlow.exe"))) {
        throw "FeedFlow.exe missing from the staged package root!"
    }

    # Render the manifest. Package versions are four-part; the app version is
    # three-part, so the revision field is always 0.
    $manifest = Get-Content -Path ".github/msix-manifest-template.xml" -Raw
    $manifest = $manifest.Replace("[AppVersion]", "$Version.0")
    $manifest = $manifest.Replace("[PublisherName]", $PublisherName)

    # msix-resources-template.xml stays the single source of truth for the
    # declared package languages, so it is spliced in rather than duplicated
    # into the manifest template.
    [xml]$resourcesTemplate = Get-Content -Path ".github/msix-resources-template.xml"
    $resourceLines = $resourcesTemplate.Resources.Resource |
        ForEach-Object { "    <Resource Language=`"$($_.Language)`" />" }
    $resourcesXml = "  <Resources>`n" + ($resourceLines -join "`n") + "`n  </Resources>"
    $manifest = $manifest.Replace("  <!--[Resources]-->", $resourcesXml)

    if ($manifest -match "\[AppVersion\]|\[PublisherName\]|\[Resources\]") {
        throw "Manifest template still contains unsubstituted placeholders!"
    }

    # UTF8 without BOM: makeappx rejects a BOM ahead of the XML declaration.
    $manifestPath = Join-Path (Resolve-Path -LiteralPath $staging).Path "AppxManifest.xml"
    [System.IO.File]::WriteAllText($manifestPath, $manifest, (New-Object System.Text.UTF8Encoding $false))

    Write-Host "`nManifest:"
    Get-Content $manifestPath | Write-Host

    New-Item -ItemType Directory -Force -Path (Split-Path $OutputPath -Parent) | Out-Null

    Write-Host "`nPacking MSIX..."
    & $makeAppx pack /d $staging /p $OutputPath /o
    if ($LASTEXITCODE -ne 0) {
        throw "makeappx pack failed with exit code $LASTEXITCODE"
    }

    Remove-Item -Path $staging -Recurse -Force

    $size = (Get-Item $OutputPath).Length
    Write-Host "`nCreated $OutputPath ($([math]::Round($size / 1MB, 1)) MB)"
}
finally {
    Pop-Location
}
