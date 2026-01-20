package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public class BusRoute {
    public final String name;
    public final List<Vector2> points = new ArrayList<>();

    public BusRoute(String name) {
        this.name = name;
    }

    public void addPoint(float x, float y) {
        points.add(new Vector2(x, y));
    }
}
