package si.um.feri.vesligaj.edit;

import com.badlogic.gdx.utils.Array;
import si.um.feri.vesligaj.transit.Stop;

/**
 * Hrani user dodane entitete (zaenkrat samo postaje).
 * Kasneje lahko tukaj dodaš še user routes, povezave ipd.
 */
public class UserEdits {
    private final Array<Stop> userStops = new Array<>();
    private int counter = 1;

    public Array<Stop> getUserStops() {
        return userStops;
    }

    public int nextId() {
        return counter++;
    }

    public void addStop(Stop s) {
        if (s == null) return;
        userStops.add(s);
    }

    /** Po reloadData() pripne user stops na trenutno list-o stops. */
    public void applyTo(Array<Stop> stops) {
        if (stops == null) return;
        for (int i = 0; i < userStops.size; i++) {
            stops.add(userStops.get(i));
        }
    }
}
