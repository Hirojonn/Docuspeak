Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile("$PWD\docuspeak.png")

function Resize-Icon($path, $size) {
    $dir = Split-Path $path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $graph = [System.Drawing.Graphics]::FromImage($bmp)
    $graph.DrawImage($img, 0, 0, $size, $size)
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graph.Dispose()
    $bmp.Dispose()
}

Resize-Icon -path "$PWD\app\src\main\res\mipmap-mdpi\ic_launcher.png" -size 48
Resize-Icon -path "$PWD\app\src\main\res\mipmap-hdpi\ic_launcher.png" -size 72
Resize-Icon -path "$PWD\app\src\main\res\mipmap-xhdpi\ic_launcher.png" -size 96
Resize-Icon -path "$PWD\app\src\main\res\mipmap-xxhdpi\ic_launcher.png" -size 144
Resize-Icon -path "$PWD\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" -size 192

Resize-Icon -path "$PWD\app\src\main\res\mipmap-mdpi\ic_launcher_round.png" -size 48
Resize-Icon -path "$PWD\app\src\main\res\mipmap-hdpi\ic_launcher_round.png" -size 72
Resize-Icon -path "$PWD\app\src\main\res\mipmap-xhdpi\ic_launcher_round.png" -size 96
Resize-Icon -path "$PWD\app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png" -size 144
Resize-Icon -path "$PWD\app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png" -size 192

$img.Dispose()
