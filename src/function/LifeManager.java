package function;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Quản lý số mạng của người chơi.
 */
public final class LifeManager {
    private static final Path LIFE_FILE = Paths.get("saves", "lives.cfg");
    private static final int DEFAULT_LIVES = 3;

    private LifeManager() {
    }

    public static int resetLives() {
        writeLives(DEFAULT_LIVES);
        return DEFAULT_LIVES;
    }

    public static int loadLives() {
        Integer value = readLives();
        if (value == null || value < 0) {
            writeLives(DEFAULT_LIVES);
            return DEFAULT_LIVES;
        }
        return value;
    }

    public static int decrementLife() {
        int lives = loadLives();
        lives = Math.max(0, lives - 1);
        writeLives(lives);
        return lives;
    }

    public static void setLives(int lives) {
        writeLives(Math.max(0, lives));
    }

    public static int getDefaultLives() {
        return DEFAULT_LIVES;
    }

    private static Integer readLives() {
        ensureFileExists();
        try {
            String content = Files.readString(LIFE_FILE, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                return DEFAULT_LIVES;
            }
            return Integer.parseInt(content);
        } catch (IOException | NumberFormatException ignored) {
            return DEFAULT_LIVES;
        }
    }

    private static void writeLives(int lives) {
        ensureDirectory();
        try {
            Files.writeString(LIFE_FILE, Integer.toString(lives), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Nếu ghi thất bại thì bỏ qua và tiếp tục bằng giá trị trong bộ nhớ
        }
    }

    private static void ensureFileExists() {
        ensureDirectory();
        if (Files.notExists(LIFE_FILE)) {
            writeLives(DEFAULT_LIVES);
        }
    }

    private static void ensureDirectory() {
        Path parent = LIFE_FILE.getParent();
        if (parent == null) {
            return;
        }
        try {
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException ignored) {
            // Nếu tạo thư mục thất bại, phần còn lại sẽ xử lý lỗi trong lúc đọc/ghi
        }
    }
}
