package si.um.feri.vesligaj.edit;

public class WorldToGeo {

    private static final int TILE_SIZE = 256;

    public static double worldXToLon(double worldX, int zoom) {
        double mapSize = (double) TILE_SIZE * (1 << zoom);
        return (worldX / mapSize) * 360.0 - 180.0;
    }

    public static double worldYToLat(double worldY, int zoom) {
        double mapSize = (double) TILE_SIZE * (1 << zoom);

        double n = Math.PI - (2.0 * Math.PI * worldY) / mapSize;
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
}
