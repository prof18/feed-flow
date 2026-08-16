<#
.SYNOPSIS
  Locates tools that ship with the Windows SDK.

.DESCRIPTION
  Dot-source this from a script that needs makeappx.exe or another SDK tool:

    . (Join-Path $PSScriptRoot "lib\windows-sdk.ps1")
#>

function Get-WindowsSdkTool {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$ToolName,
        [string]$Architecture = "x64"
    )

    # Collect every installed candidate, then take the highest SDK version.
    # Sorting the paths as strings would be wrong twice over: it compares
    # version directories lexically, and it lets the kit root outweigh the
    # version, so an old SDK under Program Files could beat a new one under
    # Program Files (x86). Version directories are "10.0.26100.0"; the
    # unversioned bin\<arch> is an older SDK layout, kept as a last resort.
    $kitRoots = @(
        "${env:ProgramFiles(x86)}\Windows Kits\10",
        "${env:ProgramFiles(x86)}\Windows Kits\11",
        "${env:ProgramFiles}\Windows Kits\10",
        "${env:ProgramFiles}\Windows Kits\11"
    ) | Where-Object { $_ -and (Test-Path $_) }

    $candidates = foreach ($root in $kitRoots) {
        $binDir = Join-Path $root "bin"

        foreach ($versionDir in (Get-ChildItem -Path $binDir -Directory -ErrorAction SilentlyContinue)) {
            $exe = Join-Path $versionDir.FullName "$Architecture\$ToolName"
            if ($versionDir.Name -match '^\d+(\.\d+){1,3}$' -and (Test-Path $exe)) {
                [pscustomobject]@{ Version = [version]$versionDir.Name; Path = $exe }
            }
        }

        $legacyExe = Join-Path $binDir "$Architecture\$ToolName"
        if (Test-Path $legacyExe) {
            [pscustomobject]@{ Version = [version]"0.0"; Path = $legacyExe }
        }
    }

    ($candidates | Sort-Object -Property Version -Descending | Select-Object -First 1).Path
}
