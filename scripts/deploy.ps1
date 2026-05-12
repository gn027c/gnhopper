# ══════════════════════════════════════════════════════════
# 🛠️ GNHOPPER AUTO BUILD & DEPLOY
# ══════════════════════════════════════════════════════════

$tempGradleHome = "C:\Users\lenovo\AppData\Local\Temp\gradle-home"
$tempProjectCache = "C:\Users\lenovo\AppData\Local\Temp\gradle-project-cache"

Write-Host ">>> [1/2] Dang bat dau Build du an gnhopper..." -ForegroundColor Cyan

# Chay Gradle Build
.\gradlew.bat clean shadowJar -g $tempGradleHome --project-cache-dir $tempProjectCache

if ($LASTEXITCODE -eq 0) {
    Write-Host "OK: Build thanh cong!" -ForegroundColor Green
    Write-Host ">>> [2/2] Dang upload len server..." -ForegroundColor Cyan
    
    # Chay Python Deploy
    python .\scripts\deploy_plugin.py
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "DONE: Plugin da duoc day len server!" -ForegroundColor Magenta
    } else {
        Write-Host "ERROR: Qua trinh upload that bai." -ForegroundColor Red
    }
} else {
    Write-Host "ERROR: Build that bai. Vui long kiem tra lai code." -ForegroundColor Red
}
