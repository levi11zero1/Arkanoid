package levels;

import entities.Block;
import java.util.ArrayList;
import java.util.List;
import utils.GameConfig;

public class LevelBuilder {
    
    public static List<Block> createLevel(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> createLevel1();
            case 2 -> createLevel2();
            case 3 -> createLevel3();
            default -> new ArrayList<>();
        };
    }
    
    private static List<Block> createLevel1() {
        List<Block> blocks = new ArrayList<>();
        
        int cols = 12;
        int rows = 8;
        
        // Calculate centered starting position
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = startX + col * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;

                int hits = switch (row) {
                    case 0, 1 -> 3;  // Top 2 rows: 3 hits
                    case 2, 3 -> 2;  // Middle 2 rows: 2 hits
                    default -> 1;    // Bottom rows: 1 hit
                };
                
                blocks.add(new Block(x, y, hits));
            }
        }
        
        return blocks;
    }

    private static List<Block> createLevel2() {
        List<Block> blocks = new ArrayList<>();
        
        int cols = 12;
        int rows = 9;
        
        // Calculate centered starting position
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;
        
        // Create a checkerboard pattern with varying hits
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // Skip some blocks to create pattern
                if ((row + col) % 3 == 0) {
                    continue;
                }
                
                int x = startX + col * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;
                
                int hits;
                if (row < 3) {
                    hits = 3;
                } else if (row < 6) {
                    hits = 2;
                } else {
                    hits = 1;
                }
                
                blocks.add(new Block(x, y, hits));
            }
        }
        
        return blocks;
    }

    private static List<Block> createLevel3() {
        List<Block> blocks = new ArrayList<>();
        
        int cols = 12;
        int maxRows = 10;
        
        // Calculate centered starting position
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;
        
        // Create a pyramid/diamond pattern
        for (int row = 0; row < maxRows; row++) {
            int blocksInRow;
            int offsetCols;
            
            if (row < 5) {
                // Expanding pyramid
                blocksInRow = cols - (row * 2);
                offsetCols = row;
            } else {
                // Contracting pyramid
                int reverseRow = row - 5;
                blocksInRow = cols - (reverseRow * 2);
                offsetCols = reverseRow;
            }
            
            for (int col = 0; col < blocksInRow; col++) {
                int x = startX + (offsetCols + col) * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;
                
                // Varied hits based on position
                int hits = ((row + col) % 3) + 1;
                blocks.add(new Block(x, y, hits));
            }
        }
        
        return blocks;
    }
}