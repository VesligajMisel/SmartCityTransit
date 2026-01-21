package si.um.feri.vesligaj.data;

import com.badlogic.gdx.utils.Array;
import si.um.feri.vesligaj.transit.BusRoute;
import si.um.feri.vesligaj.transit.Stop;

public interface TransitDataSource {
    Array<Stop> loadStops();
    Array<BusRoute> loadRoutes(Array<Stop> stops);
    String getName(); // "DEMO" ali "GTFS"
}
