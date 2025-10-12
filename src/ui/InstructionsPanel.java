package ui;

import java.awt.*;
import javax.swing.*;

/**
 * Màn hình hiển thị hướng dẫn điều khiển.
 */
public class InstructionsPanel extends JPanel {
    private final StyledButton backButton = new StyledButton("Quay lại");

    public InstructionsPanel() {
        setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(Color.WHITE);
        area.setFont(area.getFont().deriveFont(Font.PLAIN, 16f));

        area.setText(
                "HƯỚNG DẪN CHƠI\n\n" +
                "- Phím MŨI TÊN TRÁI/PHẢI hoặc A/D: di chuyển paddle.\n" +
                "- Phím SPACE: khởi động lại game timer.\n" +
                "- Phím ESC: thoát game.\n" +
                "- Nhiệm vụ: đỡ bóng và phá hết các viên gạch qua nhiều level.\n\n" +
                "Bấm 'Quay lại' để trở về Menu."
        );

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(12,12,12,12);
        content.add(area, gbc);

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
