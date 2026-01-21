package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.Comparator;

public class StopArrivalsCalculator {

    /**
     * Vrne ETA-je (v sekundah) do izbrane postaje za vse buse,
     * ki vozijo po linijah, ki vsebujejo to postajo.
     *
     * @param maxPerRoute koliko prihodov na linijo (npr. 2)
     */
    public static Array<Arrival> computeArrivalsForStop(
        Stop stop,
        Array<Bus> buses,
        ObjectMap<BusRoute, RouteStopIndex> indexMap,
        int maxPerRoute
    ) {
        Array<Arrival> out = new Array<>();
        if (stop == null || buses == null || indexMap == null) return out;

        // najprej zberemo vse kandidate
        for (int i = 0; i < buses.size; i++) {
            Bus b = buses.get(i);
            BusRoute r = b.getRoute();
            if (r == null) continue;

            RouteStopIndex idx = indexMap.get(r);
            if (idx == null) continue;

            float stopDist = idx.getDistanceForStop(stop);
            if (stopDist < 0f) continue; // ta linija ne gre čez to postajo

            float eta = estimateEtaSec(b, r, stopDist);
            if (eta >= 0f) {
                out.add(new Arrival(r, b, stop, eta));
            }
        }

        // sortiraj po ETA (globalno)
        out.sort(new Comparator<Arrival>() {
            @Override
            public int compare(Arrival a, Arrival b) {
                return Float.compare(a.etaSec, b.etaSec);
            }
        });

        if (maxPerRoute > 0) {
            ObjectMap<BusRoute, Integer> perRouteCount = new ObjectMap<>();
            Array<Arrival> filtered = new Array<>();

            for (int i = 0; i < out.size; i++) {
                Arrival a = out.get(i);
                Integer c = perRouteCount.get(a.route);
                int count = (c == null) ? 0 : c;

                if (count < maxPerRoute) {
                    filtered.add(a);
                    perRouteCount.put(a.route, count + 1);
                }
            }
            return filtered;
        }

        return out;
    }

    private static float estimateEtaSec(Bus bus, BusRoute route, float targetStopDist) {
        float total = route.getTotalLength();
        if (total <= 0.001f) return -1f;

        float busDist = bus.getDistanceOnRoute();

        float remain = targetStopDist - busDist;
        if (bus.isLoop()) {
            if (remain < 0f) remain += total;
        } else {
            if (remain < 0f) return -1f;
        }

        float spd = bus.getSpeedPx();
        if (spd <= 0.001f) return -1f;

        float eta = remain / spd;

        // če bus trenutno stoji, dodaj njegov trenutni preostali wait
        if (bus.isWaiting()) {
            eta += Math.max(0f, bus.getWaitTimer());
        }

        return eta;
    }
}
