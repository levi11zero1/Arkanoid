package utils;

//Liệt kê các Constant hết vào đây cho dễ xem
public class GameConfig {
    // Screen
    public static final int SCREEN_WIDTH = 684;
    public static final int SCREEN_HEIGHT = 800;
    
    // Ball
    public static final int BALL_SIZE = 20;
    public static final double BALL_MIN_SPEED = 2.0;
    public static final double BALL_MAX_SPEED = 6.0;
    public static final double BALL_DEFAULT_SPEED = 3.0;
    
    // Paddle
    public static final int PADDLE_WIDTH = 100;
    public static final int PADDLE_HEIGHT = 15;
    public static final double PADDLE_SPEED = 320.0;
    
    // Block
    public static final int BLOCK_WIDTH = 40;
    public static final int BLOCK_HEIGHT = 20;
    public static final int BLOCK_SPACING = 45;
    public static final int BLOCK_ROW_SPACING = 25;
    
    // Layout
    public static final int BLOCKS_START_X = 50;
    public static final int BLOCKS_START_Y = 50;
    
    // Game properties
    public static final int TIMER_DELAY = 10;
    public static final int MAX_LEVELS = 3;
    
    // Physics properties
    public static final double VELOCITY_VARIATION = 0.1;
    public static final double MAX_PADDLE_ANGLE = 60.0;
}