package si.um.feri.vesligaj.transit;

public class GeoPoint {
    public final double lat;
    public final double lon;

    public GeoPoint(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double distanceSquaredTo(double lat, double lon) {
        double dLat = this.lat - lat;
        double dLon = this.lon - lon;
        return dLat * dLat + dLon * dLon;
    }

    @Override
    public String toString() {
        return "GeoPoint{" + "lat=" + lat + ", lon=" + lon + '}';
    }
}
