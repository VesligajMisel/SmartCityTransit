package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.utils.Array;

/**
 * Geo pot (lat/lon točke). Brez render logike.
 */
public class Route {
    public final String id;
    public final String name;
    public final Array<GeoPoint> points = new Array<>();

    public Route(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Route add(double lat, double lon) {
        points.add(new GeoPoint(lat, lon));
        return this;
    }

    public int size() {
        return points.size;
    }

    public GeoPoint get(int idx) {
        return points.get(idx);
    }
}
