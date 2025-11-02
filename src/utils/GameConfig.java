package utils;

//Liệt kê các Constant hết vào đây cho dễ xem
public class GameConfig {
    // Screen
    public static final int SCREEN_WIDTH = 684;
    public static final int SCREEN_HEIGHT = 800;

    // Ball
    public static int BALL_SIZE = 20;
    public static final double BALL_MIN_SPEED = 2.0;
    public static final double BALL_MAX_SPEED = 6.0;
    public static double BALL_DEFAULT_SPEED = 10.0;

    // Paddle
    public static final int DEFAULT_PADDLE_WIDTH = 100;
    public static int PADDLE_WIDTH = DEFAULT_PADDLE_WIDTH;
    public static final int PADDLE_HEIGHT = 15;
    public static final double DEFAULT_PADDLE_SPEED = 320.0;
    public static double PADDLE_SPEED = DEFAULT_PADDLE_SPEED;
    public static final int PADDLE_BOTTOM_MARGIN = 100;
    public static final int PADDLE_EXTRA_RAISE_PIXELS = 75; // ~2cm at 96 DPI

    // Block
    public static final int BLOCK_WIDTH = 40;
    public static final int BLOCK_HEIGHT = 20;
    public static final int BLOCK_SPACING = 45;
    public static final int BLOCK_ROW_SPACING = 25;
    public static final int UNDESTRUCTABLE_BLOCK = -1; // Special value for undestructable blocks

    // Layout
    public static final int BLOCKS_START_X = 50;
    public static final int BLOCKS_START_Y = 30;  // Giảm từ 50 -> 30 để block cao hơn

    // Game properties
    public static final int TIMER_DELAY = 10;
    public static final int MAX_LEVELS = 6;

    // Physics properties
    public static final double VELOCITY_VARIATION = 0.1;
    public static final double MAX_PADDLE_ANGLE = 60.0;
}
