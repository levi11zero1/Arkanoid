package levels;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public final class LevelBackgrounds {
    private static final String[] LEVEL0_PATHS = {
        "images/bgr_levels0.png",
        "images/level0.png"
    };

    private static final String LEVEL1_ABSOLUTE_PATH =
        "C:/Users/PC/Downloads/237c7024e18e7eac07df48e7aba85c49.jpg";

    private static final String[] LEVEL1_PATHS = {
        "images/level1-background.jpg",
        "images/level1.png",
        LEVEL1_ABSOLUTE_PATH
    };

    private static final String[] LEVEL2_PATHS = {
        "images/level2.png",
        "images/level2.jpg"
    };

    private static final String[] LEVEL3_PATHS = {
        "images/SVHUST.png",

    };

    private static Image level0Image;
    private static boolean level0Initialized;

    private static Image level1Image;
    private static boolean level1Initialized;

    private static Image level2Image;
    private static boolean level2Initialized;

    private static Image level3Image;
    private static boolean level3Initialized;

    private LevelBackgrounds() {
    }

    public static Image getForLevel(int level) {
        if (level == 0) {
            ensureLevel0Loaded();
            return level0Image;
        }
        if (level == 1) {
            ensureLevel1Loaded();
            return level1Image;
        }
        if (level == 2) {
            ensureLevel2Loaded();
            return level2Image;
        }
        if (level == 3) {
            ensureLevel3Loaded();
            return level3Image;
        }
        return null;
    }

    private static void ensureLevel0Loaded() {
        if (level0Initialized) {
            return;
        }
        level0Initialized = true;
        level0Image = loadFirstAvailable(LEVEL0_PATHS);
    }

    private static void ensureLevel1Loaded() {
        if (level1Initialized) {
            return;
        }
        level1Initialized = true;
        level1Image = loadFirstAvailable(LEVEL1_PATHS);
    }

    private static void ensureLevel2Loaded() {
        if (level2Initialized) {
            return;
        }
        level2Initialized = true;
        level2Image = loadFirstAvailable(LEVEL2_PATHS);
    }

    private static void ensureLevel3Loaded() {
        if (level3Initialized) {
            return;
        }
        level3Initialized = true;
        level3Image = loadFirstAvailable(LEVEL3_PATHS);
    }

    private static Image loadFirstAvailable(String[] paths) {
        if (paths == null) {
            return null;
        }
        for (String path : paths) {
            Image image = loadImage(path);
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    private static Image loadImage(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            URL resource = LevelBackgrounds.class.getClassLoader().getResource(path);
            if (resource != null) {
                return new ImageIcon(resource).getImage();
            }

            File file = new File(path);
            if (file.exists()) {
                return ImageIO.read(file);
            }
        } catch (IOException | SecurityException ignored) {
        }
        return null;
    }
}