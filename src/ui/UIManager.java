package ui;

import function.LifeManager;
import function.Pause;
import function.SaveController;
import game.GamePanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class UIManager {
    private static final String LIFE_ICON_PATH = "images/life.png";
    private static final int LIFE_ICON_SIZE = 28;
    private static final int LIFE_ICON_SPACING = 6;
    private static final int HUD_MARGIN = 10;

    private Image lifeIcon;
    private boolean triedLoadLifeIcon;

    public void renderOverlay(Graphics2D g, GamePanel panel) {
        if (g == null || panel == null) {
            return;
        }

        int lives = Math.max(0, panel.getLives());
        int baselineLives = Math.max(1, LifeManager.getDefaultLives());
        int iconCount = Math.max(baselineLives, lives);
        if (iconCount <= 0) {
            return;
        }

        java.awt.Font originalFont = g.getFont();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Image icon = getLifeIcon();
        if (icon == null) {
            // draw fallback lives box
            drawLivesFallback(g, lives, panel);
        } else {
            String label = "x " + lives;
            int labelWidth = g.getFontMetrics().stringWidth(label);
            int iconAreaWidth = iconCount * LIFE_ICON_SIZE + (iconCount - 1) * LIFE_ICON_SPACING;
            int boxWidth = iconAreaWidth + LIFE_ICON_SPACING + labelWidth + 16;
            int boxHeight = LIFE_ICON_SIZE + 16;

            int boxX = Math.max(0, panel.getWidth() - boxWidth - HUD_MARGIN);
            int boxY = Math.max(0, panel.getHeight() - boxHeight - HUD_MARGIN);

            Color bg = new Color(0, 0, 0, 150);
            Color border = new Color(255, 255, 255, 110);
            Color textColor = Color.WHITE;

            g.setColor(bg);
            g.fillRoundRect(boxX - 4, boxY - 4, boxWidth, boxHeight, 14, 14);
            g.setColor(border);
            g.drawRoundRect(boxX - 4, boxY - 4, boxWidth, boxHeight, 14, 14);

            int iconY = boxY + (boxHeight - 16 - LIFE_ICON_SIZE) / 2 + 4;
            int iconX = boxX + 8;

            java.awt.Composite originalComposite = g.getComposite();

            for (int i = 0; i < iconCount; i++) {
                boolean active = i < lives;
                if (!active) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                } else {
                    g.setComposite(originalComposite);
                }
                g.drawImage(icon, iconX + i * (LIFE_ICON_SIZE + LIFE_ICON_SPACING), iconY,
                        LIFE_ICON_SIZE, LIFE_ICON_SIZE, null);
            }

            g.setComposite(originalComposite);
            g.setColor(textColor);
            int textX = iconX + iconAreaWidth + LIFE_ICON_SPACING;
            int textY = iconY + LIFE_ICON_SIZE - g.getFontMetrics().getDescent();
            g.drawString(label, textX, textY);

            g.setFont(originalFont);
        }

        try {
            long lastLost = LifeManager.getLastLifeLostAtMs();
            String msg = LifeManager.getLastLifeLostMessage();
            int duration = LifeManager.getLifeLostMessageDurationMs();
            if (msg != null && !msg.isBlank() && lastLost > 0) {
                long elapsed = System.currentTimeMillis() - lastLost;
                if (elapsed <= duration) {
                    float t = Math.min(1.0f, Math.max(0f, (float) elapsed / (float) duration));
                    float alpha = 1.0f - t;
                    alpha = Math.max(0.08f, alpha); 

                    java.awt.Font msgFont = originalFont.deriveFont(java.awt.Font.BOLD, Math.max(40f, panel.getWidth() / 48f));
                    g.setFont(msgFont);
                    java.awt.FontMetrics fm = g.getFontMetrics(msgFont);
                    int textWidth = fm.stringWidth(msg);
                    int textHeight = fm.getHeight();

                    int boxWidth = textWidth + 16;
                    int boxHeight = textHeight + 12;
                    int boxX = Math.max(0, panel.getWidth() / 2 - boxWidth / 2);
                    int boxY = Math.max(0, panel.getHeight() / 2 - boxHeight / 2);

                    java.awt.Composite old = g.getComposite();
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                    g.setColor(new Color(0, 0, 0, 160));
                    g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 12, 12);

                    g.setColor(new Color(255, 255, 255, 120));
                    g.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 12, 12);

                    int tx = boxX + 8;
                    int ty = boxY + fm.getAscent() + (boxHeight - textHeight) / 2;

                    g.setColor(new Color(0, 0, 0, (int) (200 * alpha)));
                    g.drawString(msg, tx + 2, ty + 2);
                    g.setColor(new Color(255, 230, 120));
                    g.drawString(msg, tx, ty);

                    g.setComposite(old);
                    g.setFont(originalFont);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private Image getLifeIcon() {
        if (!triedLoadLifeIcon) {
            triedLoadLifeIcon = true;
            lifeIcon = loadImage(LIFE_ICON_PATH);
        }
        return lifeIcon;
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            URL resource = UIManager.class.getClassLoader().getResource(path);
            if (resource != null) {
                ImageIcon icon = new ImageIcon(resource);
                return icon.getImage();
            }
            File file = new File(path);
            if (file.exists()) {
                return ImageIO.read(file);
            }
        } catch (IOException ignored) {
            // fall through
        }
        return null;
    }

    private void drawLivesFallback(Graphics2D g, int lives, GamePanel panel) {
        String text = "Lives: " + lives;
        int textWidth = g.getFontMetrics().stringWidth(text);
        int textHeight = g.getFontMetrics().getHeight();
        int boxWidth = textWidth + 24;
        int boxHeight = textHeight + 16;
        int boxX = Math.max(0, panel.getWidth() - boxWidth - HUD_MARGIN);
        int boxY = Math.max(0, panel.getHeight() - boxHeight - HUD_MARGIN);

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 12, 12);
        g.setColor(new Color(255, 255, 255, 110));
        g.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 12, 12);

        int textX = boxX + 8;
        int textY = boxY + boxHeight - g.getFontMetrics().getDescent() - 4;
        g.setColor(Color.WHITE);
        g.drawString(text, textX, textY);
    }

    public StyledButton createSaveButton(GamePanel panel, Consumer<Boolean> onSaved) {
    final StyledButton saveButton = new StyledButton("Save");
    // Nút Save nằm trên màn hình chơi — dùng cỡ chữ nhỏ hơn để tránh tràn
    saveButton.setFont(saveButton.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        saveButton.setToolTipText("Lưu game");

        final int btnW = 90, btnH = 34;
        final int rightMargin = 100;
        final int topMargin = 10;
        int baseW = panel.getWidth() > 0 ? panel.getWidth() : panel.getWidth();
        int x = Math.max(0, baseW - btnW - rightMargin);
        saveButton.setBounds(x, topMargin, btnW, btnH);

        panel.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int newX = panel.getWidth() - btnW - rightMargin;
                saveButton.setLocation(Math.max(0, newX), topMargin);
            }
        });

        SwingUtilities.invokeLater(() -> {
            int newX = panel.getWidth() - btnW - rightMargin;
            saveButton.setLocation(Math.max(0, newX), topMargin);
        });

        saveButton.addActionListener(ev -> {
            boolean saved = false;
            try {
                try { Pause.getInstance().pause(); } catch (Throwable ignored) {}

                saved = SaveController.promptAndSave(panel, panel);
            } catch (Throwable t) {
            } finally {
                if (!saved) {
                    try { Pause.getInstance().resume(); } catch (Throwable ignored) {}
                }
            }
            if (onSaved != null) onSaved.accept(saved);
        });

        panel.add(saveButton);
        return saveButton;
    }

    public int showConfirm(Component parent, String title, String message, int optionType) {
        return JOptionPane.showConfirmDialog(parent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
    }

    public String promptInput(Component parent, String title, String message) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
    }

    public void showMessage(Component parent, String title, String message, int messageType) {
        JOptionPane.showMessageDialog(parent, message, title, messageType);
    }
}
