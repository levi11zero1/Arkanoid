package entities;

import java.util.List;
import java.util.ArrayList;

/**
 * EntityManager skeleton. Manage entities lifecycle (add/remove/update) here.
 */
public class EntityManager {
    private final List<Object> entities = new ArrayList<>();

    public void update(long delta) { }

    public List<Object> getEntities() { return entities; }

    public void add(Object e) { entities.add(e); }

    public void remove(Object e) { entities.remove(e); }
}
