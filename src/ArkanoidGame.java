import game.GamePanel;
import game.MultiplayerPanel;
import ui.MenuPanel;
import ui.InstructionsPanel;
import ui.SaveListPanel;
import utils.GameConfig;
import function.SaveManager;
import function.GameState;
import java.nio.file.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class ArkanoidGame {
    
    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";
    private static final String CARD_INSTRUCTIONS = "instructions";

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

            // Tạo panel hướng dẫn (ban đầu tạo sẵn để điều hướng)
            InstructionsPanel instructionsPanel = new InstructionsPanel();
            cards.add(instructionsPanel, CARD_INSTRUCTIONS);

            // Lắng nghe nút Chơi: mở lựa chọn chế độ (Solo hoặc Multiplayer)
            menu.getPlayButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String[] options = new String[]{"Solo", "Multiplayer"};
                    int choice = JOptionPane.showOptionDialog(
                            frame,
                            "Chọn chế độ chơi:",
                            "Chơi",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]
                    );

                    if (choice == 0) {
                        // Solo: dùng GamePanel trong cùng Frame (card)
                        GamePanel gamePanel = new GamePanel();
                        // Đăng ký listener để khi Game Over thì quay lại menu
                        gamePanel.setEventsListener(new GamePanel.GameEvents() {
                            @Override
                            public void onGameOver() {
                                // Xóa thẻ game hiện tại (để tránh giữ timer cũ)
                                cards.remove(gamePanel);
                                cardLayout.show(cards, CARD_MENU);
                                menu.requestFocusInWindow();
                            }
                        });

                        cards.add(gamePanel, CARD_GAME);
                        cardLayout.show(cards, CARD_GAME);
                        gamePanel.requestFocusInWindow();
                    } else if (choice == 1) {
                        // Multiplayer: mở cửa sổ mới chứa MultiplayerPanel (giữ menu tồn tại)
                        SwingUtilities.invokeLater(() -> {
                            JFrame mpFrame = new JFrame("Arkanoid - Multiplayer");
                            MultiplayerPanel mpPanel = new MultiplayerPanel();
                            mpFrame.add(mpPanel);
                            mpFrame.setSize(utils.GameConfig.SCREEN_WIDTH, utils.GameConfig.SCREEN_HEIGHT);
                            mpFrame.setResizable(false);
                            mpFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            mpFrame.setLocationRelativeTo(frame);
                            mpFrame.setVisible(true);
                            mpPanel.requestFocusInWindow();
                        });
                    } // else: user closed dialog or cancelled
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
                        saves = SaveManager.listSaves();
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
                        java.nio.file.Files.deleteIfExists(sel);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Không thể xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                    // refresh list
                    try {
                        java.util.List<java.nio.file.Path> saves2 = SaveManager.listSaves();
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
                                GamePanel gamePanel = new GamePanel();
                                try {
                                    GameState state = SaveManager.load(fileToLoad);
                                    gamePanel.applyGameState(state);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(frame, "Load save thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                                }
                                gamePanel.setEventsListener(new GamePanel.GameEvents() {
                                    @Override
                                    public void onGameOver() {
                                        cards.remove(gamePanel);
                                        cardLayout.show(cards, CARD_MENU);
                                        menu.requestFocusInWindow();
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
            frame.setVisible(true);

            // Hiển thị màn menu đầu tiên
            cardLayout.show(cards, CARD_MENU);
        });
    }
}