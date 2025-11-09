package game;

/**
 * Tạon một vòng lặp game đơn giản sử dụng javax.swing.Timer.
 */
public class GameLoop implements IGameLoop {
    private int targetFps = 60;
    private volatile boolean running = false;
    private javax.swing.Timer timer;
    private IGameLoop.TickListener listener;
    private long lastNanos = -1L;

    @Override
    public void start() {
        if (running) return;
        int delayMs = Math.max(1, 1000 / targetFps);
        lastNanos = System.nanoTime();
        timer = new javax.swing.Timer(delayMs, e -> {
            long now = System.nanoTime();
            double delta = (now - lastNanos) / 1_000_000_000.0;
            lastNanos = now;
            try {
                if (listener != null) listener.onTick(delta);
            } catch (Throwable t) {

            }
        });
        timer.setRepeats(true);
        timer.start();
        running = true;
    }

    @Override
    public void stop() {
        if (!running) return;
        if (timer != null) timer.stop();
        running = false;
    }

    @Override
    public void pause() { stop(); }

    @Override
    public void resume() { start(); }

    @Override
    public void setTargetFps(int fps) { this.targetFps = fps; }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public void setTickListener(IGameLoop.TickListener l) {
        this.listener = l;
    }
}

