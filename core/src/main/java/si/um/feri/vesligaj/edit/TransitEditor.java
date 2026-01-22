package si.um.feri.vesligaj.edit;

import com.badlogic.gdx.utils.Array;
import si.um.feri.vesligaj.transit.GeoPoint;
import si.um.feri.vesligaj.transit.Stop;

/**
 * Logika edit-mode (začetek): klik na mapo -> doda novo postajo.
 */
public class TransitEditor {

    private final UserEdits edits;
    private final int zoom;

    private Stop lastCreatedStop = null;

    public TransitEditor(UserEdits edits, int zoom) {
        this.edits = edits;
        this.zoom = zoom;
    }

    public Stop getLastCreatedStop() {
        return lastCreatedStop;
    }

    /** Vrne true, če je dodal postajo. */
    public boolean addStopAtWorld(Array<Stop> stops, float worldX, float worldY) {
        if (stops == null) return false;

        double lon = WorldToGeo.worldXToLon(worldX, zoom);
        double lat = WorldToGeo.worldYToLat(worldY, zoom);

        String name = "User stop " + edits.nextId();
        Stop s = new Stop(name, lat, lon);

        edits.addStop(s);
        stops.add(s);

        lastCreatedStop = s;
        return true;
    }

}
