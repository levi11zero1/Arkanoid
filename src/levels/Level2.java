package levels;

import entities.Block;
import java.util.ArrayList;
import java.util.List;
import utils.GameConfig;

public class Level2 {
    // Checkerboard pattern với hits theo dải
    public static List<Block> create() {
        List<Block> blocks = new ArrayList<>();
        int cols = 12;
        int rows = 9;
        int totalWidth = cols * GameConfig.BLOCK_SPACING - (GameConfig.BLOCK_SPACING - GameConfig.BLOCK_WIDTH);
        int startX = (GameConfig.SCREEN_WIDTH - totalWidth) / 2;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if ((row + col) % 3 == 0) continue; // bỏ bớt để tạo hoa văn
                int x = startX + col * GameConfig.BLOCK_SPACING;
                int y = GameConfig.BLOCKS_START_Y + row * GameConfig.BLOCK_ROW_SPACING;
                int hits = (row < 3) ? 3 : (row < 6) ? 2 : 1;
                blocks.add(new Block(x, y, hits));
            }
        }
        return blocks;
    }
}
