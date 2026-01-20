package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.utils.Array;

public class SampleStops {

    public static Array<Stop> ljubljanaStops() {
        Array<Stop> s = new Array<>();

        // Center
        s.add(new Stop("Kongresni trg", 46.05123, 14.50330));
        s.add(new Stop("Bavarski dvor", 46.05627, 14.50735));
        s.add(new Stop("Zelezniska postaja", 46.05897, 14.51062));

        // West / North
        s.add(new Stop("Tivoli", 46.05830, 14.49490));

        // East
        s.add(new Stop("UKC Ljubljana", 46.05640, 14.52330));

        // Far east
        s.add(new Stop("BTC City", 46.06560, 14.54620));

        return s;
    }

    /** Helper: poišči stop po imenu (da SampleRoutes ostane čist) */
    public static Stop findByName(Array<Stop> stops, String name) {
        for (Stop st : stops) {
            if (st.name.equals(name)) return st;
        }
        return null;
    }
}
