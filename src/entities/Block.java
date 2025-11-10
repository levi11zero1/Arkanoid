package entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import utils.GameConfig;

public class Block implements GameObject {
    private int x, y;
    private boolean destroyed = false;
    private int hitsRemaining;
    private Color customColor = null;
    // Hiệu ứng gạch nứt: sau khi bị đập, hiển thị sprite "broken" của cấp hiện tại
    // cho tới lần va chạm tiếp theo.
    private boolean showBroken = false;
    private int brokenTier = 0;
    private boolean powerUpSpawned = false;

    public boolean isPowerUpSpawned() {
        return powerUpSpawned;
    }

    public void setPowerUpSpawned(boolean spawned) {
        this.powerUpSpawned = spawned;
    }



    /**
     * Khởi tạo block mới cho level đang chơi.
     * @param x vị trí X (pixel theo lưới)
     * @param y vị trí Y
     * @param hitsRemaining số lần chịu đòn còn lại trước khi vỡ (>=1), hoặc UNDESTRUCTABLE (-1)
     */
    public Block(int x, int y, int hitsRemaining) {
        this.x = x;
        this.y = y;
        // Allow UNDESTRUCTABLE (-1) or ensure at least 1 hit
        this.hitsRemaining = (hitsRemaining == GameConfig.UNDESTRUCTABLE_BLOCK) ? GameConfig.UNDESTRUCTABLE_BLOCK : Math.max(1, hitsRemaining);
    }

    // Overload for restore from save
    // Dùng khi khôi phục từ GameState: có thể block đã bị phá (destroyed=true) hoặc còn lại số lần đập cụ thể.
    public Block(int x, int y, int hitsRemaining, boolean destroyed) {
        this(x, y, hitsRemaining);
        this.destroyed = destroyed;
    }

    // Overload để khởi tạo block với màu tuỳ chỉnh (ví dụ level editor)
    public Block(int x, int y, int hitsRemaining, Color colorOverride) {
        this(x, y, hitsRemaining);
        this.customColor = colorOverride;
    }

    // Set màu các block khác nhau hoặc vẽ ảnh brick nếu có
    @Override
    public void draw(Graphics g) {
        if (destroyed) return;

        // Prefer image-based bricks if available
        ensureBrickImagesLoaded();

    Image img;
        if (customColor != null) {
            // custom color: still draw colored rect
            img = null;
        } else if (hitsRemaining == GameConfig.UNDESTRUCTABLE_BLOCK) {
            img = brickX;
        } else if (showBroken && brokenTier >= 2) {
            // Ưu tiên hiển thị sprite broken nếu có
            img = switch (brokenTier) {
                case 3 -> brick3_broken;
                case 2 -> brick2_broken;
                default -> null;
            };
            if (img == null) {
                // Fallback nếu thiếu ảnh broken
                img = switch (hitsRemaining) {
                    case 3 -> brick3;
                    case 2 -> brick2;
                    default -> brick1;
                };
            }
        } else if (hitsRemaining == 3) {
            img = brick3;
        } else if (hitsRemaining == 2) {
            img = brick2;
        } else {
            img = brick1;
        }

        if (img != null) {
            g.drawImage(img, x, y, GameConfig.BLOCK_WIDTH, GameConfig.BLOCK_HEIGHT, null);
        } else {
            Color color = (customColor != null)
                ? customColor
                : switch (hitsRemaining) {
                    case GameConfig.UNDESTRUCTABLE_BLOCK -> Color.WHITE;
                    case 3 -> Color.MAGENTA;
                    case 2 -> Color.ORANGE;
                    default -> Color.RED;
                };

            g.setColor(color);
            g.fillRect(x, y, GameConfig.BLOCK_WIDTH, GameConfig.BLOCK_HEIGHT);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, GameConfig.BLOCK_WIDTH, GameConfig.BLOCK_HEIGHT);
        }
    }

    private static Image brick1;
    private static Image brick2;
    private static Image brick3;
    private static Image brickX;
    private static Image brick2_broken;
    private static Image brick3_broken;
    private static boolean brickImagesInitialized = false;

    private static void ensureBrickImagesLoaded() {
        if (brickImagesInitialized) return;
        brickImagesInitialized = true;
        brick1 = loadImage("images/skinBrick/Brick1.png");
        brick2 = loadImage("images/skinBrick/Brick2.png");
        brick3 = loadImage("images/skinBrick/Brick3.png");
        brickX = loadImage("images/skinBrick/BrickX.png");
        // Ảnh gạch nứt
        brick2_broken = loadImage("images/skinBrick/Brick2_broken.png");
        brick3_broken = loadImage("images/skinBrick/Brick3_broken.png");
    }

    private static Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            URL res = Block.class.getClassLoader().getResource(path);
            if (res != null) {
                return new ImageIcon(res).getImage();
            }
            File f = new File(path);
            if (f.exists()) {
                return ImageIO.read(f);
            }
        } catch (IOException | SecurityException ignored) {
        }
        return null;
    }

    /**
     * Xử lý va chạm bóng - block. Giảm hitsRemaining; nếu về 0 thì đánh dấu destroyed.
     * Trả về true nếu có va chạm trong frame này
     * Xử lí khi nhận được power up bóng to, block c1 c2 bị phá, c3 giảm độ cứng.
     */
    public boolean isHit(int ballX, int ballY, int ballSize) {
        if (!destroyed &&
                ballX + ballSize > x && ballX < x + GameConfig.BLOCK_WIDTH &&
                ballY + ballSize > y && ballY < y + GameConfig.BLOCK_HEIGHT) {

            // Gạch không thể phá: không thay đổi trạng thái (chỉ trả về true để bóng nảy)
            if (hitsRemaining == GameConfig.UNDESTRUCTABLE_BLOCK) {
                return true;
            }

            // Nếu bóng đang to hơn kích thước mặc định
            if (GameConfig.BALL_SIZE > GameConfig.DEFAULT_BALL_SIZE) {
                if (hitsRemaining == 3) {
                    // Gạch cấp 3 → giảm xuống cấp 1, nhưng hiển thị Brick3_broken
                    hitsRemaining = 1;
                    showBroken = true;
                    brokenTier = 2;
                } else {
                    // Gạch cấp 1 hoặc 2 → vỡ ngay lập tức
                    destroyed = true;
                    showBroken = false;
                    brokenTier = 0;
                }
            } else {
                // Bóng bình thường: giảm độ bền như thường lệ
                int prevTier = hitsRemaining;
                hitsRemaining--;
                if (hitsRemaining <= 0) {
                    destroyed = true;
                    showBroken = false;
                    brokenTier = 0;
                } else {
                    // Hiển thị sprite broken của tier trước đó (prevTier)
                    showBroken = prevTier >= 2;
                    brokenTier = prevTier;
                }
            }
            return true;
        }
        return false;
    }




    // Check va chạm như paddle
    /**
     * Xác định hướng va chạm tương đối để điều chỉnh bật nảy của bóng (trái/phải/trên/dưới).
     * Bỏ qua nếu block đã destroyed.
     */
    public String getCollisionSide(double ballX, double ballY, double ballSize, double ballVx, double ballVy) {
        if (destroyed) return null;

        double ballCenterX = ballX + ballSize / 2;
        double ballCenterY = ballY + ballSize / 2;

        double leftDistance = Math.abs(ballCenterX - x);
        double rightDistance = Math.abs(ballCenterX - (x + GameConfig.BLOCK_WIDTH));
        double topDistance = Math.abs(ballCenterY - y);
        double bottomDistance = Math.abs(ballCenterY - (y + GameConfig.BLOCK_HEIGHT));

        double minHorizontal = Math.min(leftDistance, rightDistance);
        double minVertical = Math.min(topDistance, bottomDistance);

        if (minHorizontal < minVertical) {
            return (ballVx > 0) ? "left" : "right";
        } else {
            return (ballVy > 0) ? "top" : "bottom";
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    // Áp dụng 1 lần sát thương bất kể có overlap hình học hay không (dùng cho CCD)
    public void applyHit() {
        if (!destroyed && hitsRemaining != GameConfig.UNDESTRUCTABLE_BLOCK) {
            int prevTier = hitsRemaining;
            hitsRemaining--;
            if (hitsRemaining <= 0) {
                destroyed = true;
                showBroken = false;
                brokenTier = 0;
            } else {
                showBroken = prevTier >= 2;
                brokenTier = prevTier;
            }
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getHitsRemaining() {
        return hitsRemaining;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, GameConfig.BLOCK_WIDTH, GameConfig.BLOCK_HEIGHT);
    }

    @Override
    public boolean isActive() {
        return !destroyed;
    }
}
