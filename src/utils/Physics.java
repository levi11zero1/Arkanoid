package utils;

/**
 * Utility class chứa các hàm vật lý dùng chung: va chạm, nảy, clamp speed, ...
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

    public static Velocity clampSpeed(Velocity v) {
        double speed = v.getMagnitude();
        if (speed > 0 && speed < GameConfig.BALL_MIN_SPEED) {
            return v.scale(GameConfig.BALL_MIN_SPEED / speed);
        } else if (speed > GameConfig.BALL_MAX_SPEED) {
            return v.scale(GameConfig.BALL_MAX_SPEED / speed);
        }
        return v;
    }

    public static Velocity reflectHorizontal(Velocity v) {
        return new Velocity(-v.getDx(), v.getDy());
    }

    public static Velocity reflectVertical(Velocity v) {
        return new Velocity(v.getDx(), -v.getDy());
    }

    public static Velocity bounceFromPaddle(double ballX, int ballSize, double paddleX, int paddleWidth, Velocity current) {
        double paddleCenter = paddleX + paddleWidth / 2.0;
        double ballCenter = ballX + ballSize / 2.0;
        double hitOffset = (ballCenter - paddleCenter) / (paddleWidth / 2.0); // -1 -> 1

        hitOffset = Math.max(-1.0, Math.min(1.0, hitOffset));

        double angle = hitOffset * GameConfig.MAX_PADDLE_ANGLE;
        double speed = current.getMagnitude();

        // Ý nghĩa góc giống với trước: trừ 90 để bắn lên
        return clampSpeed(Velocity.fromAngle(angle - 90, speed));
    }

    public static boolean isCollidingRect(double ballX, double ballY, int ballSize,
                                          double rx, double ry, int rw, int rh) {
        return ballX + ballSize > rx && ballX < rx + rw && ballY + ballSize > ry && ballY < ry + rh;
    }

    /**
     * Tính phía va chạm giữa hình chữ nhật (rx,ry,rw,rh) và hình vuông bóng.
     * Trả về "left"/"right"/"top"/"bottom" hoặc null nếu không va chạm.
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
     * Kiểm tra mép màn hình và trả về vị trí/velocity đã điều chỉnh.
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
