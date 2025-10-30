package game;

/**
 * Minimal GameLoop skeleton implementing IGameLoop. Implementation intentionally empty
 * — this file is a placeholder/skeleton to be filled during refactor.
 */
public class GameLoop implements IGameLoop {
    private int targetFps = 60;
    private volatile boolean running = false;

    @Override
    public void start() { }

    @Override
    public void stop() { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void setTargetFps(int fps) { this.targetFps = fps; }

    @Override
    public boolean isRunning() { return running; }
}
