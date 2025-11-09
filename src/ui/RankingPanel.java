package ui;

import function.RankingManager;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class RankingPanel extends JPanel {
    private final StyledButton backButton = new StyledButton("Quay lại");
    private final DefaultListModel<function.RankingManager.Entry> model = new DefaultListModel<>();
    private final JList<function.RankingManager.Entry> list = new JList<>(model);

    public RankingPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel title = new JLabel("Bảng xếp hạng", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,6,12));

        list.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        list.setOpaque(false);
        list.setForeground(Color.WHITE);
        list.setSelectionBackground(new Color(255,255,255,80));
        list.setSelectionForeground(Color.BLACK);
        list.setFixedCellHeight(-1); // variable height
        list.setCellRenderer(new ListCellRenderer<function.RankingManager.Entry>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends function.RankingManager.Entry> listComp, function.RankingManager.Entry value, int index, boolean isSelected, boolean cellHasFocus) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(true);
                row.setBackground(isSelected ? new Color(255,255,255,110) : new Color(0,0,0,40));
                row.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));

                // Left: rank / medal
                JLabel rankLabel = new JLabel(String.valueOf(index+1));
                rankLabel.setPreferredSize(new Dimension(36,36));
                rankLabel.setHorizontalAlignment(SwingConstants.CENTER);
                rankLabel.setOpaque(true);
                if (index == 0) {
                    rankLabel.setBackground(new Color(212,175,55)); // gold
                } else if (index == 1) {
                    rankLabel.setBackground(new Color(192,192,192)); // silver
                } else if (index == 2) {
                    rankLabel.setBackground(new Color(205,127,50)); // bronze
                } else {
                    rankLabel.setBackground(new Color(60,100,100));
                }
                rankLabel.setForeground(Color.BLACK);
                rankLabel.setFont(rankLabel.getFont().deriveFont(Font.BOLD, 14f));

                // Center: player name and small subtitle
                JPanel center = new JPanel();
                center.setOpaque(false);
                center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
                // add spacing between rank box and player name
                center.setBorder(BorderFactory.createEmptyBorder(0,12,0,0));
                JLabel name = new JLabel(value.player);
                name.setForeground(Color.WHITE);
                name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
                JLabel subtitle = new JLabel(String.format("Levels: %d   Blocks: %d", value.levels, value.blocks));
                subtitle.setForeground(new Color(200,200,200));
                subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
                center.add(name);
                center.add(subtitle);

                // Right: time
                JLabel time = new JLabel(formatDuration(value.elapsedMs));
                time.setForeground(Color.WHITE);
                time.setFont(time.getFont().deriveFont(Font.PLAIN, 14f));

                row.add(rankLabel, BorderLayout.WEST);
                row.add(center, BorderLayout.CENTER);
                row.add(time, BorderLayout.EAST);
                return row;
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
        List<RankingManager.Entry> entries = RankingManager.getSorted(20);
        for (RankingManager.Entry e : entries) {
            model.addElement(e);
        }
        if (model.isEmpty()) {
            // add a placeholder entry
            RankingManager.Entry placeholder = new RankingManager.Entry("Chưa có dữ liệu", 0, 0, 0L, 0L);
            model.addElement(placeholder);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Make the list area tall enough to show ~10 rows without scrolling
        int approxRow = 68; // approximate row height with padding
        int h = approxRow * 8 + 16; // show ~8 rows
        Dimension pref = new Dimension(760, h);
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
