package levels;

import entities.Block;
import java.util.ArrayList;
import java.util.List;
import utils.GameConfig;

public class Level4 {
    // Heart layout moved from old Level2
    public static List<Block> create() {
        List<Block> blocks = new ArrayList<>();
        boolean[][] heart = new boolean[][]{
            {false, true,  true,  false, false, false, false, false, false, true,  true,  false},
            {true,  true,  true,  true,  false, false, false, false, true,  true,  true,  true },
            {true,  true,  true,  true,  true,  false, false, true,  true,  true,  true,  true },
            {true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true },
            {false, true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  false},
            {false, false, true,  true,  true,  true,  true,  true,  true,  true,  false, false},
            {false, false, false, true,  true,  true,  true,  true,  true,  false, false, false},
            {false, false, false, false, true,  true,  true,  true,  false, false, false, false},
            {false, false, false, false, false, true,  true,  false, false, false, false, false},
            {false, false, false, false, false, true,  true,  false, false, false, false, false}
        };
        int cols = heart[0].length;
        int rows = heart.length;
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (!heart[row][col]) continue;
                int x = startX + col * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;
                int hits = (row < 3) ? 3 : (row < 6) ? 2 : 1;
                blocks.add(new Block(x, y, hits));
            }
        }
        return blocks;
    }
}
