
package powerup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import entities.Paddle;
import entities.EntityManager;
import entities.Ball;

/**
 * PowerUpManager: quản lý danh sách powerup, spawn timer, update vị trí, và apply effect.
 */
public class PowerUpManager {
    private final List<PowerUp> active = new ArrayList<>();
    private final Random random = new Random();
    private Timer spawnTimer;

    // spawn interval in ms
    private final int spawnIntervalMs = 10000;

    public PowerUpManager() {
    }

    public void startSpawning(java.awt.Component parent) {
        if (spawnTimer != null && spawnTimer.isRunning()) return;
        spawnTimer = new Timer(spawnIntervalMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int pw = parent.getWidth() > 20 ? parent.getWidth() : 200;
                spawnRandom(pw);
            }
        });
        spawnTimer.setRepeats(true);
        spawnTimer.start();
    }

    public void stopSpawning() {
        if (spawnTimer != null) {
            spawnTimer.stop();
        }
    }

    public void spawnRandom(int panelWidth) {
        PowerUp.Type[] types = PowerUp.Type.values();
        PowerUp.Type randomType = types[random.nextInt(types.length)];
        int spawnX = random.nextInt(Math.max(1, panelWidth - 20));
        PowerUp p = new PowerUp(randomType, spawnX, 0);
        synchronized (active) {
            active.add(p);
        }
    }

    public void spawnPowerUp(PowerUp.Type type, int x, int y) {
        PowerUp p = new PowerUp(type, x, y);
        synchronized (active) {
            active.add(p);
        }
    }


    /**
     * Update powerups position and detect collection/out-of-bounds.
     * If collected, apply directly to provided paddle/ball.
     */
    public void updateAll(int panelHeight, Paddle paddle, EntityManager entityManager) {
        synchronized (active) {
            for (Iterator<PowerUp> it = active.iterator(); it.hasNext();) {
                PowerUp p = it.next();
                p.updatePosition();

                if (paddle != null && p.getBounds().intersects(paddle.getBounds())) {
                    applyEffect(p, paddle, entityManager);
                    it.remove();
                    continue;
                }
                if (p.isOutOfBounds(panelHeight)) {
                    it.remove();
                }
            }
        }
    }

    private void applyEffect(PowerUp p, Paddle paddle, EntityManager entityManager) {
        PowerUp.Type type = p.getType();

        if (type == PowerUp.Type.PADDLE_EXPAND || type == PowerUp.Type.PADDLE_SHRINK || type == PowerUp.Type.PADDLE_SPEED_UP) {
            paddle.applyPowerUp(type);
            return;
        }

        if (entityManager == null) {
            return;
        }

        switch (type) {
            case BALL_EXPAND, BALL_SHRINK, BALL_SLOW -> {
                Ball mainBall = entityManager.getPrimaryBall();
                if (mainBall != null) {
                    mainBall.applyPowerUp(type);
                }
            }
            case BALL_MULTIPLY_TEN -> entityManager.multiplyBallsTo(10);
            default -> {
                // no-op for unhandled types
            }
        }
    }

    public void resetAll() {
        synchronized (active) {
            active.clear();
        }
        stopSpawning();
    }

    // For rendering purposes GamePanel can still access list via a snapshot
    public List<PowerUp> snapshot() {
        synchronized (active) {
            return new ArrayList<>(active);
        }
    }
    public boolean isActive(PowerUp.Type type) {
        synchronized (active) {
            for (PowerUp p : active) {
                if (p.getType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

}
