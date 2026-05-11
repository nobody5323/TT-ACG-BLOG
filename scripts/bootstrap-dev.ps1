param(
    [string]$ProjectRoot = "C:\Users\970892102\Desktop\ACG-BLOG",
    [switch]$SkipFrameworkInstall,
    [switch]$SkipWebBuild
)

$ErrorActionPreference = "Stop"

function Write-Step($text) {
    Write-Host ""
    Write-Host "==> $text" -ForegroundColor Cyan
}

function Write-Ok($text) {
    Write-Host "[OK] $text" -ForegroundColor Green
}

function Write-Fail($text) {
    Write-Host "[FAIL] $text" -ForegroundColor Red
}

function Get-CommandPath($commandName) {
    $cmd = Get-Command $commandName -ErrorAction SilentlyContinue
    if (-not $cmd) {
        return $null
    }
    return $cmd.Source
}

function Run-InDirectory($directory, $scriptBlock) {
    Push-Location $directory
    try {
        $global:LASTEXITCODE = 0
        & $scriptBlock | Out-Host
        if ($null -ne $LASTEXITCODE) {
            return [int]$LASTEXITCODE
        }
        if ($?) {
            return 0
        }
        return 1
    } finally {
        Pop-Location
    }
}

function Wait-Port($port, $timeoutSec = 90, $process = $null) {
    $start = Get-Date
    while (((Get-Date) - $start).TotalSeconds -lt $timeoutSec) {
        if ($process -and $process.HasExited) {
            return $null
        }
        $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($conn) {
            return $conn
        }
        Start-Sleep -Seconds 1
    }
    return $null
}

function Invoke-HealthJson($name, $url, $method = "GET", $body = $null, $headers = $null, $timeoutSec = 10) {
    try {
        if ($body) {
            $resp = Invoke-RestMethod -Uri $url -Method $method -ContentType "application/json" -Body $body -Headers $headers -TimeoutSec $timeoutSec
        } else {
            $resp = Invoke-RestMethod -Uri $url -Method $method -Headers $headers -TimeoutSec $timeoutSec
        }
        Write-Ok $name
        return $resp
    } catch {
        Write-Fail $name
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host ($reader.ReadToEnd())
        } else {
            Write-Host $_.Exception.Message
        }
        return $null
    }
}

function Invoke-HealthText($name, $url, $timeoutSec = 10) {
    try {
        $resp = Invoke-WebRequest -Uri $url -TimeoutSec $timeoutSec
        Write-Ok "$name ($($resp.StatusCode))"
        return $resp
    } catch {
        Write-Fail $name
        if ($_.Exception.Response) {
            Write-Host ("HTTP " + [int]$_.Exception.Response.StatusCode)
        } else {
            Write-Host $_.Exception.Message
        }
        return $null
    }
}

function Wait-HealthJson($name, $url, $timeoutSec = 120, $method = "GET", $body = $null, $headers = $null) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $resp = Invoke-HealthJson $name $url $method $body $headers 5
        if ($resp) {
            return $resp
        }
        Start-Sleep -Seconds 2
    }
    return $null
}

function Wait-HealthText($name, $url, $timeoutSec = 120) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $resp = Invoke-HealthText $name $url 5
        if ($resp) {
            return $resp
        }
        Start-Sleep -Seconds 2
    }
    return $null
}

function Stop-PortProcess($port) {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $conn) {
        Write-Host "No process listening on port $port" -ForegroundColor Yellow
        return
    }

    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    Write-Host "Stopped process $($conn.OwningProcess) on port $port" -ForegroundColor Green
}

$mvn = "C:\Users\970892102\scoop\shims\mvn.cmd"
if (-not (Test-Path $mvn)) {
    $mvn = Get-CommandPath "mvn"
}
if (-not $mvn) {
    throw "Maven not found. Expected scoop shim or PATH entry for mvn."
}

$npm = Get-CommandPath "npm.cmd"
if (-not $npm) {
    $npm = Get-CommandPath "npm"
}
if (-not $npm) {
    throw "npm not found in PATH."
}

$systemPython = Get-CommandPath "python"
if (-not $systemPython) {
    throw "Python not found. Expected AI\\.venv\\Scripts\\python.exe or PATH entry for python."
}

$python = Join-Path $ProjectRoot "AI\.venv\Scripts\python.exe"
$venvExists = Test-Path $python
if (-not $venvExists) {
    $python = $systemPython
}

$pip = Join-Path (Split-Path $python -Parent) "pip.exe"
if (-not (Test-Path $pip)) {
    $pip = $null
}

$frameworkDir = Join-Path $ProjectRoot "framework"
$blogworkDir = Join-Path $ProjectRoot "blogwork"
$adminworkDir = Join-Path $ProjectRoot "adminwork"
$webDir = Join-Path $ProjectRoot "web"
$aiDir = Join-Path $ProjectRoot "AI"

$logDir = Join-Path $ProjectRoot "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$blogOut = Join-Path $logDir "blogwork.out.log"
$blogErr = Join-Path $logDir "blogwork.err.log"
$adminOut = Join-Path $logDir "adminwork.out.log"
$adminErr = Join-Path $logDir "adminwork.err.log"
$webOut = Join-Path $logDir "web.out.log"
$webErr = Join-Path $logDir "web.err.log"
$aiOut = Join-Path $logDir "ai.out.log"
$aiErr = Join-Path $logDir "ai.err.log"

Write-Step "Stopping existing services on ports 7979/7980/8000/5173"
foreach ($port in @(7979, 7980, 8000, 5173)) {
    Stop-PortProcess $port
}

if (-not $SkipFrameworkInstall) {
    Write-Step "Installing framework to local Maven repo"
    & $mvn "clean" "-DskipTests" "install" "--file" (Join-Path $frameworkDir "pom.xml")
}

if (-not (Test-Path (Join-Path $webDir "node_modules"))) {
    Write-Step "Installing web dependencies"
    $exitCode = Run-InDirectory $webDir { & $npm "install" }
    if ($exitCode -ne 0) {
        throw "web dependency install failed."
    }
}

if (-not $SkipWebBuild) {
    Write-Step "Building web frontend"
    $exitCode = Run-InDirectory $webDir { & $npm "run" "build" }
    if ($exitCode -ne 0) {
        throw "web build failed."
    }
}

if (-not $venvExists) {
    Write-Step "Creating AI virtual environment"
    $global:LASTEXITCODE = 0
    & $systemPython "-m" "venv" (Join-Path $aiDir ".venv")
    if ($LASTEXITCODE -ne 0) {
        throw "AI virtual environment creation failed."
    }
    $python = Join-Path $ProjectRoot "AI\.venv\Scripts\python.exe"
    $pip = Join-Path (Split-Path $python -Parent) "pip.exe"
}

if ($pip -and (Test-Path $pip)) {
    Write-Step "Ensuring AI runtime dependencies"
    $exitCode = Run-InDirectory $aiDir { & $pip "install" "-e" "." }
    if ($exitCode -ne 0) {
        throw "AI dependency install failed."
    }
} else {
    throw "pip not found for AI runtime."
}

Write-Step "Starting blogwork (port 7979)"
if (Test-Path $blogOut) { Remove-Item $blogOut -Force }
if (Test-Path $blogErr) { Remove-Item $blogErr -Force }
$blogProc = Start-Process -FilePath $mvn -ArgumentList "spring-boot:run" -WorkingDirectory $blogworkDir -RedirectStandardOutput $blogOut -RedirectStandardError $blogErr -PassThru

Write-Step "Starting adminwork (port 7980)"
if (Test-Path $adminOut) { Remove-Item $adminOut -Force }
if (Test-Path $adminErr) { Remove-Item $adminErr -Force }
$adminProc = Start-Process -FilePath $mvn -ArgumentList "spring-boot:run" -WorkingDirectory $adminworkDir -RedirectStandardOutput $adminOut -RedirectStandardError $adminErr -PassThru

Write-Step "Starting AI service (port 8000)"
if (Test-Path $aiOut) { Remove-Item $aiOut -Force }
if (Test-Path $aiErr) { Remove-Item $aiErr -Force }
$aiProc = Start-Process -FilePath $python -ArgumentList "-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", "8000" -WorkingDirectory $aiDir -RedirectStandardOutput $aiOut -RedirectStandardError $aiErr -PassThru

Write-Step "Starting web dev server (port 5173)"
if (Test-Path $webOut) { Remove-Item $webOut -Force }
if (Test-Path $webErr) { Remove-Item $webErr -Force }
$webProc = Start-Process -FilePath $npm -ArgumentList "run", "dev", "--", "--host", "127.0.0.1", "--port", "5173" -WorkingDirectory $webDir -RedirectStandardOutput $webOut -RedirectStandardError $webErr -PassThru

Write-Step "Waiting for ports"
$blogConn = Wait-Port 7979 120
$adminConn = Wait-Port 7980 120
$aiConn = Wait-Port 8000 120 $aiProc
$webConn = Wait-Port 5173 120 $webProc

if (-not $blogConn) { Write-Fail "blogwork not ready on 7979. See $blogOut / $blogErr" }
if (-not $adminConn) { Write-Fail "adminwork not ready on 7980. See $adminOut / $adminErr" }
if (-not $aiConn) { Write-Fail "AI not ready on 8000. See $aiOut / $aiErr" }
if (-not $webConn) { Write-Fail "web not ready on 5173. See $webOut / $webErr" }

if (-not $blogConn -or -not $adminConn -or -not $aiConn -or -not $webConn) {
    throw "One or more services failed to bind their ports."
}

Write-Step "Health checks"
$blogHome = Wait-HealthJson "Blog /api/content/home" "http://127.0.0.1:7979/api/content/home" 60
$adminLoginBody = @{ nickname = "nobody"; password = "zhai00031311" } | ConvertTo-Json
$adminLogin = Wait-HealthJson "Admin /api/admin/auth/login" "http://127.0.0.1:7980/api/admin/auth/login" 60 "POST" $adminLoginBody
$aiHealth = Wait-HealthJson "AI /health" "http://127.0.0.1:8000/health" 60
$webHealth = Wait-HealthText "Web /" "http://127.0.0.1:5173" 60

if ($adminLogin -and $adminLogin.data -and $adminLogin.data.token) {
    $headers = @{ Authorization = "Bearer $($adminLogin.data.token)" }
    [void](Invoke-HealthJson "Admin /api/admin/auth/me" "http://127.0.0.1:7980/api/admin/auth/me" "GET" $null $headers)
    [void](Invoke-HealthJson "Admin /api/admin/posts" "http://127.0.0.1:7980/api/admin/posts?pageNum=1&pageSize=5" "GET" $null $headers)
    [void](Invoke-HealthJson "Admin /api/admin/users" "http://127.0.0.1:7980/api/admin/users?pageNum=1&pageSize=5" "GET" $null $headers)
}

Write-Step "Done"
Write-Host "blogwork PID: $($blogProc.Id)  log: $blogOut" -ForegroundColor Yellow
Write-Host "adminwork PID: $($adminProc.Id) log: $adminOut" -ForegroundColor Yellow
Write-Host "AI PID: $($aiProc.Id)        log: $aiOut" -ForegroundColor Yellow
Write-Host "web PID: $($webProc.Id)      log: $webOut" -ForegroundColor Yellow
Write-Host "Open: http://127.0.0.1:5173" -ForegroundColor Yellow
Write-Host "Use scripts\\stop-dev.ps1 to stop all dev services." -ForegroundColor DarkYellow
