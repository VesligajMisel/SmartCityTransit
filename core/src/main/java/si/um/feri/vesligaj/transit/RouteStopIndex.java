package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.math.Vector2;
import si.um.feri.vesligaj.map.GeoUtils;

import java.util.ArrayList;
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

    private final List<StopOnRoute> ordered = new ArrayList<>();

    public RouteStopIndex(BusRoute route, List<Stop> stops, int zoom) {
        int n = route.points.size();
        if (n < 2) return;

        float[] prefix = new float[n];
        for (int i = 1; i < n; i++) {
            prefix[i] = prefix[i - 1] + route.points.get(i - 1).dst(route.points.get(i));
        }

        for (Stop s : stops) {
            Vector2 p = new Vector2(
                (float) GeoUtils.lonToWorldX(s.lon, zoom),
                (float) GeoUtils.latToWorldY(s.lat, zoom)
            );

            float best = Float.MAX_VALUE;
            float bestDist = 0f;

            for (int i = 0; i < n - 1; i++) {
                Vector2 a = route.points.get(i);
                Vector2 b = route.points.get(i + 1);

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

        ordered.sort(Comparator.comparingDouble(o -> o.distance));
    }

    public Stop getNextStop(float busDist) {
        for (StopOnRoute sr : ordered) {
            if (sr.distance >= busDist) return sr.stop;
        }
        return ordered.isEmpty() ? null : ordered.get(ordered.size() - 1).stop;
    }

    private float project(Vector2 p, Vector2 a, Vector2 b) {
        Vector2 ab = new Vector2(b).sub(a);
        Vector2 ap = new Vector2(p).sub(a);
        float t = ap.dot(ab) / ab.len2();
        return Math.max(0f, Math.min(1f, t));
    }
}
