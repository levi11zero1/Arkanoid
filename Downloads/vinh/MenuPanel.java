import java.awt.*;
import java.io.File;
import javax.swing.*;

/**
 * Màn hình menu chính với hình nền, nút Chơi và Hướng dẫn.
 */
public class MenuPanel extends JPanel {

    private final StyledButton playButton = new StyledButton("Chơi");
    private final StyledButton instructionsButton = new StyledButton("Hướng dẫn");
    private Image backgroundImage;
    // Khối chứa tiêu đề + nút để dễ điều chỉnh vị trí
    private final JPanel vbox;
    private Component topSpacer;       // đệm phía trên để dịch chuyển theo trục dọc
    private Component betweenButtons;  // đệm giữa 2 nút

    // Lưu cấu hình căn lề và neo để re-apply khi thay đổi
    private int marginLeft = 0;
    private int marginRight = 0;
    private int marginTop = 0;
    private int marginBottom = 0;
    private Alignment hAlignState = Alignment.CENTER;
    private Vertical vAlignState = Vertical.CENTER;

    public enum Alignment { LEFT, CENTER, RIGHT }
    public enum Vertical { TOP, CENTER, BOTTOM }

    public MenuPanel(String backgroundPath) {
        setLayout(new GridBagLayout());

        // Tải ảnh nền nếu có, nếu không sẽ dùng nền màu.
        if (backgroundPath != null) {
            File f = new File(backgroundPath);
            if (f.exists() && f.isFile()) {
                backgroundImage = new ImageIcon(backgroundPath).getImage();
            } else {
                // thử load từ resource (nếu đóng gói)
                try {
                    java.net.URL url = getClass().getResource("/" + backgroundPath);
                    if (url != null) {
                        backgroundImage = new ImageIcon(url).getImage();
                    }
                } catch (Throwable ignored) {}
            }
        }

        // Tùy chỉnh nút
        Dimension btnSize = new Dimension(200, 44);
        playButton.setPreferredSize(btnSize);
        instructionsButton.setPreferredSize(btnSize);
        playButton.setCornerRadius(20);
        instructionsButton.setCornerRadius(20);

    // Container dọc cho các nút
    vbox = new JPanel();
    vbox.setOpaque(false);
    vbox.setLayout(new BoxLayout(vbox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 32f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionsButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Spacer trên cùng để điều chỉnh vị trí khối theo trục dọc
        topSpacer = Box.createVerticalStrut(0);
        vbox.add(topSpacer);

        vbox.add(title);
        vbox.add(Box.createVerticalStrut(24));
        vbox.add(playButton);
        // Spacer giữa hai nút để điều chỉnh khoảng cách
        betweenButtons = Box.createVerticalStrut(24);
        vbox.add(betweenButtons);
        vbox.add(instructionsButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0; // cho phép neo theo anchor
        gbc.anchor = computeAnchor();
        gbc.insets = new Insets(marginTop, marginLeft, marginBottom, marginRight);
        add(vbox, gbc);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        // Vẽ nền
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            // phủ lớp mờ để chữ rõ hơn
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else {
            // Nền chuyển sắc khi không có ảnh
            GradientPaint gp = new GradientPaint(0, 0, new Color(36, 49, 77), 0, getHeight(), new Color(18, 25, 38));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }

    public JButton getPlayButton() { return playButton; }
    public JButton getInstructionsButton() { return instructionsButton; }
    // getters kiểu StyledButton để tiện tùy biến màu sắc/hình dạng
    public StyledButton getPlayStyledButton() { return playButton; }
    public StyledButton getInstructionsStyledButton() { return instructionsButton; }

    // ====== API điều chỉnh vị trí ======
    /**
     * Dịch chuyển khối tiêu đề + nút xuống dưới (px). Giá trị âm để đẩy lên.
     */
    public void setTopOffset(int pixels) {
        if (pixels < 0) pixels = 0; // Box.createVerticalStrut không nhận giá trị âm
        // thay thế spacer cũ bằng spacer mới và cập nhật tham chiếu
        int index = -1;
        for (int i = 0; i < vbox.getComponentCount(); i++) {
            if (vbox.getComponent(i) == topSpacer) { index = i; break; }
        }
        if (index == -1) index = 0; // dự phòng nếu không tìm thấy
        vbox.remove(index);
        topSpacer = Box.createVerticalStrut(pixels);
        vbox.add(topSpacer, index);
        vbox.revalidate();
        vbox.repaint();
    }

    /**
     * Điều chỉnh khoảng cách dọc giữa nút Chơi và Hướng dẫn.
     */
    public void setButtonsSpacing(int pixels) {
        if (pixels < 0) pixels = 0;
        // tìm vị trí betweenButtons trong vbox
        int count = vbox.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (vbox.getComponent(i) == betweenButtons) {
                vbox.remove(i);
                betweenButtons = Box.createVerticalStrut(pixels);
                vbox.add(betweenButtons, i);
                break;
            }
        }
        vbox.revalidate();
        vbox.repaint();
    }

    /**
     * Căn trái/giữa/phải cho khối nút + tiêu đề.
     */
    public void setHorizontalAlignment(Alignment alignment) {
        if (alignment == null) return;
        hAlignState = alignment;
        reapplyConstraints();
    }

    /**
     * Căn theo trục dọc: TOP/CENTER/BOTTOM.
     */
    public void setVerticalAlignment(Vertical vertical) {
        if (vertical == null) return;
        vAlignState = vertical;
        reapplyConstraints();
    }

    /**
     * Điều chỉnh khoảng cách với mép trái/phải (đơn vị px).
     */
    public void setSideMargins(int left, int right) {
        if (left < 0) left = 0;
        if (right < 0) right = 0;
        this.marginLeft = left;
        this.marginRight = right;
        reapplyConstraints();
    }

    /**
     * Đặt lề trên/dưới (px) để tinh chỉnh vị trí theo trục dọc.
     */
    public void setTopBottomMargins(int top, int bottom) {
        if (top < 0) top = 0;
        if (bottom < 0) bottom = 0;
        this.marginTop = top;
        this.marginBottom = bottom;
        reapplyConstraints();
    }

    /**
     * Đẩy khối nút lên trên: neo TOP và đặt lề trên.
     */
    public void moveUp(int topMarginPixels) {
        if (topMarginPixels < 0) topMarginPixels = 0;
        setVerticalAlignment(Vertical.TOP);
        setTopBottomMargins(topMarginPixels, 0);
    }

    private void reapplyConstraints() {
        // Gỡ và add lại vbox với anchor + insets hiện tại
        remove(vbox);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.anchor = computeAnchor();
        gbc.insets = new Insets(marginTop, marginLeft, marginBottom, marginRight);
        add(vbox, gbc);
        revalidate();
        repaint();
    }

    private int computeAnchor() {
        switch (vAlignState) {
            case TOP:
                switch (hAlignState) {
                    case LEFT: return GridBagConstraints.NORTHWEST;
                    case CENTER: return GridBagConstraints.NORTH;
                    case RIGHT: return GridBagConstraints.NORTHEAST;
                }
                break;
            case CENTER:
                switch (hAlignState) {
                    case LEFT: return GridBagConstraints.WEST;
                    case CENTER: return GridBagConstraints.CENTER;
                    case RIGHT: return GridBagConstraints.EAST;
                }
                break;
            case BOTTOM:
                switch (hAlignState) {
                    case LEFT: return GridBagConstraints.SOUTHWEST;
                    case CENTER: return GridBagConstraints.SOUTH;
                    case RIGHT: return GridBagConstraints.SOUTHEAST;
                }
                break;
        }
        return GridBagConstraints.CENTER;
    }
}
