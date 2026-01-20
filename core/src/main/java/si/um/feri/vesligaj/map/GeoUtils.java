package si.um.feri.vesligaj.map;

public class GeoUtils {

    private static final int TILE_SIZE = 256;

    public static int getTileSize() {
        return TILE_SIZE;
    }

    public static int wrapTileX(int x, int zoom) {
        int n = 1 << zoom;
        int r = x % n;
        return (r < 0) ? (r + n) : r;
    }

    // OSM tileY (top-origin) <-> WORLD tileY (bottom-origin / Y-up)
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

    // OSM/CARTO tileY (origin at TOP)
    public static int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor(
            (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI)
                / 2.0 * (1 << zoom)
        );
    }

    // lon/lat -> world pixels (Y-up)
    public static double lonToWorldPixelX(double lon, int zoom) {
        double n = (double) (1 << zoom) * TILE_SIZE;
        return (lon + 180.0) / 360.0 * n;
    }

    public static double latToWorldPixelY(double lat, int zoom) {
        double n = (double) (1 << zoom) * TILE_SIZE;
        double latRad = Math.toRadians(lat);
        double merc = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
        double yTopLeft = (1.0 - merc / Math.PI) / 2.0 * n;
        return n - yTopLeft; // flip to Y-up
    }

    // convenience (same as worldPixelX/Y)
    public static double lonToWorldX(double lon, int zoom) {
        return lonToWorldPixelX(lon, zoom);
    }

    public static double latToWorldY(double lat, int zoom) {
        return latToWorldPixelY(lat, zoom);
    }

    // ---------------------------
    // ADDITIONS (for picking, UI, cleaner tile math)
    // ---------------------------

    /** world pixels (Y-up) -> longitude */
    public static double worldXToLon(double worldX, int zoom) {
        double n = (double) (1 << zoom) * TILE_SIZE;
        return (worldX / n) * 360.0 - 180.0;
    }

    /** world pixels (Y-up) -> latitude */
    public static double worldYToLat(double worldY, int zoom) {
        double n = (double) (1 << zoom) * TILE_SIZE;

        // back to OSM's top-left Y
        double yTopLeft = n - worldY;

        // inverse Web Mercator
        double mercN = Math.PI * (1.0 - 2.0 * yTopLeft / n);
        double latRad = Math.atan(Math.sinh(mercN));
        return Math.toDegrees(latRad);
    }

    /** world pixel X -> tileX at zoom (NOT wrapped) */
    public static int worldPixelToTileX(double worldX) {
        return (int) Math.floor(worldX / TILE_SIZE);
    }

    /** world pixel Y (Y-up) -> world tileY (bottom-origin) */
    public static int worldPixelToWorldTileY(double worldY) {
        return (int) Math.floor(worldY / TILE_SIZE);
    }
}
