Write-Host "Stopping services on ports 7979, 7980, 8000, and 5173..." -ForegroundColor Cyan

$ports = @(7979, 7980, 8000, 5173)
foreach ($port in $ports) {
    $procId = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess
    if ($procId) {
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped process $procId on port $port" -ForegroundColor Green
    } else {
        Write-Host "No process listening on port $port" -ForegroundColor Yellow
    }
}
