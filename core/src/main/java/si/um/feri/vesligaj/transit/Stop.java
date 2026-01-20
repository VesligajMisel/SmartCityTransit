package si.um.feri.vesligaj.transit;

public class Stop {
    public final String name;
    public final double lat;
    public final double lon;

    public Stop(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }
}
