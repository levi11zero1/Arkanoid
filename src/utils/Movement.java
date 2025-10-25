package utils;

/**
 * Utilities for movement-related logic for Ball and Paddle.
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
     * Move ball by its velocity, record previous position/size and clamp speed.
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
     * Update paddle X position given input and dt. Keeps within frame bounds.
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
