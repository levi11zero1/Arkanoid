package ui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Panel hiển thị danh sách các bản save cho người dùng chọn.
 * Tiêu đề: "Hãy chọn phiên bản bạn muốn tiếp tục"
 * Tên mỗi bản sẽ là thời gian (ngày giờ) của lần save đó.
 */
public class SaveListPanel extends JPanel {
    private Image backgroundImage;
    private final JLabel titleLabel;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JScrollPane scroll;
    private final StyledButton loadButton = new StyledButton("Tiếp tục");
    private final StyledButton backButton = new StyledButton("Quay lại");
    private final StyledButton deleteButton = new StyledButton("Xóa");
    private Color textColor = Color.WHITE;

    // underlying paths for each list entry (index-aligned)
    private final List<Path> paths = new ArrayList<>();

    private final DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // hover tracking
    private int hoverIndex = -1;

    public SaveListPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // Try to load background image from resources or file system
        String[] tryNames = new String[] {"images/bg_saves.png", "images/bg_saves.jpg", "images/bg_saves.jpeg"};
        for (String n : tryNames) {
            try {
                java.io.File f = new java.io.File(n);
                if (f.exists() && f.isFile()) {
                    backgroundImage = new ImageIcon(n).getImage();
                    break;
                } else {
                    java.net.URL url = getClass().getResource("/" + n);
                    if (url != null) {
                        backgroundImage = new ImageIcon(url).getImage();
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        titleLabel = new JLabel("Hãy chọn phiên bản bạn muốn tiếp tục", SwingConstants.CENTER);
        titleLabel.setForeground(textColor);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(list.getFont().deriveFont(16f));
        list.setFixedCellHeight(36);
    list.setOpaque(false);
    list.setForeground(textColor);
        list.setSelectionBackground(new Color(255,255,255,80));
        list.setSelectionForeground(Color.WHITE);

        // custom renderer to show hover background
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> listComp, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(listComp, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
                if (isSelected) {
                    lbl.setBackground(new Color(255,255,255,110));
                    lbl.setForeground(Color.BLACK);
                } else if (index == hoverIndex) {
                    lbl.setBackground(new Color(255,255,255,50));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(new Color(0,0,0,0));
                    lbl.setForeground(textColor);
                }
                return lbl;
            }
        });

        // mouse motion listener to update hover index
        list.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx != hoverIndex) {
                    hoverIndex = idx;
                    list.repaint();
                }
            }
        });

        // clear hover when mouse exits list
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (hoverIndex != -1) {
                    hoverIndex = -1;
                    list.repaint();
                }
            }
        });

    scroll = new JScrollPane(list);
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));

        // Constrain the scroll pane so the list box is short (show ~3 rows).
        int visibleRows = 3;
        int cellHeight = list.getFixedCellHeight() > 0 ? list.getFixedCellHeight() : 36;
        int prefHeight = cellHeight * visibleRows + 8 * 2; // include small padding
        int prefWidth = 500; // make box narrower
        scroll.setPreferredSize(new Dimension(prefWidth, prefHeight));
        scroll.setMaximumSize(new Dimension(prefWidth, prefHeight));

    // Create a centered container: title above the short list box, centered on screen
    JPanel centerPanel = new JPanel();
    centerPanel.setOpaque(false);
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
    titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    scroll.setAlignmentX(Component.CENTER_ALIGNMENT);
    centerPanel.add(titleLabel);
    centerPanel.add(Box.createVerticalStrut(12));
    centerPanel.add(scroll);

    JPanel outer = new JPanel(new GridBagLayout());
    outer.setOpaque(false);
    outer.add(centerPanel);

    add(outer, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 12));

        loadButton.setPreferredSize(new Dimension(160, 44));
        backButton.setPreferredSize(new Dimension(140, 44));
        deleteButton.setPreferredSize(new Dimension(100, 44));

        // smaller corner for delete to look slightly different
        deleteButton.setCornerRadius(12);

        // initially disabled until selection
        loadButton.setEnabled(false);
        deleteButton.setEnabled(false);

        bottom.add(loadButton);
        bottom.add(deleteButton);
        bottom.add(backButton);

        add(bottom, BorderLayout.SOUTH);

        // enable buttons when selection changes
        list.addListSelectionListener(ev -> {
            boolean sel = list.getSelectedIndex() >= 0;
            loadButton.setEnabled(sel);
            deleteButton.setEnabled(sel);
        });

        // double-click shortcut to load
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = list.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        list.setSelectedIndex(idx);
                        // delegate to any registered listener by firing action event
                        for (ActionListener al : loadButton.getActionListeners()) {
                            al.actionPerformed(null);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            // dark overlay for readability
            g2.setColor(new Color(0,0,0,120));
            g2.fillRect(0,0,getWidth(), getHeight());
        } else {
            GradientPaint gp = new GradientPaint(0,0,new Color(36,49,77),0,getHeight(),new Color(18,25,38));
            g2.setPaint(gp);
            g2.fillRect(0,0,getWidth(), getHeight());
        }
        g2.dispose();
    }

    /**
     * Populate the panel with the given save files. The UI will show their last-modified time as name.
     */
    public void setSaves(List<Path> saveFiles) {
        model.clear();
        paths.clear();
        if (saveFiles == null) return;
        for (Path p : saveFiles) {
            String label = p.getFileName().toString();
            try {
                long ts = java.nio.file.Files.getLastModifiedTime(p).toMillis();
                Instant instant = Instant.ofEpochMilli(ts);
                LocalDateTime dt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                String when = dt.format(displayFmt);
                model.addElement(when + "  (" + label + ")");
            } catch (IOException ex) {
                model.addElement(label);
            }
            paths.add(p);
        }
        if (!paths.isEmpty()) {
            list.setSelectedIndex(0);
        }
    }

    public Path getSelectedPath() {
        int idx = list.getSelectedIndex();
        if (idx < 0 || idx >= paths.size()) return null;
        return paths.get(idx);
    }

    public JButton getLoadButton() { return loadButton; }
    public JButton getBackButton() { return backButton; }
    public JButton getDeleteButton() { return deleteButton; }
}
