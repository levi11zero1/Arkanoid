package entities;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import utils.GameConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

final class PaddleSkin {
    static final class Skin {
        private final Image[] frames;
        private final int width;
        private final int height;
        private final int frameDurationMs;

        Skin(Image[] frames, int width, int height, int frameDurationMs) {
            this.frames = frames;
            this.width = width;
            this.height = height;
            this.frameDurationMs = Math.max(1, frameDurationMs);
        }

        Image image() {
            // return current animation frame (or sole image)
            if (frames == null || frames.length == 0) return null;
            if (frames.length == 1) return frames[0];
            long idx = (System.currentTimeMillis() / frameDurationMs) % frames.length;
            return frames[(int) idx];
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
    // Default paddle skin (restore original default). Wukong animation remains
    // available via saves/paddle_skin.cfg (e.g. images/WukongAnimation/a,80).
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
            // Support optional frame duration: path may be "basePath,frameMs"
            String pathBase = path;
            int frameDuration = 100;
            int comma = path.indexOf(',');
            if (comma >= 0) {
                pathBase = path.substring(0, comma).trim();
                String dur = path.substring(comma + 1).trim();
                try {
                    frameDuration = Integer.parseInt(dur);
                    if (frameDuration <= 0) frameDuration = 100;
                } catch (NumberFormatException ignored) {
                    // fallback to default
                    frameDuration = 100;
                }
            }
            // First try single-image load (resource or file)
            Image source = null;
            int srcWidth = -1;
            int srcHeight = -1;
            URL resource = PaddleSkin.class.getClassLoader().getResource(pathBase);
            if (resource != null) {
                ImageIcon icon = new ImageIcon(resource);
                srcWidth = icon.getIconWidth();
                srcHeight = icon.getIconHeight();
                source = icon.getImage();
            }

            if (source == null) {
                File file = new File(pathBase);
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
                return new Skin(new Image[] { scaled }, scaledW, scaledH, frameDuration);
            }

            // If single image not found, try a sequence a1..a6 based on the provided path as a prefix
            // Strip extension if present
            String base = path;
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            java.util.List<Image> frames = new java.util.ArrayList<>();
            int maxFrames = 6; // try a1..a6
            int frameW = -1, frameH = -1;
            for (int i = 1; i <= maxFrames; i++) {
                String tryPath = base + i + ".png";
                Image src = null;
                URL res = PaddleSkin.class.getClassLoader().getResource(tryPath);
                if (res != null) {
                    ImageIcon icon = new ImageIcon(res);
                    src = icon.getImage();
                    if (frameW <= 0) {
                        frameW = icon.getIconWidth();
                        frameH = icon.getIconHeight();
                    }
                } else {
                    File f = new File(tryPath);
                    if (f.exists()) {
                        src = ImageIO.read(f);
                        if (src != null && frameW <= 0) {
                            frameW = src.getWidth(null);
                            frameH = src.getHeight(null);
                        }
                    }
                }
                if (src != null) {
                    // scale each frame same as single-image logic
                    double scale = 0.2;
                    double heightBoost = 1.15;
                    int scaledW = Math.max(1, (int) Math.round(frameW * scale));
                    int scaledH = Math.max(1, (int) Math.round(frameH * scale * heightBoost));
                    Image scaled = src.getScaledInstance(scaledW, scaledH, Image.SCALE_SMOOTH);
                    frames.add(scaled);
                }
            }
                if (!frames.isEmpty()) {
                    Image[] arr = frames.toArray(new Image[0]);
                    return new Skin(arr, arr[0].getWidth(null), arr[0].getHeight(null), frameDuration);
                }

                // If no external frames found, generate placeholder animated frames so user
                // immediately sees animation while they upload real assets.
                try {
                    int pw = Math.max(16, GameConfig.DEFAULT_PADDLE_WIDTH);
                    int ph = Math.max(8, GameConfig.PADDLE_HEIGHT);
                    Image[] placeholder = new Image[6];
                    Color[] colors = new Color[] { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA };
                    for (int i = 0; i < 6; i++) {
                        BufferedImage bi = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = bi.createGraphics();
                        try {
                            g2.setColor(colors[i % colors.length]);
                            g2.fillRect(0, 0, pw, ph);
                            g2.setColor(Color.BLACK);
                            g2.drawRect(0, 0, pw - 1, ph - 1);
                        } finally {
                            g2.dispose();
                        }
                        placeholder[i] = bi;
                    }
                    return new Skin(placeholder, pw, ph, frameDuration);
                } catch (Throwable t) {
                    // ignore and fall through to null
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