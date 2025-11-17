# Simple PNG icon creator
$pngData = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
$sizes = @(72, 96, 128, 144, 152, 192, 384, 512)
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path

foreach ($size in $sizes) {
    $file = Join-Path $dir "icon-$size`x$size.png"
    $bytes = [Convert]::FromBase64String($pngData)
    [IO.File]::WriteAllBytes($file, $bytes)
    Write-Host "Created: icon-$size`x$size.png"
}

Write-Host "Done! Created 8 placeholder icons."
Write-Host "Open create_icons.html in browser for real icons."
