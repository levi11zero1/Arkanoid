package ui;

import java.awt.*;
import javax.swing.*;

/**
 * Màn hình hiển thị hướng dẫn điều khiển.
 */
public class InstructionsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final StyledButton backButton = new StyledButton("Quay lại");

    public InstructionsPanel() {
        setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(Color.WHITE);
        area.setFont(area.getFont().deriveFont(Font.PLAIN, 16f));
        // Mỗi ý một dòng: tắt tự động xuống dòng
        area.setLineWrap(false);

        area.setText(
            "HƯỚNG DẪN CHƠI\n" +
            "⬅️ / A: Di chuyển sang trái\n" +
            "➡️ / D: Di chuyển sang phải\n" +
            "Space: Bắt đầu hoặc phóng bóng\n" +
            "P: Tạm dừng / Tiếp tục\n" +
            "R: Phân thân paddle thành 3 (15s)\n" +
            "Esc: Thoát game\n" +
            "\n" +
            "CHẾ ĐỘ 2 NGƯỜI\n" +
            "Người chơi Trên: A, D\n" +
            "Người chơi Dưới: Mũi tên Trái, Phải\n" +
            "(P: Tạm dừng chung)\n" +
            "\n" +
            "Mục tiêu: Dùng gậy Như Ý đánh bóng phá hết gạch mà không để bóng rơi.\n" +
            "Bấm 'Quay lại' để trở về Menu."
        );

    JPanel content = new JPanel(new GridBagLayout());
    content.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(12,12,12,12);
    // Bọc trong scroll để nếu text dài vẫn không bị tự wrap
    JScrollPane sp = new JScrollPane(area,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    sp.setBorder(null);
    sp.setOpaque(false);
    sp.getViewport().setOpaque(false);
    content.add(sp, gbc);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        backButton.setCornerRadius(18);
        bottom.add(backButton);

        add(content, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 46), 0, getHeight(), new Color(15, 15, 23));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    public JButton getBackButton() { return backButton; }
    public StyledButton getBackStyledButton() { return backButton; }
}
