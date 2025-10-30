package game;

/**
 * Interface for the game loop controller.
 * Minimal contract used by GamePanel to control timing.
 */
public interface IGameLoop {
    void start();
    void stop();
    void pause();
    void resume();
    void setTargetFps(int fps);
    boolean isRunning();
    /** Set a listener to be called every tick with delta seconds. */
    void setTickListener(TickListener listener);

    interface TickListener {
        void onTick(double deltaSeconds);
    }
}
