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
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(fg);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(getFont().deriveFont(Font.BOLD, 16f));
        setMargin(new Insets(8, 16, 8, 16));

        // Lắng nghe hover
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }

    public void setColors(Color background, Color hover, Color press, Color foreground, Color borderColor) {
        if (background != null) this.bg = background;
        if (hover != null) this.bgHover = hover;
        if (press != null) this.bgPress = press;
        if (foreground != null) { this.fg = foreground; setForeground(foreground); }
        if (borderColor != null) this.border = borderColor;
        repaint();
    }

    public void setCornerRadius(int r) {
        this.cornerRadius = Math.max(0, r);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Chọn màu theo trạng thái
        ButtonModel model = getModel();
        Color fill = bg;
        if (model.isArmed() || model.isPressed()) fill = bgPress;
        else if (hovered) fill = bgHover;

        // Đổ bóng nhẹ
        g2.setComposite(AlphaComposite.SrcOver.derive(0.25f));
        Shape shadow = new RoundRectangle2D.Float(2, 3, w - 4, h - 4, cornerRadius, cornerRadius);
        g2.setColor(Color.BLACK);
        g2.fill(shadow);

        // Nền bo góc
        g2.setComposite(AlphaComposite.SrcOver);
        Shape rr = new RoundRectangle2D.Float(0, 0, w - 3, h - 3, cornerRadius, cornerRadius);
        g2.setColor(fill);
        g2.fill(rr);

        // Viền
        g2.setColor(border);
        g2.draw(rr);

        g2.dispose();
        // Vẽ text/icon mặc định (nền đã vẽ tay)
        super.paintComponent(g);
    }
}
