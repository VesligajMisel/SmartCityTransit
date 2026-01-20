package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

public class Bus {

    private final BusRoute route;
    private final float speedPx;

    private float[] segLen;
    private float totalLen;
    private float s;

    private final Vector2 position = new Vector2();

    public Bus(BusRoute route, float speedPx) {
        this.route = route;
        this.speedPx = speedPx;
        buildRoute();
    }

    private void buildRoute() {
        List<Vector2> pts = route.points;
        if (pts.size() < 2) return;

        segLen = new float[pts.size() - 1];
        totalLen = 0f;

        for (int i = 0; i < pts.size() - 1; i++) {
            float d = pts.get(i).dst(pts.get(i + 1));
            segLen[i] = d;
            totalLen += d;
        }

        s = 0f;
        position.set(pts.get(0));
    }

    public void update(float dt) {
        if (totalLen <= 0f) return;

        s += speedPx * dt;
        if (s >= totalLen) s = totalLen;
    }

    public float getDistanceOnRoute() {
        return s;
    }

    public Vector2 getPosition() {
        float dist = s;

        for (int i = 0; i < segLen.length; i++) {
            if (dist > segLen[i]) {
                dist -= segLen[i];
                continue;
            }

            Vector2 a = route.points.get(i);
            Vector2 b = route.points.get(i + 1);
            float t = segLen[i] == 0 ? 0 : dist / segLen[i];
            position.set(a).lerp(b, t);
            break;
        }
        return position;
    }

    public void render(ShapeRenderer sr) {
        Vector2 p = getPosition();
        sr.circle(p.x, p.y, 6f);
    }
}
