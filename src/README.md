# Arkanoid

Trò chơi Arkanoid viết bằng Java.

## Mô tả

Đây là một bản Arkanoid đơn giản được triển khai bằng Java. Dự án bao gồm game loop, quản lý entities (bóng, paddle, block), hệ thống levels, power-up, âm thanh và giao diện người dùng cơ bản.

## Cấu trúc thư mục chính

- `src/` – mã nguồn Java
  - `entities/` – lớp liên quan tới đối tượng game (Ball, Paddle, Block, ...)
  - `game/` – lớp điều khiển game, panel, renderer, loop
  - `levels/` – định nghĩa level và trình quản lý level
  - `powerup/` – hệ thống power-up
  - `ui/` – giao diện người dùng (menu, ranking, ...)
  - `utils/` – tiện ích (âm thanh, cấu hình, vận tốc...)
- `images/` – tài nguyên hình ảnh
- `music/` – file nhạc/âm thanh
- `saves/` – lưu trữ điểm và tiến trình

## Tính năng

- Game loop & renderer
- Hệ thống levels và background
- Chọn skin paddle và bóng
- Power-ups
- Lưu/xếp hạng điểm
- Hỗ trợ âm thanh nền và hiệu ứng
