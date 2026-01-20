package si.um.feri.vesligaj.map;

import com.badlogic.gdx.graphics.Texture;

public class Tile {
    public final int x, y, zoom;
    public final Texture texture;

    public Tile(int x, int y, int zoom, Texture texture) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.texture = texture;
    }
}
