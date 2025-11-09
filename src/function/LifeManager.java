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

    // Timestamp (ms since epoch) when a life was last lost. 0 if never.
    private static volatile long lastLifeLostAtMs = 0L;
    // Duration (ms) that renderers should show the life-lost message.
    private static final int LIFE_LOST_MESSAGE_DURATION_MS = 2000;
    // Candidate messages to show when a life is lost. Picked at random.
    private static final String[] LIFE_LOST_MESSAGES = new String[] {
        "Giận quá mất khôn.",
        "Đứng dậy coi.",
        "Mạnh mỗi cái miệng.",
        "Yếu quá, để anh lo."
    };
    // The last message selected for display (or null if none).
    private static volatile String lastLifeLostMessage = null;

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
        // Record the time of life loss so UI code can show a temporary message.
        lastLifeLostAtMs = System.currentTimeMillis();
        // choose a random message
        try {
            int idx = (int) (Math.random() * LIFE_LOST_MESSAGES.length);
            if (idx < 0) idx = 0;
            if (idx >= LIFE_LOST_MESSAGES.length) idx = LIFE_LOST_MESSAGES.length - 1;
            lastLifeLostMessage = LIFE_LOST_MESSAGES[idx];
        } catch (Throwable t) {
            lastLifeLostMessage = LIFE_LOST_MESSAGES[0];
        }
        return lives;
    }

    /**
     * Returns the timestamp (ms since epoch) when a life was last lost, or 0 if never.
     */
    public static long getLastLifeLostAtMs() {
        return lastLifeLostAtMs;
    }

    /**
     * Returns how long (ms) the life-lost message should be displayed.
     */
    public static int getLifeLostMessageDurationMs() {
        return LIFE_LOST_MESSAGE_DURATION_MS;
    }

    /**
     * Returns the last selected life-lost message (may be null).
     */
    public static String getLastLifeLostMessage() {
        return lastLifeLostMessage;
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
