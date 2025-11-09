package function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import function.LifeManager;

public class LifeManagerTest {

    @BeforeEach
    void setup() {
        // reset trước mỗi test để độc lập
        LifeManager.resetLives();
    }

    @Test
    void resetSetsToDefault() {
        LifeManager.setLives(1);
        int v = LifeManager.resetLives();
        assertEquals(LifeManager.getDefaultLives(), v);
        assertEquals(LifeManager.getDefaultLives(), LifeManager.loadLives());
    }

    @Test
    void decrementNotBelowZero() {
        LifeManager.setLives(1);
        assertEquals(0, LifeManager.decrementLife());
        assertEquals(0, LifeManager.decrementLife()); // không âm
        assertEquals(0, LifeManager.loadLives());
    }

    @Test
    void setLivesClampedNonNegative() {
        LifeManager.setLives(-5);
        assertEquals(0, LifeManager.loadLives());
        LifeManager.setLives(10);
        assertEquals(10, LifeManager.loadLives());
    }

    @Test
    void sequenceResetSetDecrement() {
        assertEquals(LifeManager.getDefaultLives(), LifeManager.loadLives());
        LifeManager.setLives(5);
        assertEquals(5, LifeManager.loadLives());
        assertEquals(4, LifeManager.decrementLife());
        assertEquals(4, LifeManager.loadLives());
    }

    @Test
    void loadReflectsCurrent() {
        LifeManager.setLives(7);
        assertEquals(7, LifeManager.loadLives());
        LifeManager.resetLives();
        assertEquals(LifeManager.getDefaultLives(), LifeManager.loadLives());
    }
}
