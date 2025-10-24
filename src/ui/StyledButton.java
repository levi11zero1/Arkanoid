package ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * Nút tùy biến: bo góc, màu chủ đạo, hiệu ứng hover/nhấn, con trỏ tay.
 */
public class StyledButton extends JButton {
    private Color bg = new Color(0x282B88);        // màu nền mặc định (primary)
    private Color fg = Color.BLACK;                // màu chữ
    private Color bgHover = new Color(0x3793F0);   // khi hover
    private Color bgPress = new Color(0x2576C4);   // khi nhấn
    private Color border = new Color(0x282B88);    // viền
    private int cornerRadius = 18;
    // old boolean hovered kept for compatibility removed; animation uses hoverProgress
    // animated hover progress 0..1 for smooth scale/shadow effect
    private float hoverProgress = 0f;
    private javax.swing.Timer hoverTimer;
    private float hoverTarget = 0f;

    public StyledButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(fg);
        setFont(getFont().deriveFont(Font.BOLD, 24f));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(10, 20, 10, 20));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                startHoverAnimation(1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                startHoverAnimation(0f);
            }
        });
    }

    private void startHoverAnimation(float target) {
        hoverTarget = target;
        if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
        hoverTimer = new javax.swing.Timer(16, e -> {
            float step = 0.12f; // animation speed
            if (hoverProgress < hoverTarget) {
                hoverProgress = Math.min(hoverTarget, hoverProgress + step);
            } else if (hoverProgress > hoverTarget) {
                hoverProgress = Math.max(hoverTarget, hoverProgress - step);
            }
            repaint();
            if (hoverProgress == hoverTarget) {
                hoverTimer.stop();
            }
        });
        hoverTimer.setRepeats(true);
        hoverTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // animated scale (subtle)
        float scale = 1f + 0.04f * hoverProgress;
        int cx = w / 2;
        int cy = h / 2;
        g2.translate(cx, cy);
        g2.scale(scale, scale);
        g2.translate(-cx, -cy);

        // Chọn màu nền dựa trên trạng thái (pressed overrides hover)
        Color currentBg;
        ButtonModel m = getModel();
        if (m.isPressed()) {
            currentBg = bgPress;
        } else if (hoverProgress > 0.01f) {
            // blend between bg and bgHover by hoverProgress
            currentBg = blend(bg, bgHover, hoverProgress);
        } else {
            currentBg = bg;
        }

        // Drop shadow (subtle, scales with hover)
        float shadowAlpha = 0.18f * hoverProgress;
        if (shadowAlpha > 0f) {
            g2.setColor(new Color(0f,0f,0f, shadowAlpha));
            g2.fill(new RoundRectangle2D.Float(2, 4, w - 4, h - 4, cornerRadius, cornerRadius));
        }

        // Vẽ nền bo góc
        g2.setColor(currentBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        // Vẽ viền
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));

        // Draw the text using transformed graphics so it scales with the button
        super.paintComponent(g2);
        g2.dispose();
    }

    private Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = a.getRed(), ag = a.getGreen(), ab = a.getBlue(), aa = a.getAlpha();
        int br = b.getRed(), bgc = b.getGreen(), bb = b.getBlue(), ba = b.getAlpha();
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bgc - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return new Color(r, g, bl, al);
    }

    // === GETTERS / SETTERS ===
    public void setBackgroundColor(Color bg) {
        this.bg = bg;
        repaint();
    }

    public void setHoverColor(Color bgHover) {
        this.bgHover = bgHover;
    }

    public void setPressColor(Color bgPress) {
        this.bgPress = bgPress;
    }

    public void setBorderColor(Color border) {
        this.border = border;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }
}
