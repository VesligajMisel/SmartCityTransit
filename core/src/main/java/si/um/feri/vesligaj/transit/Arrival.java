package si.um.feri.vesligaj.transit;

public class Arrival {
    public final BusRoute route;
    public final Bus bus;
    public final Stop stop;
    public final float etaSec;

    public Arrival(BusRoute route, Bus bus, Stop stop, float etaSec) {
        this.route = route;
        this.bus = bus;
        this.stop = stop;
        this.etaSec = etaSec;
    }
}
