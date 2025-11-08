package entities;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

final class PaddleSkin {
    static final class Skin {
        private final Image image;
        private final int width;
        private final int height;

        Skin(Image image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
        }

        Image image() {
            return image;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }
    }

    private static final Path CONFIG_PATH = Paths.get("saves", "paddle_skin.cfg");
    private static final String DEFAULT_TOKEN = "DEFAULT";
    private static final String DEFAULT_SKIN_PATH =
        Paths.get("images", "skinPaddle1.png").toString().replace('\\', '/');

    private static boolean attempted;
    private static Skin cached;
    private static String lastSelectionToken;
    private static long lastConfigPollMs = Long.MIN_VALUE;

    private static final long CONFIG_POLL_INTERVAL_MS = 1_000; // poll config at most once per second

    private PaddleSkin() {
    }

    static Skin getSkin() {
        long now = System.currentTimeMillis();
        if (!attempted || now - lastConfigPollMs >= CONFIG_POLL_INTERVAL_MS) {
            lastConfigPollMs = now;
            String selection = readSelectionToken();
            if (!attempted || !equalsIgnoreCase(selection, lastSelectionToken)) {
                attempted = true;
                lastSelectionToken = selection;
                cached = resolveSelection(selection);
            }
        }
        return cached;
    }

    static Image getImage() {
        Skin skin = getSkin();
        return skin != null ? skin.image() : null;
    }

    static int getWidth() {
        Skin skin = getSkin();
        return skin != null ? skin.width() : -1;
    }

    static int getHeight() {
        Skin skin = getSkin();
        return skin != null ? skin.height() : -1;
    }

    static Skin loadForPath(String path) {
        return tryLoad(path);
    }

    static Skin getDefaultSkin() {
        return tryLoad(DEFAULT_SKIN_PATH);
    }

    static String getDefaultSkinPath() {
        return DEFAULT_SKIN_PATH;
    }

    static void clearCache() {
        cached = null;
        attempted = false;
        lastSelectionToken = null;
        lastConfigPollMs = Long.MIN_VALUE;
    }

    private static Skin resolveSelection(String token) {
        if (token == null || token.isBlank() || DEFAULT_TOKEN.equalsIgnoreCase(token)) {
            return tryLoad(DEFAULT_SKIN_PATH);
        }
        Skin skin = tryLoad(token);
        return skin != null ? skin : tryLoad(DEFAULT_SKIN_PATH);
    }

    private static String readSelectionToken() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                return DEFAULT_TOKEN;
            }
            for (String line : Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    return line.trim().replace('\\', '/');
                }
            }
        } catch (IOException ignored) {
            // fall through to default when read fails
        }
        return DEFAULT_TOKEN;
    }

    private static Skin tryLoad(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            Image source = null;
            int srcWidth = -1;
            int srcHeight = -1;

            URL resource = PaddleSkin.class.getClassLoader().getResource(path);
            if (resource != null) {
                ImageIcon icon = new ImageIcon(resource);
                srcWidth = icon.getIconWidth();
                srcHeight = icon.getIconHeight();
                source = icon.getImage();
            }

            if (source == null) {
                File file = new File(path);
                if (file.exists()) {
                    source = ImageIO.read(file);
                    if (source != null) {
                        srcWidth = source.getWidth(null);
                        srcHeight = source.getHeight(null);
                    }
                }
            }

            if (source != null && srcWidth > 0 && srcHeight > 0) {
                double scale = 0.2; // shrink by factor of 5
                double heightBoost = 1.15; // make paddle slightly taller
                int scaledW = Math.max(1, (int) Math.round(srcWidth * scale));
                int scaledH = Math.max(1, (int) Math.round(srcHeight * scale * heightBoost));
                Image scaled = source.getScaledInstance(scaledW, scaledH, Image.SCALE_SMOOTH);
                return new Skin(scaled, scaledW, scaledH);
            }
        } catch (IOException | SecurityException ignored) {
            // Ignore and continue with fallbacks
        }
        return null;
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a == b || (a != null && a.equalsIgnoreCase(b));
    }
}