import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class ArkanoidGame {

    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";
    private static final String CARD_INSTRUCTIONS = "instructions";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Arkanoid Basic");

            // Sử dụng CardLayout để chuyển màn hình Menu/Game/Instructions
            CardLayout cardLayout = new CardLayout();
            JPanel cards = new JPanel(cardLayout);

            // Đường dẫn ảnh nền (bạn có thể thay bằng ảnh của bạn)
            String backgroundPath = "images/arkanoid-background-intro.jpg"; // Đặt ảnh vào cùng thư mục này hoặc cập nhật đường dẫn

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
                    // Khi Game Over thì quay lại menu
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
            frame.setSize(684, 800);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Hiển thị màn menu đầu tiên
            cardLayout.show(cards, CARD_MENU);
        });
    }
}

