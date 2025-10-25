package utils;

/**
 * Lớp tiện ích chứa các hàm liên quan đến di chuyển của đối tượng trong game.
 *
 * Hiện chứa các hàm tĩnh để tính toán vị trí mới cho bóng và thanh đỡ (paddle).
 * Mục tiêu: gom logic di chuyển vào một chỗ để dễ bảo trì và test.
 */
public class Movement {

    public static class BallMoveResult {
        public double x;
        public double y;
        public double prevX;
        public double prevY;
        public int prevSize;
        public Velocity velocity;

        public BallMoveResult(double x, double y, double prevX, double prevY, int prevSize, Velocity velocity) {
            this.x = x;
            this.y = y;
            this.prevX = prevX;
            this.prevY = prevY;
            this.prevSize = prevSize;
            this.velocity = velocity;
        }
    }

    /**
     * Di chuyển bóng theo velocity hiện tại.
     *
     * Input: vị trí hiện tại (x,y), vector vận tốc và kích thước bóng.
     * Output: trả về một {@link BallMoveResult} chứa vị trí mới, vị trí trước đó và velocity
     * đã được điều chỉnh (ví dụ được clamp theo giới hạn tốc độ).
     */
    public static BallMoveResult moveBall(double x, double y, Velocity velocity, int ballSize) {
        double prevX = x;
        double prevY = y;
        int prevSize = ballSize;

        double nx = x + velocity.getDx();
        double ny = y + velocity.getDy();

        Velocity nv = Physics.clampSpeed(velocity);

        return new BallMoveResult(nx, ny, prevX, prevY, prevSize, nv);
    }

    /**
     * Cập nhật vị trí ngang (x) của paddle dựa trên trạng thái phím trái/phải và delta-time (dt).
     * Hàm sẽ giữ paddle nằm trong giới hạn chiều rộng màn hình (frameWidth).
     *
     * Trả về giá trị x mới.
     */
    public static double updatePaddle(double x, boolean leftPressed, boolean rightPressed, int frameWidth, double dt) {
        double v = 0;
        if (leftPressed) v -= GameConfig.PADDLE_SPEED;
        if (rightPressed) v += GameConfig.PADDLE_SPEED;

        double nx = x + v * dt;

        if (nx < 0) nx = 0;
        if (nx + GameConfig.PADDLE_WIDTH > frameWidth) nx = frameWidth - GameConfig.PADDLE_WIDTH;

        return nx;
    }
}
