<#
.SYNOPSIS
  Registers the built MSIX locally, under a separate identity, for testing.

.DESCRIPTION
  Unpacks the MSIX that .scripts\package-msix.ps1 produced and rewrites only
  the package identity and the two display names before registering it. The
  payload, capabilities, tile assets and declared languages are exactly what
  would ship, so this exercises the real package rather than a rebuild of it.

  The rename is the point. The shipping Identity/Name is the same one the
  Store package uses, so registering the build as-is makes Windows treat it as
  an update to an installed FeedFlow and replace it — along with the app data
  behind it. A distinct name gives the dev build its own identity, its own app
  data and its own Start-menu entry, and leaves a Store install untouched.

  Registration is loose-file: the package runs from the unpacked folder under
  desktopApp\build, so a gradle clean or a rebuild breaks it. Re-run this
  script after rebuilding; it re-registers cleanly over itself.

  Requires Developer Mode (Settings > System > For developers), which is what
  lets Windows register an unsigned package. Nothing here signs anything — the
  Store signs on ingestion, and the shipped package stays unsigned.

.EXAMPLE
  .\.scripts\install-msix-dev.ps1

  Registers the newest built MSIX and launches it.

.EXAMPLE
  .\.scripts\install-msix-dev.ps1 -Version 1.16.0 -NoLaunch

.EXAMPLE
  .\.scripts\install-msix-dev.ps1 -Uninstall

  Removes the dev registration. The Store install is never touched.
#>
[CmdletBinding()]
param(
    [string]$Version,
    [string]$MsixPath,
    [string]$IdentityName = "MarcoGomiero.FeedFlowRSSReaderDev",
    [string]$DisplayName = "FeedFlow (Dev)",
    [switch]$Uninstall,
    [switch]$NoLaunch
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "lib\windows-sdk.ps1")

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    $msixDir = "desktopApp/build/release/main-release/msix"
    $unpackDir = "desktopApp/build/release/main-release/msix-dev"

    # Stop anything still running out of the unpacked folder, otherwise the
    # files are locked and both unpack and re-register fail.
    function Stop-DevApp {
        $full = if (Test-Path $unpackDir) { (Resolve-Path -LiteralPath $unpackDir).Path } else { $null }
        if (-not $full) { return }

        Get-Process -Name "FeedFlow" -ErrorAction SilentlyContinue |
            Where-Object { $_.Path -and $_.Path.StartsWith($full, [StringComparison]::OrdinalIgnoreCase) } |
            ForEach-Object {
                Write-Host "Stopping running dev app (PID $($_.Id))"
                Stop-Process -Id $_.Id -Force
            }
    }

    function Remove-DevPackage {
        $existing = Get-AppxPackage -Name $IdentityName -ErrorAction SilentlyContinue
        foreach ($package in $existing) {
            Write-Host "Removing $($package.PackageFullName)"
            Remove-AppxPackage -Package $package.PackageFullName
        }
    }

    if ($Uninstall) {
        Stop-DevApp
        Remove-DevPackage
        if (Test-Path $unpackDir) { Remove-Item -Path $unpackDir -Recurse -Force }
        Write-Host "Dev package removed."
        return
    }

    $devMode = (Get-ItemProperty `
            -Path "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" `
            -Name "AllowDevelopmentWithoutDevLicense" `
            -ErrorAction SilentlyContinue).AllowDevelopmentWithoutDevLicense

    if ($devMode -ne 1) {
        throw "Developer Mode is off, so Windows will refuse an unsigned package. " +
              "Turn it on in Settings > System > For developers, then re-run."
    }

    if (-not $MsixPath) {
        if ($Version) {
            $MsixPath = Join-Path $msixDir "FeedFlow-$Version.msix"
        }
        else {
            $MsixPath = (Get-ChildItem -Path $msixDir -Filter "*.msix" -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
        }
    }

    if (-not $MsixPath -or -not (Test-Path $MsixPath)) {
        throw "No MSIX found. Run .\.scripts\package-msix.ps1 first."
    }

    $MsixPath = (Resolve-Path -LiteralPath $MsixPath).Path
    Write-Host "Using package: $MsixPath"

    $makeAppx = Get-WindowsSdkTool -ToolName "makeappx.exe"
    if (-not $makeAppx) {
        throw "makeappx.exe not found. Install the Windows SDK."
    }

    Stop-DevApp
    Remove-DevPackage

    if (Test-Path $unpackDir) { Remove-Item -Path $unpackDir -Recurse -Force }

    Write-Host "Unpacking..."
    & $makeAppx unpack /p $MsixPath /d $unpackDir /o | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "makeappx unpack failed with exit code $LASTEXITCODE"
    }

    # Rewrite identity and display names. Done through the XML rather than a
    # string replace: "FeedFlow" appears in several attributes that must not
    # change, and DisplayName exists both in Properties and VisualElements.
    $manifestPath = Join-Path (Resolve-Path -LiteralPath $unpackDir).Path "AppxManifest.xml"

    $xml = New-Object System.Xml.XmlDocument
    $xml.PreserveWhitespace = $true
    $xml.Load($manifestPath)

    $ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
    $ns.AddNamespace("d", "http://schemas.microsoft.com/appx/manifest/foundation/windows10")
    $ns.AddNamespace("uap", "http://schemas.microsoft.com/appx/manifest/uap/windows10")

    $xml.SelectSingleNode("/d:Package/d:Identity", $ns).SetAttribute("Name", $IdentityName)
    $xml.SelectSingleNode("/d:Package/d:Properties/d:DisplayName", $ns).InnerText = $DisplayName
    $xml.SelectSingleNode("/d:Package/d:Applications/d:Application/uap:VisualElements", $ns).
        SetAttribute("DisplayName", $DisplayName)

    # UTF8 without BOM, same as the packing script writes it.
    [System.IO.File]::WriteAllText($manifestPath, $xml.OuterXml, (New-Object System.Text.UTF8Encoding $false))

    Write-Host "Registering as $IdentityName ($DisplayName)..."
    Add-AppxPackage -Register $manifestPath

    $package = Get-AppxPackage -Name $IdentityName
    $appId = ([xml](Get-Content $manifestPath)).Package.Applications.Application.Id
    $aumid = "$($package.PackageFamilyName)!$appId"

    Write-Host ""
    Write-Host "Registered $($package.PackageFullName)"
    Write-Host "Start-menu entry: $DisplayName"
    Write-Host "AppUserModelId:   $aumid"
    Write-Host ""
    Write-Host "Runs from $unpackDir, so rebuilding breaks it - re-run this script."
    Write-Host "Remove with: .\.scripts\install-msix-dev.ps1 -Uninstall"

    if (-not $NoLaunch) {
        Write-Host ""
        Write-Host "Launching..."
        explorer.exe "shell:AppsFolder\$aumid"
    }
}
finally {
    Pop-Location
}
