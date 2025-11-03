package entities;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Common contract for drawable game entities that expose simple bounds metadata.
 */
public interface GameObject {
    void draw(Graphics g);
    Rectangle getBounds();

    default boolean isActive() {
        return true;
    }
}
