package entities;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * PaddleSkinManager hiển thị danh sách skin paddle hiện có và cho phép người chơi chọn skin.
 * Chạy file này (có hàm main) để mở giao diện chọn skin.
 */
public final class PaddleSkinManager {
    private static final Path CONFIG_PATH = Paths.get("saves", "paddle_skin.cfg");
    private static final Path IMAGES_DIR = Paths.get("images");
    private static final String DEFAULT_TOKEN = "DEFAULT";
    private static final String WUKONG_LABEL = "Wukong (Tôn Ngộ Không)";
    private static final String WUKONG_FILE = IMAGES_DIR.resolve("skinPaddle.png").toString().replace('\\', '/');
    private static final String SKIN2_LABEL = "SkinPaddle 2";
    private static final String SKIN2_FILE = IMAGES_DIR.resolve("skinPaddle2.png").toString().replace('\\', '/');
    private static final String SKIN3_LABEL = "SkinPaddle 3";
    private static final String SKIN3_FILE = IMAGES_DIR.resolve("skinPaddle3.png").toString().replace('\\', '/');

    private final DefaultListModel<SkinOption> model = new DefaultListModel<>();
    private final JList<SkinOption> list = new JList<>(model);
    private final JLabel previewLabel = new JLabel("Không có xem trước", SwingConstants.CENTER);

    private PaddleSkinManager() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaddleSkinManager().showUI());
    }

    private void showUI() {
        JFrame frame = new JFrame("Paddle Skin Manager");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(560, 420);
        frame.setLocationRelativeTo(null);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new SkinOptionRenderer());
        list.addListSelectionListener(new SelectionSyncer());

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(220, 360));

        previewLabel.setPreferredSize(new Dimension(240, 240));
        previewLabel.setBorder(BorderFactory.createTitledBorder("Xem trước"));

        JButton applyButton = new JButton("Lưu lựa chọn");
        applyButton.addActionListener(e -> saveSelection(frame));

        JButton importButton = new JButton("Chọn ảnh khác...");
        importButton.addActionListener(e -> importExternalSkin(frame));

        JPanel actionsPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        actionsPanel.add(applyButton);
        actionsPanel.add(importButton);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.add(previewLabel, BorderLayout.CENTER);
        rightPanel.add(actionsPanel, BorderLayout.SOUTH);

        frame.setLayout(new BorderLayout(12, 12));
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.EAST);

        loadOptions();
        frame.setVisible(true);

        if (!model.isEmpty()) {
            SkinOption selected = list.getSelectedValue();
            if (selected == null) {
                list.setSelectedIndex(0);
            }
            updatePreview();
        }
    }

    private void loadOptions() {
        model.clear();
        String current = readCurrentSelection();
        SkinOption selected = null;

        SkinOption defaultOption = SkinOption.defaultOption();
        model.addElement(defaultOption);
        if (defaultOption.matches(current)) {
            selected = defaultOption;
        }

        SkinOption wukongOption = SkinOption.wukongOption();
        if (wukongOption != null) {
            model.addElement(wukongOption);
            if (selected == null && wukongOption.matches(current)) {
                selected = wukongOption;
            }
        }

        SkinOption skin2Option = SkinOption.skin2Option();
        if (skin2Option != null) {
            model.addElement(skin2Option);
            if (selected == null && skin2Option.matches(current)) {
                selected = skin2Option;
            }
        }

        SkinOption skin3Option = SkinOption.skin3Option();
        if (skin3Option != null) {
            model.addElement(skin3Option);
            if (selected == null && skin3Option.matches(current)) {
                selected = skin3Option;
            }
        }

        if (selected == null && current != null && !DEFAULT_TOKEN.equalsIgnoreCase(current)) {
            SkinOption custom = SkinOption.fromConfigValue(current);
            if (custom != null) {
                model.addElement(custom);
                selected = custom;
            }
        }

        if (selected != null) {
            list.setSelectedValue(selected, true);
        } else {
            list.setSelectedIndex(0);
        }
    }

    private void saveSelection(JFrame parent) {
        SkinOption option = list.getSelectedValue();
        if (option == null) {
            JOptionPane.showMessageDialog(parent, "Vui lòng chọn một skin!", "Chưa chọn skin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.write(CONFIG_PATH, option.configValue.getBytes(StandardCharsets.UTF_8));
            PaddleSkin.clearCache();
            JOptionPane.showMessageDialog(parent, "Đã lưu skin: " + option.displayName, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    "Không thể lưu lựa chọn: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importExternalSkin(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn ảnh skin");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "Ảnh (*.png, *.jpg, *.jpeg, *.gif, *.bmp)",
                "png", "jpg", "jpeg", "gif", "bmp"));

        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selectedFile = chooser.getSelectedFile().toPath();
        SkinOption option = SkinOption.fromExternalFile(selectedFile);
        if (option == null) {
            JOptionPane.showMessageDialog(parent, "Không thể tải ảnh này làm skin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean replaced = replaceExistingOption(option);
        if (!replaced) {
            model.addElement(option);
        }
        list.setSelectedValue(option, true);
        updatePreview();
    }

    private boolean replaceExistingOption(SkinOption newOption) {
        for (int i = 0; i < model.size(); i++) {
            SkinOption existing = model.get(i);
            if (existing.matches(newOption.configValue)) {
                model.set(i, newOption);
                return true;
            }
        }
        return false;
    }

    private String readCurrentSelection() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                return DEFAULT_TOKEN;
            }
            for (String line : Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    return normalise(line);
                }
            }
        } catch (IOException ignored) {
            // Ignore read failures; fallback to default
        }
        return DEFAULT_TOKEN;
    }

    private void updatePreview() {
        SkinOption option = list.getSelectedValue();
        if (option == null) {
            previewLabel.setText("Không có xem trước");
            previewLabel.setIcon(null);
            return;
        }

        PaddleSkin.Skin skin = option.ensureLoaded();
        if (skin == null) {
            if (option.isDefault) {
                previewLabel.setText("Không tìm thấy SkinPaddle 1 mặc định");
            } else {
                previewLabel.setText("Không thể tải skin");
            }
            previewLabel.setIcon(null);
            return;
        }

        Image image = skin.image();
        if (image == null) {
            if (option.isDefault) {
                previewLabel.setText("Skin mặc định không hợp lệ");
            } else {
                previewLabel.setText("Skin không hợp lệ");
            }
            previewLabel.setIcon(null);
            return;
        }

        previewLabel.setText(null);
        previewLabel.setIcon(new ImageIcon(image));
    }

    private static String normalise(String value) {
        if (value == null) {
            return DEFAULT_TOKEN;
        }
        return value.trim().replace('\\', '/');
    }

    private final class SelectionSyncer implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                updatePreview();
            }
        }
    }

    private static final class SkinOptionRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SkinOption option) {
                label.setText(option.displayName);
            }
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return label;
        }
    }

    private static final class SkinOption {
        private final String displayName;
        private final String configValue;
        private final boolean isDefault;
        private PaddleSkin.Skin cachedSkin;

        private SkinOption(String displayName, String configValue, boolean isDefault, PaddleSkin.Skin cachedSkin) {
            this.displayName = displayName;
            this.configValue = normalise(configValue);
            this.isDefault = isDefault;
            this.cachedSkin = cachedSkin;
        }

        static SkinOption defaultOption() {
            PaddleSkin.Skin skin = PaddleSkin.getDefaultSkin();
            return new SkinOption("SkinPaddle 1 (mặc định)", DEFAULT_TOKEN, true, skin);
        }

        static SkinOption wukongOption() {
            PaddleSkin.Skin skin = PaddleSkin.loadForPath(WUKONG_FILE);
            if (skin == null) {
                return null;
            }
            return new SkinOption(WUKONG_LABEL, WUKONG_FILE, false, skin);
        }

        static SkinOption skin2Option() {
            PaddleSkin.Skin skin = PaddleSkin.loadForPath(SKIN2_FILE);
            if (skin == null) {
                return null;
            }
            return new SkinOption(SKIN2_LABEL, SKIN2_FILE, false, skin);
        }

        static SkinOption skin3Option() {
            PaddleSkin.Skin skin = PaddleSkin.loadForPath(SKIN3_FILE);
            if (skin == null) {
                return null;
            }
            return new SkinOption(SKIN3_LABEL, SKIN3_FILE, false, skin);
        }

        static SkinOption fromExternalFile(Path file) {
            if (file == null || !Files.isRegularFile(file)) {
                return null;
            }
            PaddleSkin.Skin skin = PaddleSkin.loadForPath(file.toString());
            if (skin == null) {
                return null;
            }
            String display = file.getFileName().toString() + " (ngoài)";
            return new SkinOption(display, file.toString(), false, skin);
        }

        static SkinOption fromConfigValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String normalised = normalise(raw);
            if (DEFAULT_TOKEN.equalsIgnoreCase(normalised)) {
                return defaultOption();
            }
            if (normalised.equalsIgnoreCase(PaddleSkin.getDefaultSkinPath())) {
                return defaultOption();
            }
            if (normalised.equalsIgnoreCase(WUKONG_FILE)) {
                return wukongOption();
            }
            if (normalised.equalsIgnoreCase(SKIN2_FILE)) {
                return skin2Option();
            }
            if (normalised.equalsIgnoreCase(SKIN3_FILE)) {
                return skin3Option();
            }
            PaddleSkin.Skin skin = PaddleSkin.loadForPath(normalised);
            String display = deriveDisplayName(normalised);
            return new SkinOption(display, normalised, false, skin);
        }

        private static String deriveDisplayName(String path) {
            Path p = Paths.get(path);
            if (p.getFileName() != null) {
                String name = p.getFileName().toString();
                if (!path.startsWith("images")) {
                    return name + " (tùy chỉnh)";
                }
                return name;
            }
            return path;
        }

        boolean matches(String raw) {
            if (raw == null) {
                return isDefault;
            }
            String normalisedRaw = normalise(raw);
            if (isDefault) {
                return DEFAULT_TOKEN.equalsIgnoreCase(normalisedRaw)
                        || normalisedRaw.equalsIgnoreCase(PaddleSkin.getDefaultSkinPath());
            }
            return this.configValue.equalsIgnoreCase(normalisedRaw);
        }

        PaddleSkin.Skin ensureLoaded() {
            if (cachedSkin == null) {
                if (isDefault) {
                    cachedSkin = PaddleSkin.getDefaultSkin();
                } else {
                    cachedSkin = PaddleSkin.loadForPath(configValue);
                }
            }
            return cachedSkin;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
