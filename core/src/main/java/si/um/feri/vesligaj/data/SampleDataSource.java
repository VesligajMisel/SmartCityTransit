package si.um.feri.vesligaj.data;

import com.badlogic.gdx.utils.Array;
import si.um.feri.vesligaj.transit.BusRoute;
import si.um.feri.vesligaj.transit.SampleRoutes;
import si.um.feri.vesligaj.transit.SampleStops;
import si.um.feri.vesligaj.transit.Stop;

public class SampleDataSource implements TransitDataSource {
    @Override public Array<Stop> loadStops() { return SampleStops.ljubljanaStops(); }
    @Override public Array<BusRoute> loadRoutes(Array<Stop> stops) { return SampleRoutes.ljubljanaRoutes(stops); }
    @Override public String getName() { return "DEMO"; }
}
