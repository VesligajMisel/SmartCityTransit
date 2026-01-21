package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.utils.Array;

/**
 * Testne linije za Ljubljano (geo lat/lon).
 */
public class SampleRoutes {

    public static Array<BusRoute> ljubljanaRoutes(Array<Stop> stops) {
        Array<BusRoute> routes = new Array<>();

        // ------------------------
        // L1: Center Loop (krog okoli centra)
        // ------------------------
        Route geo1 = new Route("L1", "Center Loop")
            .add(46.05123, 14.50330) // Kongresni trg
            .add(46.05627, 14.50735) // Bavarski dvor
            .add(46.05897, 14.51062) // Zelezniska
            .add(46.05640, 14.52330) // UKC
            .add(46.05123, 14.50330); // nazaj

        Array<Stop> s1 = new Array<>();
        s1.add(SampleStops.findByName(stops, "Kongresni trg"));
        s1.add(SampleStops.findByName(stops, "Bavarski dvor"));
        s1.add(SampleStops.findByName(stops, "Zelezniska postaja"));
        s1.add(SampleStops.findByName(stops, "UKC Ljubljana"));
        cleanupNulls(s1);

        routes.add(new BusRoute("L1", "Center Loop", geo1, s1));

        // ------------------------
        // L2: West -> Center -> East (Tivoli do BTC)
        // ------------------------
        Route geo2 = new Route("L2", "West–East")
            .add(46.05830, 14.49490) // Tivoli
            .add(46.05627, 14.50735) // Bavarski
            .add(46.05897, 14.51062) // Zelezniska
            .add(46.05640, 14.52330) // UKC
            .add(46.06560, 14.54620); // BTC

        Array<Stop> s2 = new Array<>();
        s2.add(SampleStops.findByName(stops, "Tivoli"));
        s2.add(SampleStops.findByName(stops, "Bavarski dvor"));
        s2.add(SampleStops.findByName(stops, "Zelezniska postaja"));
        s2.add(SampleStops.findByName(stops, "UKC Ljubljana"));
        s2.add(SampleStops.findByName(stops, "BTC City"));
        cleanupNulls(s2);

        routes.add(new BusRoute("L2", "West–East", geo2, s2));

        // ------------------------
        // L3: Express (Center -> BTC)
        // ------------------------
        Route geo3 = new Route("L3", "Center–BTC Express")
            .add(46.05123, 14.50330) // Kongresni trg
            .add(46.05897, 14.51062) // Zelezniska
            .add(46.06560, 14.54620); // BTC

        Array<Stop> s3 = new Array<>();
        s3.add(SampleStops.findByName(stops, "Kongresni trg"));
        s3.add(SampleStops.findByName(stops, "Zelezniska postaja"));
        s3.add(SampleStops.findByName(stops, "BTC City"));
        cleanupNulls(s3);

        routes.add(new BusRoute("L3", "Center–BTC Express", geo3, s3));

        // ------------------------
        // L4: North–South (Bezigrad -> Center -> Rudnik)
        // ------------------------
        Route geo4 = new Route("L4", "North–South")
            .add(46.06520, 14.51320) // Bezigrad
            .add(46.05897, 14.51062) // Zelezniska
            .add(46.05108, 14.50611) // Presernov trg
            .add(46.05123, 14.50330) // Kongresni trg
            .add(46.03670, 14.48990) // Vic
            .add(46.02890, 14.53260); // Rudnik

        Array<Stop> s4 = new Array<>();
        s4.add(SampleStops.findByName(stops, "Bezigrad"));
        s4.add(SampleStops.findByName(stops, "Zelezniska postaja"));
        s4.add(SampleStops.findByName(stops, "Presernov trg"));
        s4.add(SampleStops.findByName(stops, "Kongresni trg"));
        s4.add(SampleStops.findByName(stops, "Vič"));
        s4.add(SampleStops.findByName(stops, "Rudnik"));
        cleanupNulls(s4);

        routes.add(new BusRoute("L4", "North–South", geo4, s4));

        // ------------------------
        // L5: West Ring (Rozna dolina -> Tivoli -> Siska -> Bezigrad -> Center)
        // ------------------------
        Route geo5 = new Route("L5", "West Ring")
            .add(46.04960, 14.48760) // Rozna dolina
            .add(46.05830, 14.49490) // Tivoli
            .add(46.06710, 14.48990) // Siska
            .add(46.06520, 14.51320) // Bezigrad
            .add(46.05897, 14.51062) // Zelezniska
            .add(46.05170, 14.50260); // Trg republike

        Array<Stop> s5 = new Array<>();
        s5.add(SampleStops.findByName(stops, "Rozna dolina"));
        s5.add(SampleStops.findByName(stops, "Tivoli"));
        s5.add(SampleStops.findByName(stops, "Siska"));
        s5.add(SampleStops.findByName(stops, "Bezigrad"));
        s5.add(SampleStops.findByName(stops, "Zelezniska postaja"));
        s5.add(SampleStops.findByName(stops, "Trg republike"));
        cleanupNulls(s5);

        routes.add(new BusRoute("L5", "West Ring", geo5, s5));

        // ------------------------
        // L6: East Connector (UKC -> Moste -> BTC -> Stozice)
        // ------------------------
        Route geo6 = new Route("L6", "East Connector")
            .add(46.05640, 14.52330) // UKC
            .add(46.06270, 14.53610) // Moste
            .add(46.06560, 14.54620) // BTC
            .add(46.08120, 14.52030); // Stozice

        Array<Stop> s6 = new Array<>();
        s6.add(SampleStops.findByName(stops, "UKC Ljubljana"));
        s6.add(SampleStops.findByName(stops, "Moste"));
        s6.add(SampleStops.findByName(stops, "BTC City"));
        s6.add(SampleStops.findByName(stops, "Stozice"));
        cleanupNulls(s6);

        routes.add(new BusRoute("L6", "East Connector", geo6, s6));

        return routes;
    }

    private static void cleanupNulls(Array<Stop> arr) {
        for (int i = arr.size - 1; i >= 0; i--) {
            if (arr.get(i) == null) arr.removeIndex(i);
        }
    }
}
