package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import si.um.feri.vesligaj.map.GeoUtils;

/**
 * Konkretna avtobusna linija: geo route + postaje + world-cache za izris/animacijo.
 */
public class BusRoute {
    public final String id;
    public final String name;

    public final Route geoRoute;
    public final Array<Stop> stops;

    // cached world polyline (Y-up world pixels)
    private final Array<Vector2> worldPoints = new Array<>();
    private float[] segmentLengths = new float[0];
    private float totalLength = 0f;

    public BusRoute(String id, String name, Route geoRoute, Array<Stop> stops) {
        this.id = id;
        this.name = name;
        this.geoRoute = geoRoute;
        this.stops = (stops == null) ? new Array<>() : stops;
    }

    public void rebuildWorld(int zoom) {
        worldPoints.clear();
        totalLength = 0f;

        if (geoRoute == null || geoRoute.size() < 2) {
            segmentLengths = new float[0];
            return;
        }

        // build world points
        for (int i = 0; i < geoRoute.size(); i++) {
            GeoPoint gp = geoRoute.get(i);
            float x = (float) GeoUtils.lonToWorldPixelX(gp.lon, zoom);
            float y = (float) GeoUtils.latToWorldPixelY(gp.lat, zoom);
            worldPoints.add(new Vector2(x, y));
        }

        // compute lengths
        int segCount = worldPoints.size - 1;
        segmentLengths = new float[Math.max(0, segCount)];
        for (int i = 0; i < segCount; i++) {
            Vector2 a = worldPoints.get(i);
            Vector2 b = worldPoints.get(i + 1);
            float len = a.dst(b);
            segmentLengths[i] = len;
            totalLength += len;
        }
    }

    public Array<Vector2> getWorldPoints() {
        return worldPoints;
    }

    public float getTotalLength() {
        return totalLength;
    }

    public Vector2 getPositionAtDistance(float distance, Vector2 out) {
        if (out == null) out = new Vector2();
        if (worldPoints.size == 0) return out.set(0, 0);
        if (worldPoints.size == 1) return out.set(worldPoints.first());

        float d = distance;

        if (totalLength > 0f) {
            d = d % totalLength;
            if (d < 0f) d += totalLength;
        } else {
            return out.set(worldPoints.first());
        }

        for (int i = 0; i < segmentLengths.length; i++) {
            float segLen = segmentLengths[i];
            if (segLen <= 0.0001f) continue;

            if (d <= segLen) {
                Vector2 a = worldPoints.get(i);
                Vector2 b = worldPoints.get(i + 1);
                float t = d / segLen;
                return out.set(
                    a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t
                );
            }
            d -= segLen;
        }

        return out.set(worldPoints.peek());
    }
}
