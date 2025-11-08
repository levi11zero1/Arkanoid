package entities;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Hiển thị hộp thoại chọn skin cho paddle hoặc bóng thông qua một hàm chung.
 */
public final class SkinManager {
    private static final Path PADDLE_CONFIG_PATH = Paths.get("saves", "paddle_skin.cfg");
    private static final Path BALL_CONFIG_PATH = Paths.get("saves", "ball_skin.cfg");
    private static final Path IMAGES_DIR = Paths.get("images");
    private static final String DEFAULT_TOKEN = "DEFAULT";

    private static final SkinContext PADDLE_CONTEXT = new SkinContext(
            "Chọn Paddle Skin",
            "Vui lòng chọn skin paddle!",
            "Đã lưu skin paddle",
            PADDLE_CONFIG_PATH,
            List.of(
                    SkinEntry.defaultEntry("mặc định", PaddleSkin.getDefaultSkinPath()),
                    SkinEntry.of("Tề thiên võ thần", IMAGES_DIR.resolve("skinPaddle2.png").toString()),
                    SkinEntry.of("Phù thủy thời không", IMAGES_DIR.resolve("skinPaddle3.png").toString())
            ),
            PaddleSkin::clearCache
    );

    private static final SkinContext BALL_CONTEXT = new SkinContext(
            "Chọn Ball Skin",
            "Vui lòng chọn skin bóng!",
            "Đã lưu skin bóng",
            BALL_CONFIG_PATH,
            List.of(
                    SkinEntry.defaultEntry("mặc định", BallSkin.getDefaultSkinPath()),
                    SkinEntry.of("Ice Cream", IMAGES_DIR.resolve("SkinBall2.png").toString()),
                    SkinEntry.of("My country", IMAGES_DIR.resolve("Skinball3.png").toString())
            ),
            BallSkin::clearCache
    );

    public enum SkinType {
        PADDLE,
        BALL
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SkinType type = determineType(args);
            selectSkin(type);
        });
    }

    public static void selectPaddleSkin() {
        selectSkin(SkinType.PADDLE);
    }

    public static void selectBallSkin() {
        selectSkin(SkinType.BALL);
    }

    public static void selectSkin(SkinType type) {
        if (type == null) {
            return;
        }
    SwingUtilities.invokeLater(() -> new SkinManager().openSelector(contextFor(type)));
    }

    private static SkinType determineType(String[] args) {
        if (args != null && args.length > 0) {
            if ("ball".equalsIgnoreCase(args[0])) {
                return SkinType.BALL;
            }
            if ("paddle".equalsIgnoreCase(args[0])) {
                return SkinType.PADDLE;
            }
        }

        Object[] options = { "Paddle", "Ball" };
        int choice = JOptionPane.showOptionDialog(
                null,
                "Bạn muốn chọn skin cho loại nào?",
                "Chọn loại skin",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 1) {
            return SkinType.BALL;
        }
        if (choice == 0) {
            return SkinType.PADDLE;
        }
        return null;
    }

    private static SkinContext contextFor(SkinType type) {
        return switch (type) {
            case PADDLE -> PADDLE_CONTEXT;
            case BALL -> BALL_CONTEXT;
        };
    }

    private void openSelector(SkinContext context) {
        DefaultListModel<SkinEntry> model = new DefaultListModel<>();
        JList<SkinEntry> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        populateModel(context, model, list);

        JFrame frame = new JFrame(context.windowTitle());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(320, 240);
        frame.setLocationRelativeTo(null);

        JButton saveButton = new JButton("Lưu lựa chọn");
        saveButton.addActionListener(e -> saveSelection(frame, context, list));

        frame.setLayout(new BorderLayout(8, 8));
        frame.add(new JScrollPane(list), BorderLayout.CENTER);
        frame.add(saveButton, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void populateModel(SkinContext context, DefaultListModel<SkinEntry> model, JList<SkinEntry> list) {
        model.clear();
        String current = readSelection(context.configPath());
        SkinEntry selected = null;

        for (SkinEntry entry : context.entries()) {
            model.addElement(entry);
            if (selected == null && entry.matches(current)) {
                selected = entry;
            }
        }

        if (selected != null) {
            list.setSelectedValue(selected, true);
        } else if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        }
    }

    private void saveSelection(JFrame parent, SkinContext context, JList<SkinEntry> list) {
        SkinEntry entry = list.getSelectedValue();
        if (entry == null) {
            JOptionPane.showMessageDialog(parent, context.warningMessage(), "Chưa chọn skin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            writeSelection(context.configPath(), entry.configValue());
            context.clearCache().run();
            JOptionPane.showMessageDialog(parent, context.successMessage(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Không thể lưu lựa chọn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String readSelection(Path configPath) {
        try {
            if (!Files.exists(configPath)) {
                return DEFAULT_TOKEN;
            }
            for (String line : Files.readAllLines(configPath, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    return normalise(line);
                }
            }
        } catch (IOException ignored) {
            // Fallback to default nếu đọc thất bại
        }
        return DEFAULT_TOKEN;
    }

    private static void writeSelection(Path configPath, String value) throws IOException {
        Path parent = configPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(configPath, value.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalise(String value) {
        if (value == null) {
            return DEFAULT_TOKEN;
        }
        return value.trim().replace('\\', '/');
    }

    private record SkinContext(
            String windowTitle,
            String warningMessage,
            String successMessage,
            Path configPath,
            List<SkinEntry> entries,
            Runnable clearCache
    ) {
    }

    private static final class SkinEntry {
        private final String displayName;
        private final String configValue;
        private final boolean isDefault;
        private final String defaultPath;

        private SkinEntry(String displayName, String configValue, boolean isDefault, String defaultPath) {
            this.displayName = displayName;
            this.configValue = normalise(configValue);
            this.isDefault = isDefault;
            this.defaultPath = defaultPath != null ? normalise(defaultPath) : null;
        }

        static SkinEntry defaultEntry(String displayName, String defaultPath) {
            return new SkinEntry(displayName, DEFAULT_TOKEN, true, defaultPath);
        }

        static SkinEntry of(String displayName, String configValue) {
            return new SkinEntry(displayName, configValue, false, null);
        }

        String configValue() {
            return configValue;
        }

        boolean matches(String raw) {
            String normalised = normalise(raw);
            if (isDefault) {
                if (DEFAULT_TOKEN.equalsIgnoreCase(normalised)) {
                    return true;
                }
                return defaultPath != null && defaultPath.equalsIgnoreCase(normalised);
            }
            return configValue.equalsIgnoreCase(normalised);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
