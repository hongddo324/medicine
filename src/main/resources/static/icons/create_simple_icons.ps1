# PowerShell 스크립트로 간단한 PNG 아이콘 생성
# 1x1 픽셀 투명 PNG의 base64 데이터
$base64PngData = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="

$sizes = @(72, 96, 128, 144, 152, 192, 384, 512)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "🎨 임시 아이콘 파일 생성 시작..." -ForegroundColor Cyan

foreach ($size in $sizes) {
    $filename = "icon-$size`x$size.png"
    $filepath = Join-Path $scriptDir $filename

    # Base64를 바이트로 변환하여 파일로 저장
    $bytes = [Convert]::FromBase64String($base64PngData)
    [System.IO.File]::WriteAllBytes($filepath, $bytes)

    Write-Host "✅ $filename 생성 완료" -ForegroundColor Green
}

Write-Host "`n🎉 모든 임시 아이콘 생성 완료!" -ForegroundColor Green
Write-Host "📁 저장 위치: $scriptDir" -ForegroundColor Yellow
Write-Host "`n⚠️  주의: 이것은 임시 1x1 픽셀 플레이스홀더입니다." -ForegroundColor Yellow
Write-Host "실제 아이콘을 생성하려면:" -ForegroundColor Yellow
Write-Host "  1. 브라우저에서 create_icons.html 파일을 열어주세요" -ForegroundColor White
Write-Host "  2. 또는 generate_icons.py (Python) 실행" -ForegroundColor White
Write-Host "  3. 또는 온라인 도구: https://www.pwabuilder.com/imageGenerator" -ForegroundColor White
