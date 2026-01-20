package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Bus {

    private final BusRoute route;

    // hitrost v world px/s (world pixels per second)
    private float speedPx;

    // razdalja po poti (world px)
    private float distance;

    private boolean loop = true;

    // cache da ne alociramo vsaki frame
    private final Vector2 position = new Vector2();

    /**
     * @param route BusRoute (mora imeti rebuildWorld(zoom) že izveden)
     * @param speedPx hitrost v world px/s
     * @param startDistance začetni zamik po poti (world px) -> za več avtobusov na isti liniji
     */
    public Bus(BusRoute route, float speedPx, float startDistance) {
        this.route = route;
        this.speedPx = Math.max(1f, speedPx);
        this.distance = Math.max(0f, startDistance);
    }

    public BusRoute getRoute() {
        return route;
    }

    public void setSpeedPx(float speedPx) {
        this.speedPx = Math.max(1f, speedPx);
    }

    public float getSpeedPx() {
        return speedPx;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isLoop() {
        return loop;
    }

    public float getDistanceOnRoute() {
        return distance;
    }

    public void update(float dt) {
        float total = route.getTotalLength();
        if (total <= 0f) return;

        distance += speedPx * dt;

        if (loop) {
            distance = distance % total;
            if (distance < 0f) distance += total;
        } else {
            if (distance > total) distance = total;
        }
    }

    public Vector2 getPosition() {
        return route.getPositionAtDistance(distance, position);
    }

    /**
     * “Lepši” render: halo + body.
     * Barvo naj nastavi caller (po liniji), tu samo izrišemo oblike.
     */
    public void render(ShapeRenderer sr) {
        Vector2 p = getPosition();

        // halo
        sr.circle(p.x, p.y, 8.5f, 20);

        // body (caller naj pred tem nastavi drugo barvo)
        sr.circle(p.x, p.y, 6.0f, 20);
    }
}
