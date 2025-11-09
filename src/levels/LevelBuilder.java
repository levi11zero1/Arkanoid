package levels;

import entities.Block;
import java.util.ArrayList;
import java.util.List;
import utils.GameConfig;
public class LevelBuilder {
    public static List<Block> buildFromMapDirect(String[] mapLines) {
        List<Block> blocks = new ArrayList<>();
        int[][] grid = parseHitMask(java.util.Arrays.asList(mapLines));
        int cols = grid[0].length;
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;
        addMaskWithHits(blocks, startX, 0, 0, grid);
        return blocks;
    }
    
    public static List<Block> createLevel(int levelNumber) {
        String[] map = switch (levelNumber) {
            case 0 -> Level0.MAP;
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

    private static List<Block> buildFromMap(String[] mapLines) {
        List<Block> blocks = new ArrayList<>();

        int cols = 15;
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;

    int[][] src = parseHitMask(java.util.Arrays.asList(mapLines));
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

        int[][] scaled = scaleHitMaskNearest(src, targetRows, targetCols);
        int[][] grid = new int[targetRows][maxCols];
        int padLeft = (maxCols - targetCols) / 2;
        for (int r = 0; r < targetRows; r++) {
            for (int c = 0; c < targetCols; c++) {
                grid[r][padLeft + c] = scaled[r][c];
            }
        }
        int topPad = (maxRows - grid.length) / 2;
        addMaskWithHits(blocks, startX, 0, topPad, grid);

        return blocks;
    }

    private static void addMaskWithHits(
            List<Block> out,
            int startX,
            int offsetCol,
            int offsetRow,
            int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int hits = grid[r][c];
                if (hits > 0 || hits == GameConfig.UNDESTRUCTABLE_BLOCK) {
                    int x = startX + (offsetCol + c) * GameConfig.BLOCK_SPACING;
                    int y = GameConfig.BLOCKS_START_Y + (offsetRow + r) * GameConfig.BLOCK_ROW_SPACING;
                    out.add(new Block(x, y, hits));
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private static boolean[][] parseBinaryMask(List<String> lines) {
        // Deprecated: use parseHitMask
        throw new UnsupportedOperationException("Use parseHitMask instead");
    }

    private static int[][] parseHitMask(List<String> lines) {
        int h = lines.size();
        int w = 0;
        for (String s : lines) w = Math.max(w, s.trim().length());
        int[][] g = new int[h][w];
        for (int r = 0; r < h; r++) {
            String s = lines.get(r).trim();
            for (int c = 0; c < s.length(); c++) {
                char ch = s.charAt(c);
                if (ch == '1' || ch == '2' || ch == '3') {
                    g[r][c] = ch - '0';
                } else if (ch == 'X' || ch == 'x') {
                    g[r][c] = GameConfig.UNDESTRUCTABLE_BLOCK;
                } else {
                    g[r][c] = 0;
                }
            }
        }
        return g;
    }
    @SuppressWarnings("unused")
    private static boolean[][] scaleMaskNearest(boolean[][] src, int targetRows, int targetCols) {
        throw new UnsupportedOperationException("Use scaleHitMaskNearest instead");
    }

    private static int[][] scaleHitMaskNearest(int[][] src, int targetRows, int targetCols) {
        int srcH = src.length, srcW = src[0].length;
        int[][] dst = new int[targetRows][targetCols];
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