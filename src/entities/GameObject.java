package entities;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Giao ước chung cho các thực thể game có thể vẽ được và cung cấp thông tin
 * về vùng biên (bounds) đơn giản.
 */
public interface GameObject {
    void draw(Graphics g);
    Rectangle getBounds();

    default boolean isActive() {
        return true;
    }
}
