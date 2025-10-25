package utils;

/**
 * Lớp tiện ích chứa các hàm vật lý dùng chung cho trò chơi.
 * Bao gồm: kiểm tra va chạm giữa bóng và hình chữ nhật, tính mặt va chạm,
 * phản xạ vận tốc (bounce), giới hạn tốc độ (clamp), và xử lý va chạm với mép màn hình.
 *
 * Các phương thức ở đây là tĩnh (static) để dễ gọi từ nhiều nơi trong code.
 */
public class Physics {

    public static class BoundsResult {
        public double x;
        public double y;
        public Velocity velocity;
        public boolean bounced;

        public BoundsResult(double x, double y, Velocity velocity, boolean bounced) {
            this.x = x;
            this.y = y;
            this.velocity = velocity;
            this.bounced = bounced;
        }
    }

    /**
     * Giới hạn (clamp) vector vận tốc sao cho magnitude nằm trong khoảng
     * [BALL_MIN_SPEED, BALL_MAX_SPEED]. Nếu vận tốc quá nhỏ hoặc quá lớn thì scale lại.
     */
    public static Velocity clampSpeed(Velocity v) {
        double speed = v.getMagnitude();
        if (speed > 0 && speed < GameConfig.BALL_MIN_SPEED) {
            return v.scale(GameConfig.BALL_MIN_SPEED / speed);
        } else if (speed > GameConfig.BALL_MAX_SPEED) {
            return v.scale(GameConfig.BALL_MAX_SPEED / speed);
        }
        return v;
    }

    /** Trả về vận tốc phản xạ theo phương ngang (đổi chiều dx). */
    public static Velocity reflectHorizontal(Velocity v) {
        return new Velocity(-v.getDx(), v.getDy());
    }

    /** Trả về vận tốc phản xạ theo phương dọc (đổi chiều dy). */
    public static Velocity reflectVertical(Velocity v) {
        return new Velocity(v.getDx(), -v.getDy());
    }

    /**
     * Tính vận tốc mới khi bóng va chạm với paddle dựa trên điểm chạm.
     * Khi bóng chạm gần mép paddle, góc nảy lớn hơn; khi chạm gần tâm, bóng sẽ nảy lên gần thẳng.
     * Trả về một Velocity đã được clamp.
     */
    public static Velocity bounceFromPaddle(double ballX, int ballSize, double paddleX, int paddleWidth, Velocity current) {
        double paddleCenter = paddleX + paddleWidth / 2.0;
        double ballCenter = ballX + ballSize / 2.0;
        double hitOffset = (ballCenter - paddleCenter) / (paddleWidth / 2.0); // -1 -> 1

        hitOffset = Math.max(-1.0, Math.min(1.0, hitOffset));

        double angle = hitOffset * GameConfig.MAX_PADDLE_ANGLE;
        double speed = current.getMagnitude();

        return clampSpeed(Velocity.fromAngle(angle - 90, speed));
    }

    /**
     * Kiểm tra va chạm giữa hình vuông (bóng) và hình chữ nhật (rx,ry,rw,rh).
     * Trả về true nếu có overlap (AABB check).
     */
    public static boolean isCollidingRect(double ballX, double ballY, int ballSize,
                                          double rx, double ry, int rw, int rh) {
        return ballX + ballSize > rx && ballX < rx + rw && ballY + ballSize > ry && ballY < ry + rh;
    }

    /**
     * Tính phía va chạm giữa hình chữ nhật (rx,ry,rw,rh) và hình vuông bóng.
     * Trả về "left"/"right"/"top"/"bottom" hoặc null nếu không va chạm.
     */
    /**
     * Xác định 'mặt' va chạm giữa bóng (square) và một hình chữ nhật: trả về
     * "left", "right", "top", hoặc "bottom". Nếu không va chạm trả về null.
     *
     * Cách hoạt động: so sánh khoảng cách từ tâm bóng tới các cạnh của rectangle,
     * cạnh nào gần nhất quyết định là va chạm ngang hay dọc; sau đó dùng dấu của
     * vận tốc để phân biệt left/right hoặc top/bottom.
     */
    public static String collisionSideForRect(double ballX, double ballY, int ballSize,
                                              double rx, double ry, int rw, int rh,
                                              double ballVx, double ballVy) {
        if (!isCollidingRect(ballX, ballY, ballSize, rx, ry, rw, rh)) return null;

        double ballCenterX = ballX + ballSize / 2.0;
        double ballCenterY = ballY + ballSize / 2.0;

        double leftDistance = Math.abs(ballCenterX - rx);
        double rightDistance = Math.abs(ballCenterX - (rx + rw));
        double topDistance = Math.abs(ballCenterY - ry);
        double bottomDistance = Math.abs(ballCenterY - (ry + rh));

        double minHorizontal = Math.min(leftDistance, rightDistance);
        double minVertical = Math.min(topDistance, bottomDistance);

        if (minHorizontal < minVertical) {
            return (ballVx > 0) ? "left" : "right";
        } else {
            return (ballVy > 0) ? "top" : "bottom";
        }
    }

    /**
     * Kiểm tra va chạm với mép màn hình (trái, phải, trên). Nếu bóng vượt ra ngoài
     * thì hàm sẽ đẩy bóng vào trong và phản xạ thành phần vận tốc tương ứng.
     *
     * Trả về một {@link BoundsResult} chứa vị trí và vận tốc đã điều chỉnh, cùng
     * flag cho biết có xảy ra phản xạ hay không.
     */
    public static BoundsResult checkBounds(double x, double y, int ballSize, Velocity velocity,
                                           int screenWidth, int screenHeight) {
        boolean bounced = false;
        double nx = x;
        double ny = y;
        Velocity nv = velocity;

        if (nx < 0) {
            nx = 0;
            nv = new Velocity(Math.abs(nv.getDx()), nv.getDy());
            bounced = true;
        }

        if (nx + ballSize > screenWidth) {
            nx = screenWidth - ballSize;
            nv = new Velocity(-Math.abs(nv.getDx()), nv.getDy());
            bounced = true;
        }

        if (ny < 0) {
            ny = 0;
            nv = new Velocity(nv.getDx(), Math.abs(nv.getDy()));
            bounced = true;
        }

        if (bounced) {
            nv = clampSpeed(nv);
        }

        return new BoundsResult(nx, ny, nv, bounced);
    }
}
