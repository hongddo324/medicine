#!/usr/bin/env python3
"""
PWA 알약 아이콘 생성 스크립트
필요한 패키지: pip install Pillow
"""

from PIL import Image, ImageDraw
import os

def draw_pill_icon(size):
    """알약 캡슐 아이콘을 생성합니다."""
    # RGBA 모드로 이미지 생성 (투명 배경)
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # 캡슐 크기 계산
    center_x = size // 2
    center_y = size // 2
    pill_width = int(size * 0.7)
    pill_height = int(size * 0.4)
    radius = pill_height // 2

    # 캡슐 왼쪽 (파란색 반원)
    left_x = center_x - pill_width // 2
    blue_bbox = [
        left_x,
        center_y - radius,
        left_x + radius * 2,
        center_y + radius
    ]
    draw.ellipse(blue_bbox, fill=(59, 130, 246, 255))  # Blue

    # 캡슐 오른쪽 (빨간색 반원)
    right_x = center_x + pill_width // 2 - radius * 2
    red_bbox = [
        right_x,
        center_y - radius,
        right_x + radius * 2,
        center_y + radius
    ]
    draw.ellipse(red_bbox, fill=(239, 68, 68, 255))  # Red

    # 중앙 사각형 (왼쪽 파란색)
    blue_rect = [
        left_x + radius,
        center_y - radius,
        center_x,
        center_y + radius
    ]
    draw.rectangle(blue_rect, fill=(59, 130, 246, 255))

    # 중앙 사각형 (오른쪽 빨간색)
    red_rect = [
        center_x,
        center_y - radius,
        right_x + radius,
        center_y + radius
    ]
    draw.rectangle(red_rect, fill=(239, 68, 68, 255))

    # 중앙 분할선
    line_width = max(2, size // 100)
    draw.line(
        [(center_x, center_y - radius), (center_x, center_y + radius)],
        fill=(0, 0, 0, 100),
        width=line_width
    )

    # 테두리 추가
    outline_width = max(1, size // 100)

    # 왼쪽 반원 테두리
    draw.arc(blue_bbox, 90, 270, fill=(0, 0, 0, 50), width=outline_width)

    # 오른쪽 반원 테두리
    draw.arc(red_bbox, 270, 90, fill=(0, 0, 0, 50), width=outline_width)

    # 위쪽 선
    draw.line(
        [(left_x + radius, center_y - radius), (right_x + radius, center_y - radius)],
        fill=(0, 0, 0, 50),
        width=outline_width
    )

    # 아래쪽 선
    draw.line(
        [(left_x + radius, center_y + radius), (right_x + radius, center_y + radius)],
        fill=(0, 0, 0, 50),
        width=outline_width
    )

    return image

def main():
    """모든 크기의 아이콘을 생성합니다."""
    sizes = [72, 96, 128, 144, 152, 192, 384, 512]

    # 현재 스크립트 위치에 아이콘 저장
    script_dir = os.path.dirname(os.path.abspath(__file__))

    print("🎨 알약 아이콘 생성 시작...")

    for size in sizes:
        icon = draw_pill_icon(size)
        filename = f"icon-{size}x{size}.png"
        filepath = os.path.join(script_dir, filename)
        icon.save(filepath, 'PNG')
        print(f"✅ {filename} 생성 완료")

    print("\n🎉 모든 아이콘 생성 완료!")
    print(f"📁 저장 위치: {script_dir}")

if __name__ == "__main__":
    try:
        main()
    except ImportError:
        print("❌ Pillow 패키지가 설치되어 있지 않습니다.")
        print("다음 명령어로 설치하세요: pip install Pillow")
    except Exception as e:
        print(f"❌ 오류 발생: {e}")
