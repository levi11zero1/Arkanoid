package ui;

import function.RankingManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RankingPanel extends JPanel {
    private final StyledButton backButton = new StyledButton("Quay lại");
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);

    public RankingPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel title = new JLabel("Bảng xếp hạng", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,6,12));

        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        list.setOpaque(false);
        list.setForeground(Color.WHITE);
        list.setSelectionBackground(new Color(255,255,255,80));
        list.setSelectionForeground(Color.BLACK);
        list.setFixedCellHeight(28);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> listComp, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(listComp, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                if (isSelected) {
                    lbl.setBackground(new Color(255,255,255,110));
                    lbl.setForeground(Color.BLACK);
                } else {
                    lbl.setBackground(new Color(0,0,0,0));
                    lbl.setForeground(Color.WHITE);
                }
                lbl.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
                return lbl;
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(8,24,8,24));

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(backButton);

        add(title, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        GradientPaint gp = new GradientPaint(0,0,new Color(36,49,77),0,getHeight(),new Color(18,25,38));
        g2.setPaint(gp);
        g2.fillRect(0,0,getWidth(), getHeight());
        g2.dispose();
    }

    public void refreshList() {
        model.clear();
        List<RankingManager.Entry> entries = RankingManager.getSorted(10);
        int idx = 1;
        for (RankingManager.Entry e : entries) {
            String timeStr = formatDuration(e.elapsedMs);
            String line = String.format("%2d. %-16s  Levels:%2d  Blocks:%4d  Time:%s",
                    idx++, truncate(e.player,16), e.levels, e.blocks, timeStr);
            model.addElement(line);
        }
        if (model.isEmpty()) {
            model.addElement("Chưa có dữ liệu xếp hạng.");
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Make the list area tall enough to show ~10 rows without scrolling
        int cellH = list.getFixedCellHeight() > 0 ? list.getFixedCellHeight() : 28;
        int h = cellH * 10 + 16; // a bit padding
        Dimension pref = new Dimension(680, h);
        if (getLayout() instanceof BorderLayout) {
            // Try to adjust center scroll if present
            for (Component c : getComponents()) {
                if (c instanceof JScrollPane sp) {
                    sp.setPreferredSize(pref);
                    break;
                }
            }
        }
        revalidate();
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        if (s.length() <= n) return s;
        return s.substring(0, n-1) + "…";
    }

    private static String formatDuration(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%02dm %02ds", m, s);
    }

    public JButton getBackButton() { return backButton; }
}
