package levels;

import entities.Block;
import java.util.ArrayList;
import java.util.List;
import utils.GameConfig;

public class Level3 {
    // Pyramid/diamond pattern
    public static List<Block> create() {
        List<Block> blocks = new ArrayList<>();
        int cols = 12;
        int maxRows = 10;
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;

        for (int row = 0; row < maxRows; row++) {
            int blocksInRow;
            int offsetCols;
            if (row < 5) {
                blocksInRow = cols - (row * 2);
                offsetCols = row;
            } else {
                int reverseRow = row - 5;
                blocksInRow = cols - (reverseRow * 2);
                offsetCols = reverseRow;
            }
            for (int col = 0; col < blocksInRow; col++) {
                int x = startX + (offsetCols + col) * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;
                int hits = ((row + col) % 3) + 1;
                blocks.add(new Block(x, y, hits));
            }
        }
        return blocks;
    }
}
