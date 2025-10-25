package ui;

import java.awt.*;
import java.io.File;
import javax.swing.*;

/**
 * Màn hình menu chính với hình nền, nút Chơi và Hướng dẫn.
 */
public class MenuPanel extends JPanel {

    private final StyledButton playButton = new StyledButton("Chơi");
    private final StyledButton continueButton = new StyledButton("Tiếp tục");
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
    continueButton.setPreferredSize(btnSize);
        playButton.setCornerRadius(20);
        instructionsButton.setCornerRadius(20);
    continueButton.setCornerRadius(20);

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
    continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Spacer trên cùng để điều chỉnh vị trí khối theo trục dọc
        topSpacer = Box.createVerticalStrut(0);
        vbox.add(topSpacer);

        vbox.add(title);
        vbox.add(Box.createVerticalStrut(24));
    vbox.add(playButton);
    // Spacer giữa các nút
    betweenButtons = Box.createVerticalStrut(16);
    vbox.add(betweenButtons);
    vbox.add(continueButton);
    vbox.add(Box.createVerticalStrut(16));
    vbox.add(instructionsButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0; // cho phép neo theo anchor
        gbc.anchor = computeAnchor();
        gbc.insets = new Insets(marginTop, marginLeft, marginBottom, marginRight);
        add(vbox, gbc);

        // Mode selection overlay (hidden by default). Splits the panel into two big clickable halves.
        initModeSelectionOverlay();
    }

    // -------- Mode selection UI --------
    private JPanel modeOverlay;
    private JPanel topModePanel;
    private JPanel bottomModePanel;
    private ModeSelectionListener modeListener;

    public interface ModeSelectionListener {
        void onModeSelected(String mode); // "solo" or "multiplayer"
    }

    private void initModeSelectionOverlay() {
    modeOverlay = new JPanel(new GridLayout(2,1));
    modeOverlay.setOpaque(true);
    modeOverlay.setBackground(Color.BLACK);

    // Set base backgrounds for each half as requested
    topModePanel = createModeHalf("1 Player", "", new Color(0x53, 0x53, 0x53)); // #535353
    bottomModePanel = createModeHalf("2 Player", "", new Color(0x76, 0x76, 0x76)); // #767676

        modeOverlay.add(topModePanel);
        modeOverlay.add(bottomModePanel);

        modeOverlay.setVisible(false);
        // add on top (same GridBag position as vbox)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(modeOverlay, gbc);
    }

    private JPanel createModeHalf(String titleText, String subtitle, Color overlay) {
        JPanel p = new JPanel(new GridBagLayout());
    p.setOpaque(true);
    p.setBackground(overlay);

        JLabel title = new JLabel(titleText);
        title.setForeground(Color.WHITE);
        Font titleFont = getPixelFont(28f).deriveFont(Font.PLAIN, 28f);
        Font titleHoverFont = titleFont.deriveFont(Font.BOLD, 32f);
        title.setFont(titleFont);

        JLabel sub = new JLabel(subtitle);
        sub.setForeground(Color.LIGHT_GRAY);
        sub.setFont(getPixelFont(12f).deriveFont(Font.PLAIN, 12f));

        Box box = Box.createVerticalBox();
        box.add(title);
        box.add(Box.createVerticalStrut(8));
        box.add(sub);

        p.add(box);

        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    // Hover effect: compute a hover tint from the base overlay color so it matches each half
    Color origBg = p.getBackground();
    Color hoverBg = blendColor(origBg, Color.WHITE, 0.12f);
        p.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (modeListener != null) {
                    if (titleText.startsWith("1")) modeListener.onModeSelected("solo");
                    else modeListener.onModeSelected("multiplayer");
                }
                hideModeSelection();
            }

            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                p.setBackground(hoverBg);
                title.setForeground(new Color(255, 235, 120));
                title.setFont(titleHoverFont);
                sub.setForeground(Color.WHITE);
                p.repaint();
            }

            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                p.setBackground(origBg);
                title.setForeground(Color.WHITE);
                title.setFont(titleFont);
                sub.setForeground(Color.LIGHT_GRAY);
                p.repaint();
            }
        });

        return p;
    }

    public void showModeSelection() {
        modeOverlay.setVisible(true);
        modeOverlay.requestFocusInWindow();
        // hide the standard vbox controls while selecting
        vbox.setVisible(false);
        revalidate();
        repaint();
    }

    public void hideModeSelection() {
        modeOverlay.setVisible(false);
        vbox.setVisible(true);
        revalidate();
        repaint();
    }

    public void setModeSelectionListener(ModeSelectionListener l) { this.modeListener = l; }

    // Try to load a pixel font from project `fonts/` folder or fall back to common names/monospaced.
    private Font getPixelFont(float size) {
        // Try project fonts folder first
        String[] candidates = new String[] { "fonts/PressStart2P-Regular.ttf", "fonts/pixel.ttf" };
        for (String c : candidates) {
            try {
                java.io.File f = new java.io.File(c);
                if (f.exists()) {
                    Font fo = Font.createFont(Font.TRUETYPE_FONT, f);
                    return fo.deriveFont(size);
                }
            } catch (Throwable ignored) {}
        }

        // Try known pixel font family name (might be installed)
        try {
            Font test = new Font("PressStart2P", Font.PLAIN, (int) size);
            if (!"Dialog".equals(test.getFamily())) return test.deriveFont(size);
        } catch (Throwable ignored) {}

        // Fallback to monospaced
        return new Font(Font.MONOSPACED, Font.PLAIN, (int) size);
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

    // Small color utility: blend two colors by t (0..1)
    private static Color blendColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = a.getRed(), ag = a.getGreen(), ab = a.getBlue(), aa = a.getAlpha();
        int br = b.getRed(), bg = b.getGreen(), bb = b.getBlue(), ba = b.getAlpha();
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return new Color(r, g, bl, al);
    }

    public JButton getPlayButton() { return playButton; }
    public JButton getContinueButton() { return continueButton; }
    public JButton getInstructionsButton() { return instructionsButton; }
    // getters kiểu StyledButton để tiện tùy biến màu sắc/hình dạng
    public StyledButton getPlayStyledButton() { return playButton; }
    public StyledButton getContinueStyledButton() { return continueButton; }
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
