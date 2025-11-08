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
    private final StyledButton rankingButton = new StyledButton("Bảng xếp hạng");
    private final StyledButton instructionsButton = new StyledButton("Hướng dẫn");

    private final StyledButton skinButton = new StyledButton("Chọn skin");
    private final JPanel skinOptionsPanel = new JPanel();
    private final StyledButton chooseBallSkinButton = new StyledButton("Chọn Ball Skin");
    private final StyledButton choosePaddleSkinButton = new StyledButton("Chọn Paddle Skin");
    private Image backgroundImage;
    // Khối chứa tiêu đề + nút để dễ điều chỉnh vị trí
    private final JPanel vbox;
    private Component topSpacer; // đệm phía trên để dịch chuyển theo trục dọc
    private Component betweenButtons; // đệm giữa 2 nút

    // Lưu cấu hình căn lề và neo để re-apply khi thay đổi
    private int marginLeft = 0;
    private int marginRight = 0;
    private int marginTop = 0;
    private int marginBottom = 0;
    private Alignment hAlignState = Alignment.CENTER;
    private Vertical vAlignState = Vertical.CENTER;

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    public enum Vertical {
        TOP, CENTER, BOTTOM
    }

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
                } catch (Throwable ignored) {
                }
            }
        }

        // Tùy chỉnh nút
        Dimension btnSize = new Dimension(200, 44);
        playButton.setPreferredSize(btnSize);
        instructionsButton.setPreferredSize(btnSize);
        continueButton.setPreferredSize(btnSize);
        rankingButton.setPreferredSize(btnSize);
        playButton.setCornerRadius(20);
        instructionsButton.setCornerRadius(20);
        continueButton.setCornerRadius(20);
        rankingButton.setCornerRadius(20);

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
        rankingButton.setAlignmentX(Component.CENTER_ALIGNMENT);

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
        vbox.add(rankingButton);
        vbox.add(Box.createVerticalStrut(16));
        vbox.add(instructionsButton);


        skinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        skinButton.setPreferredSize(btnSize);
        skinButton.setCornerRadius(20);
        vbox.add(Box.createVerticalStrut(12));
        vbox.add(skinButton);


        skinOptionsPanel.setOpaque(false);
        skinOptionsPanel.setLayout(new BoxLayout(skinOptionsPanel, BoxLayout.Y_AXIS));
        chooseBallSkinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        choosePaddleSkinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooseBallSkinButton.setPreferredSize(new Dimension(180, 36));
        choosePaddleSkinButton.setPreferredSize(new Dimension(180, 36));
        chooseBallSkinButton.setCornerRadius(14);
        choosePaddleSkinButton.setCornerRadius(14);

        skinOptionsPanel.add(Box.createVerticalStrut(8));
        skinOptionsPanel.add(chooseBallSkinButton);
        skinOptionsPanel.add(Box.createVerticalStrut(6));
        skinOptionsPanel.add(choosePaddleSkinButton);
        skinOptionsPanel.setVisible(false);
        vbox.add(skinOptionsPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0; // cho phép neo theo anchor
        gbc.anchor = computeAnchor();
        gbc.insets = new Insets(marginTop, marginLeft, marginBottom, marginRight);
        add(vbox, gbc);


        initSkinSelection();

        // Mode selection overlay (hidden by default). Splits the panel into two big
        // clickable halves.
        initModeSelectionOverlay();
    }

    // -------- Mode selection UI --------
    private JPanel modeOverlay;
    private JPanel topModePanel;
    private JPanel bottomModePanel;
    private ModeSelectionListener modeListener;

    // Small helper panel that draws a background image and a translucent hover
    // overlay
    private class ModePanel extends JPanel {
        private final Image bg;
        private boolean hovered = false;

        ModePanel(Image bg) {
            super(new GridBagLayout());
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (bg != null) {
                g2.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            } else {
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            if (hovered) {
                Color hoverTint = new Color(255, 255, 255, (int) (0.12f * 255));
                g2.setColor(hoverTint);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }

        void setHovered(boolean h) {
            if (this.hovered != h) {
                this.hovered = h;
                repaint();
            }
        }
    }

    public interface ModeSelectionListener {
        void onModeSelected(String mode); // "solo" or "multiplayer"
    }

    private void initModeSelectionOverlay() {
        modeOverlay = new JPanel(new GridLayout(2, 1));
        modeOverlay.setOpaque(true);
        modeOverlay.setBackground(Color.BLACK);

        // Set base backgrounds for each half as requested (use image files)
        topModePanel = createModeHalf("1 Player", "", "images/bg_1player.png");
        bottomModePanel = createModeHalf("2 Player", "", "images/bg_2player.png");

        modeOverlay.add(topModePanel);
        modeOverlay.add(bottomModePanel);

        modeOverlay.setVisible(false);
        // add on top (same GridBag position as vbox)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(modeOverlay, gbc);
    }

    private JPanel createModeHalf(String titleText, String subtitle, String bgImagePath) {
        // Load background image if available (make final for inner usage)
        Image tmpImg = null;
        if (bgImagePath != null) {
            File f = new File(bgImagePath);
            if (f.exists() && f.isFile())
                tmpImg = new ImageIcon(bgImagePath).getImage();
            else {
                try {
                    java.net.URL url = getClass().getResource("/" + bgImagePath);
                    if (url != null)
                        tmpImg = new ImageIcon(url).getImage();
                } catch (Throwable ignored) {
                }
            }
        }
        final Image img = tmpImg;

        ModePanel p = new ModePanel(img);

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
        p.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (modeListener != null) {
                    if (titleText.startsWith("1"))
                        modeListener.onModeSelected("solo");
                    else
                        modeListener.onModeSelected("multiplayer");
                }
                hideModeSelection();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                p.setHovered(true);
                title.setForeground(new Color(255, 235, 120));
                title.setFont(titleHoverFont);
                sub.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                p.setHovered(false);
                title.setForeground(Color.WHITE);
                title.setFont(titleFont);
                sub.setForeground(Color.LIGHT_GRAY);
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

    public void setModeSelectionListener(ModeSelectionListener l) {
        this.modeListener = l;
    }

    private Timer skinAnimTimer;
    private int skinAnimTarget = 0;
    private int skinAnimCurrent = 0;
    private final int SKIN_ANIM_STEP = 12; // px per frame

    private void initSkinSelection() {

        skinOptionsPanel.setVisible(false);
        skinOptionsPanel.setPreferredSize(new Dimension(200, 0));

        skinButton.addActionListener(e -> toggleSkinOptions());

        chooseBallSkinButton.addActionListener(e -> {
            try {
                entities.SkinManager.selectBallSkin();
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở trình chọn skin bóng: " + ex.getMessage());
            }
        });

        choosePaddleSkinButton.addActionListener(e -> {
            try {
                entities.SkinManager.selectPaddleSkin();
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở trình chọn skin paddle: " + ex.getMessage());
            }
        });
    }

    private void toggleSkinOptions() {
        if (skinAnimTimer != null && skinAnimTimer.isRunning()) {
            return; 
        }
        final int expandedHeight = 90; 
        if (!skinOptionsPanel.isVisible() || skinAnimCurrent == 0) {

            skinOptionsPanel.setVisible(true);
            skinAnimTarget = expandedHeight;
        } else {
            skinAnimTarget = 0;
        }

        skinAnimTimer = new Timer(15, null);
        skinAnimTimer.addActionListener(evt -> {
            if (skinAnimCurrent < skinAnimTarget) {
                skinAnimCurrent = Math.min(skinAnimTarget, skinAnimCurrent + SKIN_ANIM_STEP);
            } else if (skinAnimCurrent > skinAnimTarget) {
                skinAnimCurrent = Math.max(skinAnimTarget, skinAnimCurrent - SKIN_ANIM_STEP);
            }
            skinOptionsPanel.setPreferredSize(new Dimension(200, skinAnimCurrent));
            skinOptionsPanel.revalidate();
            skinOptionsPanel.repaint();
            if (skinAnimCurrent == skinAnimTarget) {
                skinAnimTimer.stop();
                if (skinAnimTarget == 0) {
                    skinOptionsPanel.setVisible(false);
                }
            }
        });
        skinAnimTimer.start();
    }


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
            } catch (Throwable ignored) {
            }
        }

        // Try known pixel font family name (might be installed)
        try {
            Font test = new Font("PressStart2P", Font.PLAIN, (int) size);
            if (!"Dialog".equals(test.getFamily()))
                return test.deriveFont(size);
        } catch (Throwable ignored) {
        }

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

    public JButton getPlayButton() {
        return playButton;
    }

    public JButton getContinueButton() {
        return continueButton;
    }

    public JButton getInstructionsButton() {
        return instructionsButton;
    }

    // getters kiểu StyledButton để tiện tùy biến màu sắc/hình dạng
    public StyledButton getPlayStyledButton() {
        return playButton;
    }

    public StyledButton getContinueStyledButton() {
        return continueButton;
    }

    public StyledButton getInstructionsStyledButton() {
        return instructionsButton;
    }

    public JButton getRankingButton() {
        return rankingButton;
    }

    public StyledButton getRankingStyledButton() {
        return rankingButton;
    }

    // ====== API điều chỉnh vị trí ======
    /**
     * Dịch chuyển khối tiêu đề + nút xuống dưới (px). Giá trị âm để đẩy lên.
     */
    public void setTopOffset(int pixels) {
        if (pixels < 0)
            pixels = 0; // Box.createVerticalStrut không nhận giá trị âm
        // thay thế spacer cũ bằng spacer mới và cập nhật tham chiếu
        int index = -1;
        for (int i = 0; i < vbox.getComponentCount(); i++) {
            if (vbox.getComponent(i) == topSpacer) {
                index = i;
                break;
            }
        }
        if (index == -1)
            index = 0; // dự phòng nếu không tìm thấy
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
        if (pixels < 0)
            pixels = 0;
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
        if (alignment == null)
            return;
        hAlignState = alignment;
        reapplyConstraints();
    }

    /**
     * Căn theo trục dọc: TOP/CENTER/BOTTOM.
     */
    public void setVerticalAlignment(Vertical vertical) {
        if (vertical == null)
            return;
        vAlignState = vertical;
        reapplyConstraints();
    }

    /**
     * Điều chỉnh khoảng cách với mép trái/phải (đơn vị px).
     */
    public void setSideMargins(int left, int right) {
        if (left < 0)
            left = 0;
        if (right < 0)
            right = 0;
        this.marginLeft = left;
        this.marginRight = right;
        reapplyConstraints();
    }

    /**
     * Đặt lề trên/dưới (px) để tinh chỉnh vị trí theo trục dọc.
     */
    public void setTopBottomMargins(int top, int bottom) {
        if (top < 0)
            top = 0;
        if (bottom < 0)
            bottom = 0;
        this.marginTop = top;
        this.marginBottom = bottom;
        reapplyConstraints();
    }

    /**
     * Đẩy khối nút lên trên: neo TOP và đặt lề trên.
     */
    public void moveUp(int topMarginPixels) {
        if (topMarginPixels < 0)
            topMarginPixels = 0;
        setVerticalAlignment(Vertical.TOP);
        setTopBottomMargins(topMarginPixels, 0);
    }

    private void reapplyConstraints() {
        // Gỡ và add lại vbox với anchor + insets hiện tại
        remove(vbox);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
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
                    case LEFT:
                        return GridBagConstraints.NORTHWEST;
                    case CENTER:
                        return GridBagConstraints.NORTH;
                    case RIGHT:
                        return GridBagConstraints.NORTHEAST;
                }
                break;
            case CENTER:
                switch (hAlignState) {
                    case LEFT:
                        return GridBagConstraints.WEST;
                    case CENTER:
                        return GridBagConstraints.CENTER;
                    case RIGHT:
                        return GridBagConstraints.EAST;
                }
                break;
            case BOTTOM:
                switch (hAlignState) {
                    case LEFT:
                        return GridBagConstraints.SOUTHWEST;
                    case CENTER:
                        return GridBagConstraints.SOUTH;
                    case RIGHT:
                        return GridBagConstraints.SOUTHEAST;
                }
                break;
        }
        return GridBagConstraints.CENTER;
    }
}
