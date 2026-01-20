package si.um.feri.vesligaj.map;

/**
 * Pomožni razred za pretvorbo geografskih koordinat
 */
public class GeoUtils {

    private static final int TILE_SIZE = 256;

    public static int wrapTileX(int x, int zoom) {
        int n = 1 << zoom;
        int r = x % n;
        return (r < 0) ? (r + n) : r;
    }
    public static int osmTileYToWorldTileY(int osmTileY, int zoom) {
        int n = 1 << zoom;
        return (n - 1) - osmTileY;
    }
    public static int worldTileYToOsmTileY(int worldTileY, int zoom) {
        int n = 1 << zoom;
        return (n - 1) - worldTileY;
    }

    public static int lonToTileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }
    public static int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor(
            (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI)
                / 2.0 * (1 << zoom)
        );
    }

}
