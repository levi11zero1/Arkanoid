package game;

/**
 * Interface của game loop controller.
 */
public interface IGameLoop {
    void start();
    void stop();
    void pause();
    void resume();
    void setTargetFps(int fps);
    boolean isRunning();

    void setTickListener(TickListener listener);

    interface TickListener {
        void onTick(double deltaSeconds);
    }
}
