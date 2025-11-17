#!/usr/bin/env node
/**
 * PWA 알약 아이콘 생성 스크립트 (Node.js)
 * 필요한 패키지: npm install canvas
 *
 * 사용법:
 * 1. npm install canvas
 * 2. node generate_icons.js
 */

const fs = require('fs');
const path = require('path');

// Canvas 라이브러리가 설치되어 있는지 확인
let Canvas;
try {
    Canvas = require('canvas');
} catch (err) {
    console.error('❌ canvas 패키지가 설치되어 있지 않습니다.');
    console.error('다음 명령어로 설치하세요: npm install canvas');
    process.exit(1);
}

const { createCanvas } = Canvas;

/**
 * 알약 캡슐 아이콘을 생성합니다.
 */
function drawPillIcon(size) {
    const canvas = createCanvas(size, size);
    const ctx = canvas.getContext('2d');

    // 배경 투명
    ctx.clearRect(0, 0, size, size);

    // 알약 캡슐 그리기
    const centerX = size / 2;
    const centerY = size / 2;
    const pillWidth = size * 0.7;
    const pillHeight = size * 0.4;
    const radius = pillHeight / 2;

    // 그림자
    ctx.shadowColor = 'rgba(0, 0, 0, 0.2)';
    ctx.shadowBlur = size * 0.05;
    ctx.shadowOffsetX = size * 0.02;
    ctx.shadowOffsetY = size * 0.02;

    // 캡슐 배경 (흰색 부분)
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath();
    ctx.arc(centerX - pillWidth/2 + radius, centerY, radius, Math.PI/2, Math.PI*3/2);
    ctx.arc(centerX + pillWidth/2 - radius, centerY, radius, -Math.PI/2, Math.PI/2);
    ctx.closePath();
    ctx.fill();

    // 그림자 제거
    ctx.shadowColor = 'transparent';

    // 캡슐 왼쪽 절반 (파란색)
    const gradient1 = ctx.createLinearGradient(
        centerX - pillWidth/2, centerY - radius,
        centerX - pillWidth/2, centerY + radius
    );
    gradient1.addColorStop(0, '#60A5FA');
    gradient1.addColorStop(1, '#3B82F6');

    ctx.fillStyle = gradient1;
    ctx.beginPath();
    ctx.arc(centerX - pillWidth/2 + radius, centerY, radius, Math.PI/2, Math.PI*3/2);
    ctx.lineTo(centerX, centerY - radius);
    ctx.lineTo(centerX, centerY + radius);
    ctx.closePath();
    ctx.fill();

    // 캡슐 오른쪽 절반 (빨간색)
    const gradient2 = ctx.createLinearGradient(
        centerX + pillWidth/2, centerY - radius,
        centerX + pillWidth/2, centerY + radius
    );
    gradient2.addColorStop(0, '#F87171');
    gradient2.addColorStop(1, '#EF4444');

    ctx.fillStyle = gradient2;
    ctx.beginPath();
    ctx.arc(centerX + pillWidth/2 - radius, centerY, radius, -Math.PI/2, Math.PI/2);
    ctx.lineTo(centerX, centerY + radius);
    ctx.lineTo(centerX, centerY - radius);
    ctx.closePath();
    ctx.fill();

    // 테두리
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.1)';
    ctx.lineWidth = size * 0.01;
    ctx.beginPath();
    ctx.arc(centerX - pillWidth/2 + radius, centerY, radius, Math.PI/2, Math.PI*3/2);
    ctx.arc(centerX + pillWidth/2 - radius, centerY, radius, -Math.PI/2, Math.PI/2);
    ctx.closePath();
    ctx.stroke();

    // 중앙 분할선
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.15)';
    ctx.lineWidth = size * 0.015;
    ctx.beginPath();
    ctx.moveTo(centerX, centerY - radius);
    ctx.lineTo(centerX, centerY + radius);
    ctx.stroke();

    // 하이라이트 효과
    ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
    ctx.beginPath();
    ctx.ellipse(
        centerX - pillWidth/4, centerY - radius/3,
        pillWidth/3, radius/2.5,
        0, 0, Math.PI * 2
    );
    ctx.fill();

    return canvas;
}

/**
 * 모든 크기의 아이콘을 생성합니다.
 */
function generateAllIcons() {
    const sizes = [72, 96, 128, 144, 152, 192, 384, 512];
    const outputDir = __dirname;

    console.log('🎨 알약 아이콘 생성 시작...\n');

    sizes.forEach(size => {
        const canvas = drawPillIcon(size);
        const filename = `icon-${size}x${size}.png`;
        const filepath = path.join(outputDir, filename);

        // PNG 버퍼로 변환하여 저장
        const buffer = canvas.toBuffer('image/png');
        fs.writeFileSync(filepath, buffer);

        console.log(`✅ ${filename} 생성 완료`);
    });

    console.log(`\n🎉 모든 아이콘 생성 완료!`);
    console.log(`📁 저장 위치: ${outputDir}`);
}

// 스크립트 실행
try {
    generateAllIcons();
} catch (error) {
    console.error('❌ 오류 발생:', error.message);
    process.exit(1);
}
