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
        
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 8; col++) {
                int x = GameConfig.BLOCKS_START_X + col * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;

                int hits = switch (row) {
                    case 0 -> 3;  
                    case 1 -> 2;
                    default -> 1;
                };
                
                blocks.add(new Block(x, y, hits));
            }
        }
        
        return blocks;
    }

    private static List<Block> createLevel2() {
        List<Block> blocks = new ArrayList<>();
        
        for (int row = 0; row < 6; row++) {
            int blocksInRow = 8 - row;
            int startX = GameConfig.BLOCKS_START_X + (row * (GameConfig.BLOCK_SPACING / 2));
            
            for (int col = 0; col < blocksInRow; col++) {
                int x = startX + col * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;
                
                int hits;
                if (row < 2) {
                    hits = 3;
                } else if (row < 4) {
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
        int centerX = GameConfig.SCREEN_WIDTH / 2 - GameConfig.BLOCK_WIDTH / 2;
        
        for (int row = 0; row < 7; row++) {
            int blocksInRow = row < 4 ? row + 1 : 7 - row;
            int startX = centerX - (blocksInRow * GameConfig.BLOCK_SPACING / 2);
            
            for (int col = 0; col < blocksInRow; col++) {
                int x = startX + col * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;
                
                //randomize
                int hits = ((row + col) % 3) + 1;
                blocks.add(new Block(x, y, hits));
            }
        }
        
        return blocks;
    }
}