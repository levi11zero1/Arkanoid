package function;

import java.util.ArrayList;
import java.util.List;

// Trạng thái game (snapshot)
//
// Vai trò:
// - Đóng gói toàn bộ dữ liệu cần thiết để khôi phục lại màn chơi đúng tại thời điểm lưu.
// - Được tạo bởi GamePanel.toGameState() và áp dụng vào game bằng GamePanel.applyGameState(...).
// - Được ghi/đọc bởi SaveManager theo định dạng văn bản.
public class GameState {
    public final int level;           // Màn hiện tại
    public final double ballX;        // Toạ độ X bóng (double để chính xác)
    public final double ballY;        // Toạ độ Y bóng
    public final double ballDx;       // Vận tốc X bóng
    public final double ballDy;       // Vận tốc Y bóng
    public final double paddleX;      // Toạ độ X của paddle (double để khớp nội bộ)
    public final int paddleY;         // Toạ độ Y của paddle (thường cố định theo cấu hình)
    public final List<BlockState> blocks; // Danh sách trạng thái block (vị trí, số lần chịu đòn còn lại, đã phá chưa)
    public final boolean ballAttached; // whether the ball was attached to the paddle when saved

    public GameState(int level,
                     double ballX, double ballY,
                     double ballDx, double ballDy,
                     double paddleX, int paddleY,
                     List<BlockState> blocks,
                     boolean ballAttached) {
        this.level = level;
        this.ballX = ballX;
        this.ballY = ballY;
        this.ballDx = ballDx;
        this.ballDy = ballDy;
        this.paddleX = paddleX;
        this.paddleY = paddleY;
        this.blocks = new ArrayList<>(blocks);
        this.ballAttached = ballAttached;
    }

    // Ảnh chụp một viên gạch trong game
    public static class BlockState {
        public final int x;               // vị trí X của block (theo lưới)
        public final int y;               // vị trí Y của block
        public final int hitsRemaining;   // số lần đập còn lại trước khi vỡ (>=1)
        public final boolean destroyed;   // true nếu block đã bị phá (bỏ qua vẽ/va chạm)

        public BlockState(int x, int y, int hitsRemaining, boolean destroyed) {
            this.x = x;
            this.y = y;
            this.hitsRemaining = hitsRemaining;
            this.destroyed = destroyed;
        }
    }
}
