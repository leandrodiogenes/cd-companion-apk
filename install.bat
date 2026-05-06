@echo off
set APK=app\release\app-release.apk

adb devices | findstr /v "List" | findstr "device" >nul 2>&1
if errorlevel 1 (
    echo No device connected.
    pause
    exit /b 1
)

echo Installing %APK%...
adb install -r %APK%
if errorlevel 1 (
    echo Install failed.
    pause
    exit /b 1
)

echo Launching app...
adb shell monkey -p io.github.leandrodiogenes.cdcompanion -c android.intent.category.LAUNCHER 1 >nul 2>&1

echo Done.
pause
