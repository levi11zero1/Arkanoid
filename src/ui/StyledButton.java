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
    private boolean hovered = false;

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
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Chọn màu nền dựa trên trạng thái
        Color currentBg;
        ButtonModel model = getModel();
        if (model.isPressed()) {
            currentBg = bgPress;
        } else if (hovered) {
            currentBg = bgHover;
        } else {
            currentBg = bg;
        }

        // Vẽ nền bo góc
        g2.setColor(currentBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        // Vẽ viền
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));

        g2.dispose();

        // Vẽ text
        super.paintComponent(g);
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
