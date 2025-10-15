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

            // Nút Tiếp tục: nếu có save thì sẽ ĐẾM NGƯỢC 3s rồi vào game đã lưu
            // Quy trình:
            // 1) Kiểm tra tồn tại tệp save (saves/stage.txt)
            // 2) Hiển thị dialog đếm ngược 3,2,1 để tạo cảm giác tiếp nối
            // 3) Sau khi hết thời gian, tạo GamePanel, gọi SaveManager.load(), applyGameState(...)
            // 4) Đăng ký listener để khi Game Over quay lại MENU
            menu.getContinueButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Path savePath = Path.of("saves").resolve("stage.txt");
                    if (!Files.exists(savePath)) {
                        JOptionPane.showMessageDialog(frame, "Không tìm thấy save để tiếp tục.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    // Hiển thị đếm ngược 3 giây trước khi vào game (dialog modal đơn giản)
                    JDialog dialog = new JDialog(frame, "Tiếp tục trò chơi", true);
                    JLabel label = new JLabel("Vào lại game sau 3s...", SwingConstants.CENTER);
                    label.setFont(new Font("Arial", Font.BOLD, 18));
                    dialog.getContentPane().add(label);
                    dialog.setSize(320, 120);
                    dialog.setLocationRelativeTo(frame);

                    Timer countdown = new Timer(1000, null);
                    final int[] remaining = {3};
                    countdown.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ev) {
                            remaining[0]--;
                            if (remaining[0] > 0) {
                                label.setText("Vào lại game sau " + remaining[0] + "s...");
                            } else {
                                countdown.stop();
                                dialog.dispose();
                                // Tạo panel game và nạp (load) trạng thái đã lưu
                                GamePanel gamePanel = new GamePanel();
                                try {
                                    GameState state = SaveManager.load();
                                    gamePanel.applyGameState(state);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(frame, "Load save thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                                }
                                gamePanel.setEventsListener(new GamePanel.GameEvents() {
                                    @Override
                                    public void onGameOver() {
                                        // Khi game kết thúc, xóa thẻ game hiện tại và quay lại MENU
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

                    // chạy đếm ngược ở EDT; dialog modal được hiển thị sau khi timer bắt đầu
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