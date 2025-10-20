# Level System Refactor Summary

## Changes Made (October 20, 2025)

### Objective
Centralized level-building rules from Level6 into `LevelBuilder.java` and converted all level classes (Level1-6) to map-only definitions.

### What Changed

#### Before
- Each level class (Level1-6) contained its own `create()` method with unique building logic
- Level6 had advanced features: binary mask parsing, aspect-ratio-preserving scaling, hit-tier assignment by row bands
- Code duplication across levels; hard to maintain consistency

#### After
- **LevelBuilder.java** now contains all shared building logic:
  - Binary mask parsing ('0'/'1' strings → boolean grid)
  - Aspect-ratio-preserving nearest-neighbor scaling to fit 15 columns
  - Horizontal & vertical centering
  - Hit-tier assignment by row bands:
    - Top 3 rows → 3 hits (MAGENTA blocks)
    - Next 3 rows → 2 hits (ORANGE blocks)
    - Remaining rows → 1 hit (RED blocks)

- **Level1-6.java** are now map-only classes:
  - Each defines a `public static final String[] MAP`
  - Only '0' and '1' characters (0=empty, 1=block)
  - No building logic; just data

### Benefits
✅ **Single source of truth**: All levels follow Level6's proven rules  
✅ **Easy to add new levels**: Just create a new `LevelN` with a `MAP` field  
✅ **Less code duplication**: ~150 lines per level → ~10 lines  
✅ **Consistent behavior**: All levels scale, center, and assign hits uniformly  
✅ **Maintainable**: Change rules once in `LevelBuilder`, applies everywhere  

### How to Add a New Level
1. Create `src/levels/LevelN.java`:
   ```java
   package levels;
   public class LevelN {
       public static final String[] MAP = new String[]{
           "0110",
           "1111",
           "0110",
       };
   }
   ```
2. Add a case in `LevelBuilder.createLevel(int)`:
   ```java
   case N -> LevelN.MAP;
   ```
3. Update `GameConfig.MAX_LEVELS` if needed.

### Files Modified
- `src/levels/LevelBuilder.java` – Added buildFromMap() + helper methods with docs
- `src/levels/Level1.java` – Now map-only (8×12 solid grid)
- `src/levels/Level2.java` – Now map-only (checkerboard pattern)
- `src/levels/Level3.java` – Now map-only (diamond/pyramid)
- `src/levels/Level4.java` – Now map-only (heart shape)
- `src/levels/Level5.java` – Now map-only (LV monogram)
- `src/levels/Level6.java` – Now map-only (original complex shape)

### Testing
- ✅ Compiled successfully (0 errors, 30 warnings unrelated to refactor)
- ✅ Game launches without runtime errors
- ✅ All levels load and display correctly
- ✅ Block hit tiers (colors) render as expected

### Notes
- Level maps can be any size; LevelBuilder automatically scales them to fit the 15-column grid with aspect ratio preserved.
- Nearest-neighbor sampling ensures crisp edges in scaled block patterns.
- Row-based hit tiers mean **global row index** (after vertical centering) determines hits, not map row index.

---
**Author**: GitHub Copilot  
**Date**: October 20, 2025
