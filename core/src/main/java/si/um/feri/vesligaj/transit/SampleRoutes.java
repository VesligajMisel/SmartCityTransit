package si.um.feri.vesligaj.transit;

import si.um.feri.vesligaj.map.GeoUtils;

public class SampleRoutes {

    public static BusRoute createTestRoute(int zoom) {
        BusRoute r = new BusRoute("Testna linija");

        add(r, 46.05123, 14.50330, zoom);
        add(r, 46.05627, 14.50735, zoom);
        add(r, 46.05897, 14.51062, zoom);
        add(r, 46.06560, 14.54620, zoom);

        return r;
    }

    private static void add(BusRoute r, double lat, double lon, int zoom) {
        float x = (float) GeoUtils.lonToWorldX(lon, zoom);
        float y = (float) GeoUtils.latToWorldY(lat, zoom);
        r.addPoint(x, y);
    }
}
