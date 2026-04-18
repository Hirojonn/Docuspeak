$ErrorActionPreference = "Stop"
$ProgressPreference = 'SilentlyContinue'

$workspace = (Get-Location).Path
$toolsDir = Join-Path $workspace ".build_tools"

if (-not (Test-Path $toolsDir)) { New-Item -ItemType Directory -Path $toolsDir | Out-Null }

Write-Host "Downloading OpenJDK 17..."
$jdkUrl = "https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip"
$jdkZip = Join-Path $toolsDir "jdk.zip"
$jdkDir = Join-Path $toolsDir "jdk"
if (-not (Test-Path $jdkZip)) { Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkZip }
if (-not (Test-Path $jdkDir)) { Write-Host "Extracting JDK..."; Expand-Archive -Path $jdkZip -DestinationPath $jdkDir }

Write-Host "Downloading Android Command Line Tools..."
$cmdToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$cmdToolsZip = Join-Path $toolsDir "cmdline-tools.zip"
$androidHome = Join-Path $toolsDir "android_sdk"
if (-not (Test-Path $cmdToolsZip)) { Invoke-WebRequest -Uri $cmdToolsUrl -OutFile $cmdToolsZip }
if (-not (Test-Path $androidHome)) { 
    Write-Host "Extracting Command Line Tools..."
    Expand-Archive -Path $cmdToolsZip -DestinationPath $androidHome 
    
    # Fix SDK manager structure: android_sdk/cmdline-tools/latest/bin 
    $tempCmdToolsPath = Join-Path $androidHome "cmdline-tools"
    $targetCmdToolsPath = Join-Path $androidHome "cmdline-tools\latest"
    Rename-Item -Path $tempCmdToolsPath -NewName "latest_temp"
    New-Item -ItemType Directory -Path $tempCmdToolsPath | Out-Null
    Move-Item -Path (Join-Path $androidHome "latest_temp") -Destination $targetCmdToolsPath
}

Write-Host "Downloading Gradle 8.5..."
$gradleUrl = "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
$gradleZip = Join-Path $toolsDir "gradle.zip"
$gradleDir = Join-Path $toolsDir "gradle"
if (-not (Test-Path $gradleZip)) { Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip }
if (-not (Test-Path $gradleDir)) { Write-Host "Extracting Gradle..."; Expand-Archive -Path $gradleZip -DestinationPath $gradleDir }

# Setup Environments
Write-Host "Setting Environment Variables..."
$env:JAVA_HOME = Join-Path $jdkDir "jdk-17.0.2"
$env:ANDROID_HOME = $androidHome
$gradleBin = Join-Path $gradleDir "gradle-8.5\bin"
$env:PATH = "$env:JAVA_HOME\bin;$gradleBin;$env:PATH"

# Accept Licenses and Download Platforms
Write-Host "Accepting Android SDK Licenses (this may take a moment)..."
$sdkManager = Join-Path $androidHome "cmdline-tools\latest\bin\sdkmanager.bat"

# Auto accept all licenses via a file
$yesFile = Join-Path $toolsDir "yes.txt"
"y`n" * 100 | Set-Content $yesFile
cmd.exe /c " ""$sdkManager"" --licenses < ""$yesFile"" " | Out-Null

Write-Host "Installing Android Platforms..."
cmd.exe /c " ""$sdkManager"" ""platforms;android-34"" ""build-tools;34.0.0"" ""platform-tools"" " | Out-Null

Write-Host "Compiling APK via Gradle..."
Set-Location $workspace
cmd.exe /c " gradle assembleDebug "

# Wait for completion checking APK
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    Write-Host "============================"
    Write-Host "APK built exactly at: $apkPath"
    Write-Host "Installing APK onto connected device..."
    $adb = "C:\platform-tools\adb.exe"
    & $adb install -r $apkPath
    Write-Host "Launching App..."
    & $adb shell monkey -p com.example.docuspeak -c android.intent.category.LAUNCHER 1
    Write-Host "============================"
    Write-Host "SUCCESS! The app is successfully installed and running on your device."
} else {
    Write-Host "Failed to find the built APK."
}
