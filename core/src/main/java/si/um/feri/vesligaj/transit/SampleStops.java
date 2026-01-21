package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.utils.Array;

public class SampleStops {

    public static Array<Stop> ljubljanaStops() {
        Array<Stop> s = new Array<>();

        // demo postaje
        s.add(new Stop("Kongresni trg", 46.05123, 14.50330));
        s.add(new Stop("Zelezniska postaja", 46.05897, 14.51062));
        s.add(new Stop("Bavarski dvor", 46.05627, 14.50735));
        s.add(new Stop("Tivoli", 46.05830, 14.49490));
        s.add(new Stop("UKC Ljubljana", 46.05640, 14.52330));
        s.add(new Stop("BTC City", 46.06560, 14.54620));
        s.add(new Stop("Presernov trg", 46.05108, 14.50611));
        s.add(new Stop("Trg republike", 46.05170, 14.50260));
        s.add(new Stop("Bezigrad", 46.06520, 14.51320));
        s.add(new Stop("Stozice", 46.08120, 14.52030));
        s.add(new Stop("Siska", 46.06710, 14.48990));
        s.add(new Stop("Vič", 46.03670, 14.48990));
        s.add(new Stop("Rudnik", 46.02890, 14.53260));
        s.add(new Stop("Moste", 46.06270, 14.53610));
        s.add(new Stop("Rozna dolina", 46.04960, 14.48760));

        return s;
    }

    public static Stop findByName(Array<Stop> stops, String name) {
        if (stops == null || name == null) return null;
        for (int i = 0; i < stops.size; i++) {
            Stop s = stops.get(i);
            if (s != null && name.equalsIgnoreCase(s.name)) return s;
        }
        return null;
    }
}
