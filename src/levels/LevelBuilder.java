package levels;

import entities.Block;
import java.util.ArrayList;
import java.util.List;
import utils.GameConfig;

/**
 * LevelBuilder – Unified level construction from binary (0/1) maps.
 * 
 * Rules (originally from Level6, now shared by all levels):
 *   1. Each level defines a static String[] MAP of '0'/'1' characters:
 *      '1' => block exists; '0' => empty space.
 *   2. Map is scaled to fit 15 columns across screen width with aspect ratio preserved (nearest-neighbor).
 *   3. Result is horizontally & vertically centered in available grid space.
 *   4. Hit tiers are assigned by row band:
 *      - Top 3 rows => 3 hits (MAGENTA blocks)
 *      - Next 3 rows => 2 hits (ORANGE blocks)
 *      - Remaining rows => 1 hit (RED blocks)
 * 
 * To add a new level:
 *   - Create a LevelN class with a static MAP field (String[] of '0'/'1').
 *   - Add a case to createLevel(int) referencing LevelN.MAP.
 *   - Update GameConfig.MAX_LEVELS if needed.
 */
public class LevelBuilder {
    
    public static List<Block> createLevel(int levelNumber) {
        String[] map = switch (levelNumber) {
            case 1 -> Level1.MAP;
            case 2 -> Level2.MAP;
            case 3 -> Level3.MAP;
            case 4 -> Level4.MAP;
            case 5 -> Level5.MAP;
            case 6 -> Level6.MAP;
            default -> null;
        };
        if (map == null) return new ArrayList<>();
        return buildFromMap(map);
    }

    // ================= Shared rules implementation =================

    private static List<Block> buildFromMap(String[] mapLines) {
        List<Block> blocks = new ArrayList<>();

        int cols = 15; // fits SCREEN_WIDTH with current spacing
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;

        java.util.function.BiFunction<Integer, Integer, Integer> rowToHits = (offsetRow, r) -> {
            int globalRow = offsetRow + r;
            if (globalRow < 3) return 3;
            if (globalRow < 6) return 2;
            return 1;
        };

        boolean[][] src = parseBinaryMask(java.util.Arrays.asList(mapLines));
        int srcH = src.length, srcW = src[0].length;

        int maxCols = cols;
        int maxRows = Math.max(8, (GameConfig.SCREEN_HEIGHT - GameConfig.BLOCKS_START_Y - 250) / GameConfig.BLOCK_ROW_SPACING);
        double sW = maxCols / (double) srcW;
        int rowsIfFitWidth = (int) Math.floor(srcH * sW + 0.5);
        int targetCols, targetRows;
        if (rowsIfFitWidth <= maxRows) {
            targetCols = maxCols;
            targetRows = Math.max(1, rowsIfFitWidth);
        } else {
            double sH = maxRows / (double) srcH;
            targetRows = maxRows;
            targetCols = Math.max(1, (int) Math.floor(srcW * sH + 0.5));
            if (targetCols > maxCols) targetCols = maxCols;
        }

        boolean[][] scaled = scaleMaskNearest(src, targetRows, targetCols);

        // Center horizontally within maxCols by padding columns
        boolean[][] grid = new boolean[targetRows][maxCols];
        int padLeft = (maxCols - targetCols) / 2;
        for (int r = 0; r < targetRows; r++) {
            for (int c = 0; c < targetCols; c++) {
                grid[r][padLeft + c] = scaled[r][c];
            }
        }

        int topPad = (maxRows - grid.length) / 2;
        addMask(blocks, startX, 0, topPad, grid, rowToHits);

        return blocks;
    }

    private static void addMask(
            List<Block> out,
            int startX,
            int offsetCol,
            int offsetRow,
            boolean[][] grid,
            java.util.function.BiFunction<Integer, Integer, Integer> rowToHits) {
        int rows = grid.length;
        int cols = grid[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c]) {
                    int x = startX + (offsetCol + c) * GameConfig.BLOCK_SPACING;
                    int y = GameConfig.BLOCKS_START_Y + (offsetRow + r) * GameConfig.BLOCK_ROW_SPACING;
                    int hits = rowToHits.apply(offsetRow, r);
                    out.add(new Block(x, y, hits));
                }
            }
        }
    }

    // Parse lines of '0'/'1' into boolean grid (rows x cols)
    private static boolean[][] parseBinaryMask(List<String> lines) {
        int h = lines.size();
        int w = 0;
        for (String s : lines) w = Math.max(w, s.trim().length());
        boolean[][] g = new boolean[h][w];
        for (int r = 0; r < h; r++) {
            String s = lines.get(r).trim();
            for (int c = 0; c < s.length(); c++) {
                char ch = s.charAt(c);
                g[r][c] = (ch == '1');
            }
        }
        return g;
    }

    // Nearest-neighbor sampling to preserve 0/1 pattern as closely as possible
    private static boolean[][] scaleMaskNearest(boolean[][] src, int targetRows, int targetCols) {
        int srcH = src.length, srcW = src[0].length;
        boolean[][] dst = new boolean[targetRows][targetCols];
        for (int tr = 0; tr < targetRows; tr++) {
            double srcY = ((tr + 0.5) * srcH) / targetRows - 0.5;
            int sy = (int) Math.round(srcY);
            if (sy < 0) sy = 0; else if (sy >= srcH) sy = srcH - 1;
            for (int tc = 0; tc < targetCols; tc++) {
                double srcX = ((tc + 0.5) * srcW) / targetCols - 0.5;
                int sx = (int) Math.round(srcX);
                if (sx < 0) sx = 0; else if (sx >= srcW) sx = srcW - 1;
                dst[tr][tc] = src[sy][sx];
            }
        }
        return dst;
    }
    
}