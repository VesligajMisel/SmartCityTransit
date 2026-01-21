package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import si.um.feri.vesligaj.map.GeoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RouteStopIndex {

    private static class StopOnRoute {
        Stop stop;
        float distance;
        StopOnRoute(Stop s, float d) {
            stop = s;
            distance = d;
        }
    }

    private final List<StopOnRoute> ordered = new ArrayList<StopOnRoute>();

    /**
     * Ustvari indeks postaj po razdalji na route poliliniji (world px).
     * Predpogoj: route.rebuildWorld(zoom) mora biti že poklican.
     */
    public RouteStopIndex(BusRoute route, Array<Stop> stops, int zoom) {
        Array<Vector2> pts = route.getWorldPoints();
        int n = (pts == null) ? 0 : pts.size;
        if (n < 2) return;

        float[] prefix = new float[n];
        for (int i = 1; i < n; i++) {
            prefix[i] = prefix[i - 1] + pts.get(i - 1).dst(pts.get(i));
        }

        for (int si = 0; si < stops.size; si++) {
            Stop s = stops.get(si);

            Vector2 p = new Vector2(
                (float) GeoUtils.lonToWorldX(s.point.lon, zoom),
                (float) GeoUtils.latToWorldY(s.point.lat, zoom)
            );

            float best = Float.MAX_VALUE;
            float bestDist = 0f;

            for (int i = 0; i < n - 1; i++) {
                Vector2 a = pts.get(i);
                Vector2 b = pts.get(i + 1);

                float t = project(p, a, b);
                Vector2 proj = new Vector2(
                    a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t
                );

                float d2 = p.dst2(proj);
                if (d2 < best) {
                    best = d2;
                    bestDist = prefix[i] + a.dst(proj);
                }
            }

            ordered.add(new StopOnRoute(s, bestDist));
        }

        Collections.sort(ordered, new Comparator<StopOnRoute>() {
            @Override
            public int compare(StopOnRoute o1, StopOnRoute o2) {
                return Float.compare(o1.distance, o2.distance);
            }
        });
    }

    public Stop getNextStop(float busDist) {
        for (StopOnRoute sr : ordered) {
            if (sr.distance >= busDist) return sr.stop;
        }
        return ordered.isEmpty() ? null : ordered.get(ordered.size() - 1).stop;
    }

    private float project(Vector2 p, Vector2 a, Vector2 b) {
        Vector2 ab = new Vector2(b).sub(a);
        float len2 = ab.len2();
        if (len2 <= 0.000001f) return 0f;

        Vector2 ap = new Vector2(p).sub(a);
        float t = ap.dot(ab) / len2;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t;
    }
    public float getNextStopDistance(float busDist) {
        for (int i = 0; i < ordered.size(); i++) {
            StopOnRoute sr = ordered.get(i);
            if (sr.distance >= busDist) return sr.distance;
        }
        return ordered.isEmpty() ? 0f : ordered.get(ordered.size() - 1).distance;
    }
}
