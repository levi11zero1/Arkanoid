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

## Hướng dẫn chơi

⬅️ / A: Di chuyển paddle sang trái
➡️ / D: Di chuyển paddle sang phải
Space: Bắt đầu game hoặc phóng bóng nếu đang gắn vào paddle
P: Tạm dừng / Tiếp tục
Esc: Thoát game (có hộp thoại xác nhận)

### Chế độ 2 người
Người chơi Trên: A, D để di chuyển
Người chơi Dưới: Mũi tên Trái, Phải để di chuyển
P: Tạm dừng chung cho cả hai

Mục tiêu: Dùng gậy Như Ý đánh bóng phá hết gạch mà không để bóng rơi khỏi màn hình.

Ghi chú: Các phím thử nghiệm như R (phát nhạc) và hiển thị preview level đã bị loại bỏ để gameplay rõ ràng hơn.
