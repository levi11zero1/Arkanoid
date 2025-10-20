package levels;

import entities.Block;
import java.util.ArrayList;
import java.util.List;
import utils.GameConfig;

public class Level1 {
    public static List<Block> create() {
        List<Block> blocks = new ArrayList<>();
        int cols = 12;
        int rows = 8;
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = startX + col * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;
                int hits = switch (row) {
                    case 0, 1 -> 3;
                    case 2, 3 -> 2;
                    default -> 1;
                };
                blocks.add(new Block(x, y, hits));
            }
        }
        return blocks;
    }
}
