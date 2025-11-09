import game.GamePanel;
import game.MultiplayerPanel;
import ui.MenuPanel;
import ui.StyledButton;
import ui.InstructionsPanel;
import ui.SaveListPanel;
import ui.RankingPanel;
import utils.GameConfig;
import utils.AudioManager;
import java.nio.file.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class ArkanoidGame {
    
    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";
    private static final String CARD_INSTRUCTIONS = "instructions";
    private static final String CARD_RANKING = "ranking";

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Arkanoid");
            
            // Sử dụng CardLayout để chuyển màn hình Menu/Game/Instructions
            CardLayout cardLayout = new CardLayout();
            JPanel cards = new JPanel(cardLayout);

            // Đường dẫn ảnh nền
            String backgroundPath = "images/arkanoid-background-intro.jpg";

            // Tạo MenuPanel
            MenuPanel menu = new MenuPanel(backgroundPath);
            menu.setHorizontalAlignment(MenuPanel.Alignment.LEFT);
            menu.setSideMargins(500, 12);
            menu.moveUp(230);
            cards.add(menu, CARD_MENU);

            // Tạo panel hướng dẫn 
            InstructionsPanel instructionsPanel = new InstructionsPanel();
            cards.add(instructionsPanel, CARD_INSTRUCTIONS);

            // Bảng xếp hạng
            RankingPanel rankingPanel = new RankingPanel();
            cards.add(rankingPanel, CARD_RANKING);

            // Nút Chơi: mở overlay chọn chế độ chơi
            menu.getPlayButton().addActionListener(e -> { if (e != null) { /* satisfy linter */ } menu.showModeSelection(); });

            // Xử lý lựa chọn chế độ từ overlay
            menu.setModeSelectionListener(mode -> {
                if ("solo".equals(mode)) {
                    // Solo: dùng GamePanel trong cùng Frame (card)
                    AudioManager.stop();
                    // Prompt player name using a custom styled dialog
                    String playerName = showPlayerNameDialog(frame);
                    if (playerName == null) {
                        // user cancelled -> back to menu, resume music
                        try { AudioManager.playLoop("music/screen.wav"); } catch (Throwable t) {}
                        return;
                    }

                    GamePanel gamePanel = new GamePanel();
                    gamePanel.setPlayerRunInfo(playerName, 0, 0, 0);
                    // Đăng ký listener để khi Game Over thì quay lại menu
                    gamePanel.setEventsListener(new GamePanel.GameEvents() {
                        @Override
                        public void onGameOver() {
                            // Xóa thẻ game hiện tại (để tránh giữ timer cũ)
                            cards.remove(gamePanel);
                            cardLayout.show(cards, CARD_MENU);
                            menu.requestFocusInWindow();
                            // Resume menu music
                            try { AudioManager.playLoop("music/screen.wav"); } catch (Throwable t) {}
                        }
                    });

                    cards.add(gamePanel, CARD_GAME);
                    cardLayout.show(cards, CARD_GAME);
                    gamePanel.requestFocusInWindow();
                } else if ("multiplayer".equals(mode)) {
                    // Multiplayer: mở cửa sổ mới chứa MultiplayerPanel (giữ menu tồn tại)
                    SwingUtilities.invokeLater(() -> {
                        AudioManager.stop();

                        JFrame mpFrame = new JFrame("Arkanoid - Multiplayer");
                        MultiplayerPanel mpPanel = new MultiplayerPanel();
                        mpFrame.add(mpPanel);
                        mpFrame.setSize(utils.GameConfig.SCREEN_WIDTH, utils.GameConfig.SCREEN_HEIGHT);
                        mpFrame.setResizable(false);
                        mpFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        mpFrame.setLocationRelativeTo(frame);
                        // When multiplayer window closes, resume menu music
                        mpFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosed(java.awt.event.WindowEvent e) {
                                try { AudioManager.playLoop("music/screen.wav"); } catch (Throwable t) {}
                            }
                        });
                        mpFrame.setVisible(true);
                        mpPanel.requestFocusInWindow();
                    });
                }
            });

            // Nút Tiếp tục: mở màn chọn bản save trên một màn hình riêng
            SaveListPanel saveListPanel = new SaveListPanel();
            cards.add(saveListPanel, "savelist");

            menu.getContinueButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    java.util.List<java.nio.file.Path> saves;
                    try {
                        saves = function.SaveController.listSaves();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Không thể liệt kê save: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (saves.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "Chưa có bản lưu nào.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    // Show up to 10 most recent saves in the list (practically limited)
                    java.util.List<java.nio.file.Path> top = saves.size() > 10 ? saves.subList(0, 10) : saves;
                    saveListPanel.setSaves(top);
                    cardLayout.show(cards, "savelist");
                    saveListPanel.requestFocusInWindow();
                }
            });

            // Back from save list -> menu
            saveListPanel.getBackButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cardLayout.show(cards, CARD_MENU);
                    menu.requestFocusInWindow();
                }
            });

            // Delete selected save
            saveListPanel.getDeleteButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Path sel = saveListPanel.getSelectedPath();
                    if (sel == null) return;
                    int ok = JOptionPane.showConfirmDialog(frame, "Bạn có chắc muốn xóa bản lưu này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                    if (ok != JOptionPane.YES_OPTION) return;
                    try {
                        function.SaveController.deleteSave(sel);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Không thể xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                    
                    try {
                        java.util.List<java.nio.file.Path> saves2 = function.SaveController.listSaves();
                        java.util.List<java.nio.file.Path> top2 = saves2.size() > 10 ? saves2.subList(0, 10) : saves2;
                        saveListPanel.setSaves(top2);
                    } catch (Exception ex) {
                        // ignore refresh error
                    }
                }
            });

            // Load selected save and resume after a 3s countdown
            saveListPanel.getLoadButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Path sel = saveListPanel.getSelectedPath();
                    if (sel == null) return;

                    // 3s countdown modal
                    JDialog dialog = new JDialog(frame, "Tiếp tục trò chơi", true);
                    JLabel label = new JLabel("Vào lại game sau 3s...", SwingConstants.CENTER);
                    label.setFont(new Font("Arial", Font.BOLD, 18));
                    dialog.getContentPane().add(label);
                    dialog.setSize(360, 130);
                    dialog.setLocationRelativeTo(frame);

                    Timer countdown = new Timer(1000, null);
                    final int[] remaining = {3};
                    java.nio.file.Path fileToLoad = sel;
                    countdown.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ev) {
                            remaining[0]--;
                            if (remaining[0] > 0) {
                                label.setText("Vào lại game sau " + remaining[0] + "s...");
                            } else {
                                countdown.stop();
                                dialog.dispose();
                                // Stop menu music when resuming saved game
                                AudioManager.stop();

                                GamePanel gamePanel = new GamePanel();
                                try {
                                    function.SaveController.loadAndApply(frame, gamePanel, fileToLoad);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(frame, "Load save thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                                }
                                gamePanel.setEventsListener(new GamePanel.GameEvents() {
                                    @Override
                                    public void onGameOver() {
                                        cards.remove(gamePanel);
                                        cardLayout.show(cards, CARD_MENU);
                                        menu.requestFocusInWindow();
                                        // Resume menu music (GamePanel already played lose.wav)
                                        try { AudioManager.playLoop("music/screen.wav"); } catch (Throwable t) {}
                                    }
                                });
                                cards.add(gamePanel, CARD_GAME);
                                cardLayout.show(cards, CARD_GAME);
                                gamePanel.requestFocusInWindow();
                            }
                        }
                    });

                    label.setText("Vào lại game sau 3s...");
                    countdown.start();
                    dialog.setVisible(true);
                }
            });

            // Lắng nghe nút Hướng dẫn
            menu.getInstructionsButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cardLayout.show(cards, CARD_INSTRUCTIONS);
                    instructionsPanel.requestFocusInWindow();
                }
            });

            // Nút Ranking
            menu.getRankingButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rankingPanel.refreshList();
                    cardLayout.show(cards, CARD_RANKING);
                }
            });
            rankingPanel.getBackButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cardLayout.show(cards, CARD_MENU);
                    menu.requestFocusInWindow();
                }
            });

            // Nút Quay lại trong màn Hướng dẫn
            instructionsPanel.getBackButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cardLayout.show(cards, CARD_MENU);
                    menu.requestFocusInWindow();
                }
            });

            frame.setContentPane(cards);
            frame.setSize(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            // Start background music (non-blocking). If JavaFX is not available,
            // MusicPlayer will print an error but the game will continue to run.
            try {
                AudioManager.init();
                // Use WAV (Java Sound) which works without JavaFX; user converted file to WAV
                AudioManager.playLoop("music/screen.wav");
            } catch (Throwable t) {
                System.err.println("Could not start background music: " + t.getMessage());
            }

            frame.setVisible(true);

            // Hiển thị màn menu đầu tiên
            cardLayout.show(cards, CARD_MENU);
        });
    }

    // Custom modal dialog for entering player name with nicer styling
    private static String showPlayerNameDialog(JFrame parent) {
        final String[] result = { null };
        JDialog dialog = new JDialog(parent, "1 Player - Nhập tên", true);

        JPanel content = new JPanel(new BorderLayout(10,10));
        content.setBorder(new javax.swing.border.EmptyBorder(12,12,12,12));
        content.setBackground(new Color(18, 18, 20));

        JLabel lbl = new JLabel("Nhập tên người chơi:");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));

        JTextField tf = new JTextField();
        tf.setColumns(18);
        tf.setFont(new Font("Arial", Font.PLAIN, 10));

        JPanel center = new JPanel(new BorderLayout(6,6));
        center.setOpaque(false);
        center.add(lbl, BorderLayout.NORTH);
        center.add(tf, BorderLayout.CENTER);

        StyledButton ok = new StyledButton("Chơi");
        StyledButton cancel = new StyledButton("Hủy");
        ok.setPreferredSize(new Dimension(100, 36));
        cancel.setPreferredSize(new Dimension(100, 36));
        // Reduce button label font size to better fit the dialog
        ok.setFont(ok.getFont().deriveFont(Font.PLAIN, 14f));
        cancel.setFont(cancel.getFont().deriveFont(Font.PLAIN, 14f));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(cancel);
        btns.add(ok);

        content.add(center, BorderLayout.CENTER);
        content.add(btns, BorderLayout.SOUTH);

        // Actions
        ok.addActionListener(e -> {
            String text = tf.getText();
            if (text != null) text = text.trim();
            if (text == null || text.isEmpty()) {
                tf.requestFocusInWindow();
                return;
            }
            result[0] = text;
            dialog.dispose();
        });
        cancel.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        tf.addActionListener(e -> ok.doClick()); // Enter triggers OK

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        tf.requestFocusInWindow();
        dialog.setVisible(true);
        return result[0];
    }
}