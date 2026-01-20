package si.um.feri.vesligaj.transit;

/**
 * Model postaje.
 * Immutable in primeren za rendering/picking.
 */
public class Stop {
    public final String id;
    public final String name;
    public final GeoPoint point;

    public Stop(String name, double lat, double lon) {
        this(name, new GeoPoint(lat, lon));
    }

    public Stop(String name, GeoPoint point) {
        this.name = name;
        this.point = point;
        this.id = slug(name);
    }

    private static String slug(String s) {
        if (s == null) return "stop";
        String out = s.trim().toLowerCase();
        out = out.replace('č', 'c').replace('š', 's').replace('ž', 'z');
        out = out.replaceAll("[^a-z0-9]+", "-");
        out = out.replaceAll("(^-|-$)", "");
        return out.isEmpty() ? "stop" : out;
    }

    @Override
    public String toString() {
        return "Stop{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", point=" + point + '}';
    }
}
