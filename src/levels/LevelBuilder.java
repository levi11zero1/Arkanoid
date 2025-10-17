package levels;

import entities.Block;
import java.util.ArrayList;
import java.util.List;

public class LevelBuilder {
    
    public static List<Block> createLevel(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> Level1.create();
            case 2 -> Level2.create();
            case 3 -> Level3.create();
            case 4 -> Level4.create();
            case 5 -> Level5.create();
            case 6 -> Level6.create();
            default -> new ArrayList<>();
        };
    }
    
}