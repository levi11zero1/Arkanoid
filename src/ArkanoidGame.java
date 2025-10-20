import game.GamePanel;
import ui.MenuPanel;
import ui.InstructionsPanel;
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

            // Lắng nghe nút Chơi
            menu.getPlayButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Khi bấm Chơi, tạo GamePanel mới và chuyển sang thẻ game
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
                }
            });

            // Nút Tiếp tục: hiển thị 3 bản save gần nhất để chọn, sau đó đếm ngược 3s và vào game đã lưu
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

                    // Lấy tối đa 3 bản gần nhất
                    java.util.List<java.nio.file.Path> top = saves.size() > 3 ? saves.subList(0, 3) : saves;
                    String[] options = new String[top.size()];
                    for (int i = 0; i < top.size(); i++) {
                        java.nio.file.Path p = top.get(i);
                        String name = p.getFileName().toString();
                        // Hiển thị thêm thời gian chỉnh sửa
                        try {
                            long ts = java.nio.file.Files.getLastModifiedTime(p).toMillis();
                            java.time.Instant instant = java.time.Instant.ofEpochMilli(ts);
                            java.time.ZoneId zone = java.time.ZoneId.systemDefault();
                            java.time.LocalDateTime dt = java.time.LocalDateTime.ofInstant(instant, zone);
                            String when = dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            options[i] = name + "  (" + when + ")";
                        } catch (Exception ex) {
                            options[i] = name;
                        }
                    }

                    String chosen = (String) JOptionPane.showInputDialog(
                        frame,
                        "Chọn bản lưu để tiếp tục:",
                        "Tiếp tục",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                    );

                    if (chosen == null) return; // user cancelled

                    // Map lại từ label đã chọn -> path
                    java.nio.file.Path selected = null;
                    for (int i = 0; i < options.length; i++) {
                        if (options[i].equals(chosen)) { selected = top.get(i); break; }
                    }
                    if (selected == null) return;

                    // Đếm ngược 3s trước khi vào game
                    JDialog dialog = new JDialog(frame, "Tiếp tục trò chơi", true);
                    JLabel label = new JLabel("Vào lại game sau 3s...", SwingConstants.CENTER);
                    label.setFont(new Font("Arial", Font.BOLD, 18));
                    dialog.getContentPane().add(label);
                    dialog.setSize(360, 130);
                    dialog.setLocationRelativeTo(frame);

                    Timer countdown = new Timer(1000, null);
                    final int[] remaining = {3};
                    java.nio.file.Path fileToLoad = selected;
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