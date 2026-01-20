package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import si.um.feri.vesligaj.map.GeoUtils;

public class Route {
    public final String id;
    public final String name;
    public final Array<GeoPoint> points = new Array<>();
    // dolžine segmentov v world koordinatah
    private float[] segmentLengths;
    private float totalLength = 0f;


    public Route(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Route add(double lat, double lon) {
        points.add(new GeoPoint(lat, lon));
        return this;
    }
    public void buildWorldRoute(int zoom) {
        int n = points.size;
        if (n < 2) return;

        segmentLengths = new float[n - 1];
        totalLength = 0f;

        for (int i = 0; i < n - 1; i++) {
            GeoPoint a = points.get(i);
            GeoPoint b = points.get(i + 1);

            float ax = (float) GeoUtils.lonToWorldPixelX(a.lon, zoom);
            float ay = (float) GeoUtils.latToWorldPixelY(a.lat, zoom);
            float bx = (float) GeoUtils.lonToWorldPixelX(b.lon, zoom);
            float by = (float) GeoUtils.latToWorldPixelY(b.lat, zoom);

            float dx = bx - ax;
            float dy = by - ay;
            float len = (float) Math.sqrt(dx * dx + dy * dy);

            segmentLengths[i] = len;
            totalLength += len;
        }
    }
    public Vector2 getPositionAt(float distance, int zoom, Vector2 out) {
        if (segmentLengths == null || segmentLengths.length == 0) return out;

        float d = Math.min(distance, totalLength);

        for (int i = 0; i < segmentLengths.length; i++) {
            if (d > segmentLengths[i]) {
                d -= segmentLengths[i];
                continue;
            }

            GeoPoint a = points.get(i);
            GeoPoint b = points.get(i + 1);

            float ax = (float) GeoUtils.lonToWorldPixelX(a.lon, zoom);
            float ay = (float) GeoUtils.latToWorldPixelY(a.lat, zoom);
            float bx = (float) GeoUtils.lonToWorldPixelX(b.lon, zoom);
            float by = (float) GeoUtils.latToWorldPixelY(b.lat, zoom);

            float t = segmentLengths[i] == 0 ? 0f : d / segmentLengths[i];

            out.set(
                ax + (bx - ax) * t,
                ay + (by - ay) * t
            );
            return out;
        }

        return out;
    }
}
