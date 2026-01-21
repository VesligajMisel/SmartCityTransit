package si.um.feri.vesligaj.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import si.um.feri.vesligaj.transit.BusRoute;
import si.um.feri.vesligaj.transit.GeoPoint;
import si.um.feri.vesligaj.transit.Route;
import si.um.feri.vesligaj.transit.Stop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class GtfsDataSource implements TransitDataSource {

    private final FileHandle gtfsDir; // folder od data
    private final Map<String, Stop> stopByGtfsId = new HashMap<>();

    public GtfsDataSource(FileHandle gtfsDir) {
        this.gtfsDir = gtfsDir;
    }

    @Override
    public Array<Stop> loadStops() {
        stopByGtfsId.clear();

        FileHandle f = gtfsDir.child("stops.txt");
        if (!f.exists()) f = gtfsDir.child("stops.TXT");

        if (!f.exists()) {
            Gdx.app.log("GTFS", "stops.txt NOT FOUND in: " + gtfsDir.path());
            return new Array<>();
        }

        String text = f.readString("UTF-8");
        String[] lines = text.split("\\r?\\n");

        Array<Stop> out = new Array<>();

        if (lines.length <= 1) {
            Gdx.app.log("GTFS", "stops.txt is empty/only header");
            return out;
        }

        // header: stop_id,stop_code,stop_name,stop_lat,stop_lon
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            List<String> cols = splitCsvLine(line);
            if (cols.size() < 5) continue;

            String stopId = cols.get(0).trim();
            String stopName = cols.get(2).trim();
            String latStr = cols.get(3).trim();
            String lonStr = cols.get(4).trim();

            if (stopId.isEmpty() || stopName.isEmpty()) continue;

            double lat, lon;
            try {
                lat = Double.parseDouble(latStr);
                lon = Double.parseDouble(lonStr);
            } catch (Exception e) {
                continue;
            }

            Stop s = new Stop(stopName, lat, lon);
            out.add(s);
            stopByGtfsId.put(stopId, s);
        }

        Gdx.app.log("GTFS", "Loaded stops=" + out.size + " (file=" + f.path() + ")");
        return out;
    }

    @Override
    public Array<BusRoute> loadRoutes(Array<Stop> stops) {

        //routes.txt
        FileHandle routesF = gtfsDir.child("routes.txt");
        if (!routesF.exists()) routesF = gtfsDir.child("routes.TXT");

        if (!routesF.exists()) {
            Gdx.app.log("GTFS", "routes.txt NOT FOUND in: " + gtfsDir.path());
            return new Array<>();
        }

        // route_id -> (shortName, longName)
        class RouteInfo {
            String routeId;
            String shortName;
            String longName;
            RouteInfo(String routeId, String shortName, String longName) {
                this.routeId = routeId;
                this.shortName = shortName;
                this.longName = longName;
            }
        }

        Map<String, RouteInfo> routeInfo = new HashMap<>();
        {
            String[] lines = routesF.readString("UTF-8").split("\\r?\\n");
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                List<String> cols = splitCsvLine(line);
                if (cols.size() < 4) continue;

                String routeId = cols.get(0).trim();
                String shortName = cols.get(2).trim();
                String longName = cols.get(3).trim();

                if (routeId.isEmpty()) continue;

                routeInfo.put(routeId, new RouteInfo(routeId, shortName, longName));
            }
        }

        Gdx.app.log("GTFS", "Parsed routes=" + routeInfo.size());

        // trips.txt (route_id -> pick first trip_id)
        FileHandle tripsF = gtfsDir.child("trips.txt");
        if (!tripsF.exists()) tripsF = gtfsDir.child("trips.TXT");

        Map<String, String> chosenTripForRoute = new HashMap<>();
        if (tripsF.exists()) {
            String[] lines = tripsF.readString("UTF-8").split("\\r?\\n");
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                // header: route_id,service_id,trip_id,...
                List<String> cols = splitCsvLine(line);
                if (cols.size() < 3) continue;

                String routeId = cols.get(0).trim();
                String tripId = cols.get(2).trim();
                if (routeId.isEmpty() || tripId.isEmpty()) continue;

                if (!chosenTripForRoute.containsKey(routeId)) {
                    chosenTripForRoute.put(routeId, tripId);
                }
            }
        } else {
            Gdx.app.log("GTFS", "trips.txt missing -> can't build routes from stop_times");
        }

        //stop_times.txt (trip_id -> list of (seq, stop_id))
        FileHandle stopTimesF = gtfsDir.child("stop_times.txt");
        if (!stopTimesF.exists()) stopTimesF = gtfsDir.child("stop_times.TXT");

        // trip_id -> list entries
        class StopTimeEntry {
            int seq;
            String stopId;
            StopTimeEntry(int seq, String stopId) {
                this.seq = seq;
                this.stopId = stopId;
            }
        }

        Map<String, ArrayList<StopTimeEntry>> stopTimesByTrip = new HashMap<>();
        if (stopTimesF.exists()) {
            String[] lines = stopTimesF.readString("UTF-8").split("\\r?\\n");
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                // header: trip_id,arrival_time,departure_time,stop_id,stop_sequence
                List<String> cols = splitCsvLine(line);
                if (cols.size() < 5) continue;

                String tripId = cols.get(0).trim();
                String stopId = cols.get(3).trim();
                String seqStr = cols.get(4).trim();

                if (tripId.isEmpty() || stopId.isEmpty()) continue;

                int seq;
                try {
                    seq = Integer.parseInt(seqStr);
                } catch (Exception e) {
                    continue;
                }

                ArrayList<StopTimeEntry> arr = stopTimesByTrip.get(tripId);
                if (arr == null) {
                    arr = new ArrayList<>();
                    stopTimesByTrip.put(tripId, arr);
                }
                arr.add(new StopTimeEntry(seq, stopId));
            }
        } else {
            Gdx.app.log("GTFS", "stop_times.txt missing -> can't build routes geometry");
        }

        //build BusRoute list
        Array<BusRoute> out = new Array<>();

        for (RouteInfo ri : routeInfo.values()) {
            String routeId = ri.routeId;

            // choose trip
            String tripId = chosenTripForRoute.get(routeId);
            ArrayList<StopTimeEntry> st = (tripId == null) ? null : stopTimesByTrip.get(tripId);

            Array<Stop> routeStops = new Array<>();
            Route geo = new Route(
                ri.shortName != null && !ri.shortName.isEmpty() ? ri.shortName : routeId,
                (ri.longName != null && !ri.longName.isEmpty()) ? ri.longName : ri.shortName
            );

            if (st != null && !st.isEmpty()) {
                // sort by stop_sequence
                st.sort((a, b) -> Integer.compare(a.seq, b.seq));

                // build ordered stops
                HashSet<String> seen = new HashSet<>();
                for (StopTimeEntry e : st) {
                    if (e.stopId == null) continue;
                    if (seen.contains(e.stopId)) continue;
                    seen.add(e.stopId);

                    Stop s = stopByGtfsId.get(e.stopId);
                    if (s == null) continue;

                    routeStops.add(s);
                    geo.add(s.point.lat, s.point.lon);
                }
            }

            String id = (ri.shortName != null && !ri.shortName.isEmpty()) ? ri.shortName : routeId;
            String name = (ri.longName != null && !ri.longName.isEmpty()) ? ri.longName : id;

            out.add(new BusRoute(id, name, geo, routeStops));
        }

        Gdx.app.log("GTFS", "Built BusRoutes=" + out.size + " (dir=" + gtfsDir.path() + ")");
        return out;
    }

    @Override
    public String getName() {
        return "GTFS";
    }

    // ---CSV --
    private static List<String> splitCsvLine(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
