package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.utils.Array;

/**
 * Testne linije za Ljubljano (geo lat/lon).
 * BusRoute bo kasneje dobil rebuildWorld(zoom) v SmartCityTransit.
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
            .add(46.05897, 14.51062) // Železniška
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
            .add(46.05627, 14.50735) // Bavarski dvor
            .add(46.05897, 14.51062) // Železniška
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
        // L3: Express (Center -> BTC) z manj postajami
        // ------------------------
        Route geo3 = new Route("L3", "Center–BTC Express")
            .add(46.05123, 14.50330) // Kongresni trg
            .add(46.05897, 14.51062) // Železniška
            .add(46.06560, 14.54620); // BTC

        Array<Stop> s3 = new Array<>();
        s3.add(SampleStops.findByName(stops, "Kongresni trg"));
        s3.add(SampleStops.findByName(stops, "Zelezniska postaja"));
        s3.add(SampleStops.findByName(stops, "BTC City"));
        cleanupNulls(s3);

        routes.add(new BusRoute("L3", "Center–BTC Express", geo3, s3));

        return routes;
    }

    private static void cleanupNulls(Array<Stop> arr) {
        for (int i = arr.size - 1; i >= 0; i--) {
            if (arr.get(i) == null) arr.removeIndex(i);
        }
    }
}
