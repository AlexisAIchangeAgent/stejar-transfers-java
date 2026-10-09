# setup.ps1 - install the dependencies of the project, then check that the unit tests pass.
# stejar-transfers-java. Training material. Invented bank.
#
# Run it in PowerShell 5.1 or 7, from the repository folder:
#   powershell -ExecutionPolicy Bypass -File .\setup.ps1
#
# What a developer does on a new project: Maven packages, Playwright browser, virtual environment of the cycle 6 agent,
# the superpowers plugin of Claude Code (switched off), then the unit tests.
# Run check.ps1 first: the tools must be installed.
# It needs network access. Safe to run again: it keeps what is installed.

# Tools write warnings on stderr: do not stop on them, check the exit codes instead.
$ErrorActionPreference = 'Continue'
# Work in the repository folder, wherever the script is called from.
Set-Location -LiteralPath $PSScriptRoot
# Allow TLS 1.2 for the network checks (needed by Windows PowerShell 5.1 on old settings).
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

# Print the title of a step.
function Step([string]$text) { Write-Host ''; Write-Host "==> $text" -ForegroundColor Cyan }
# Print an error and stop the script.
function Fail([string]$text) { Write-Host "FAILED: $text" -ForegroundColor Red; exit 1 }
# Run a program, and stop the script if it fails.
function Invoke-Checked([string]$exe, [string[]]$arguments) {
    & $exe @arguments
    if ($LASTEXITCODE -ne 0) { Fail "exit code $LASTEXITCODE for: $exe $($arguments -join ' ')" }
}

# Find Python: the py launcher first, then python and python3. Returns the first one
# that is at least $min, else the first one found (too old), else $null.
function Find-Python([version]$min) {
    $first = $null
    $candidates = @(
        @{ Exe = 'py'; Pre = @('-3') },
        @{ Exe = 'python'; Pre = @() },
        @{ Exe = 'python3'; Pre = @() }
    )
    foreach ($c in $candidates) {
        if (-not (Get-Command $c.Exe -ErrorAction SilentlyContinue)) { continue }
        $pre = $c.Pre
        $out = & $c.Exe @pre -c 'import sys; print(*sys.version_info[:3], sep=chr(46))' 2>$null
        if ($LASTEXITCODE -ne 0 -or -not $out) { continue }
        try { $v = [version]("$out".Trim()) } catch { continue }
        $found = @{ Exe = $c.Exe; Pre = $pre; Version = $v }
        if ($v -ge $min) { return $found }
        if (-not $first) { $first = $found }
    }
    return $first
}

# State of the superpowers plugin for your user: missing, enabled, disabled or unknown.
function Get-SuperpowersState {
    $json = (& claude plugin list --json 2>$null) | Out-String
    if ($LASTEXITCODE -ne 0) { return 'unknown' }
    try { $list = $json | ConvertFrom-Json } catch { return 'unknown' }
    $sp = @($list | Where-Object { "$($_.id)" -like 'superpowers@*' -and $_.scope -eq 'user' })
    if ($sp.Count -eq 0) { return 'missing' }
    if ($sp[0].enabled) { return 'enabled' }
    return 'disabled'
}

# Find the java that the Maven wrapper uses: JAVA_HOME first, then the PATH.
function Find-Java {
    $java = $null
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
    }
    elseif (Get-Command java -ErrorAction SilentlyContinue) { $java = (Get-Command java).Source }
    if (-not $java) { return $null }
    $text = (& $java -version 2>&1 | ForEach-Object { "$_" }) -join ' '
    $m = [regex]::Match($text, 'version "(\d+)(?:\.(\d+))?(?:\.(\d+))?')
    if (-not $m.Success) { return $null }
    $parts = @($m.Groups[1].Value, $m.Groups[2].Value, $m.Groups[3].Value) | Where-Object { $_ -ne '' }
    # "1.8.0" means Java 8.
    if ($parts[0] -eq '1' -and $parts.Count -gt 1) { $parts = @($parts | Select-Object -Skip 1) }
    if ($parts.Count -eq 1) { $parts += '0' }
    return @{ Path = $java; Version = [version]($parts -join '.') }
}

Step 'Git repository'
# The ZIP of a GitHub release has no .git folder: create the repository and its first commit.
if (Test-Path -LiteralPath (Join-Path $PSScriptRoot '.git')) { Write-Host 'Already a git repository: kept.' }
else {
    if (-not ((git config user.name) -and (git config user.email))) {
        Fail 'git needs a name and an e-mail first: git config --global user.name "Your Name" and git config --global user.email "you@example.com"'
    }
    Invoke-Checked 'git' @('init', '-q', '-b', 'main')
    Invoke-Checked 'git' @('-c', 'core.safecrlf=false', 'add', '-A')
    Invoke-Checked 'git' @('commit', '-q', '-m', 'Starter repository, training material')
    Write-Host 'Created: a git repository with one commit.'
}

Step 'JDK 17 or newer'
$java = Find-Java
if (-not $java -or $java.Version -lt [version]'17.0') { Fail 'JDK 17 or newer not found. Run check.ps1, then install-tools.ps1.' }
Write-Host "JDK $($java.Version): $($java.Path)"

Step 'Python (cycle 6 agent), 3.10 or newer'
$py = Find-Python ([version]'3.10')
if (-not $py -or $py.Version -lt [version]'3.10') { Fail 'Python 3.10 or newer not found. Run check.ps1, then install-tools.ps1.' }
$pyExe = $py.Exe
$pyPre = $py.Pre
Write-Host "Python $($py.Version)"

Step 'TEMP folder'
# Java 21 cannot create its local sockets in a TEMP folder with a short name
# (C:\Users\ALEXIS~1\...): two tests of AppConfigurationTest then fail with
# "Unable to establish loopback connection". In that case, point Java to a
# folder with a long name, for this script only.
$javaOption = $null
if ($env:TEMP -like '*~*') {
    $socketDir = Join-Path $env:PUBLIC 'stejar-java-tmp'
    New-Item -ItemType Directory -Force -Path $socketDir | Out-Null
    $javaOption = "-Djdk.net.unixdomain.tmpdir=$socketDir"
    $env:JAVA_TOOL_OPTIONS = (@($env:JAVA_TOOL_OPTIONS, $javaOption) | Where-Object { $_ }) -join ' '
    Write-Host "Short name found in TEMP ($env:TEMP): Java uses $socketDir instead."
}
else { Write-Host 'OK: nothing to change.' }

Step 'Build, and download the Maven packages (a few minutes the first time)'
Invoke-Checked '.\mvnw.cmd' @('-B', 'test-compile')

Step 'Playwright browser, used in cycle 5'
# Downloads Chromium once. Exit code 0 means it is installed. It prints no BUILD SUCCESS line: this is normal.
Invoke-Checked '.\mvnw.cmd' @('-B', 'exec:java@playwright-install')

Step 'Virtual environment of the cycle 6 agent: privacy-agent\.venv'
# Create it only if it is not there yet, then install only what is missing.
$venvPy = Join-Path $PSScriptRoot 'privacy-agent\.venv\Scripts\python.exe'
if (Test-Path -LiteralPath $venvPy) { Write-Host 'Already there: kept.' }
else { Invoke-Checked $pyExe ($pyPre + @('-m', 'venv', 'privacy-agent\.venv')) }
Invoke-Checked $venvPy @('-m', 'pip', 'install', '--disable-pip-version-check', '-r', 'privacy-agent\requirements.txt')

Step 'Plugin superpowers, used in cycle 1: installed, then switched off'
$spNote = ''
if (-not (Get-Command claude -ErrorAction SilentlyContinue)) { $spNote = 'Claude Code not found: plugin skipped. Run check.ps1.' }
else {
    # Install only when missing: installing again would switch it on.
    if ((Get-SuperpowersState) -eq 'missing') {
        & claude plugin install superpowers@claude-plugins-official
        if ($LASTEXITCODE -ne 0) {
            # The marketplace of Anthropic is out of date or not registered: update or add it, then try again.
            & claude plugin marketplace update claude-plugins-official
            if ($LASTEXITCODE -ne 0) { & claude plugin marketplace add anthropics/claude-plugins-official }
            & claude plugin install superpowers@claude-plugins-official
        }
    }
    if ((Get-SuperpowersState) -eq 'enabled') { & claude plugin disable superpowers@claude-plugins-official --scope user }
    $state = Get-SuperpowersState
    if ($state -eq 'disabled') { Write-Host 'OK: installed, switched off.' }
    else { $spNote = "superpowers is $state. P1e of cycle 1 (optional) needs it installed and switched off." }
}
if ($spNote) { Write-Host "WARNING: $spNote" -ForegroundColor Yellow }

Step 'Unit tests (expected: Tests run: 40, BUILD SUCCESS)'
Invoke-Checked '.\mvnw.cmd' @('-B', 'test')

Step 'Ready for cycle 1'
if ($spNote) { Write-Host "Except: $spNote" -ForegroundColor Yellow }
if ($javaOption) {
    Write-Host 'Your TEMP folder has a short name. In each new terminal, before mvnw.cmd or claude, type:'
    Write-Host "  `$env:JAVA_TOOL_OPTIONS = `"$javaOption`""
}
Write-Host 'Next: start Claude Code in this folder with: claude'
