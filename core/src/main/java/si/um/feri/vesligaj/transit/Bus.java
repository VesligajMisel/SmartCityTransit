package si.um.feri.vesligaj.transit;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Bus {

    private final BusRoute route;

    private float speedPx;

    private float distance;

    private boolean loop = true;

    private final Vector2 position = new Vector2();

    private boolean waiting = false;
    private float waitTimer = 0f;

    // tuning
    private float stopTimeSec = 1.6f;
    private float stopRadiusPx = 10f;
    private float lastStopDistance = -999999f;

    private float stopTimeMinSec = 1.0f;
    private float stopTimeMaxSec = 2.4f;

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

    public boolean isWaiting() {
        return waiting;
    }

    public float getWaitTimer() {
        return waitTimer;
    }

    public void setStopTimeSec(float stopTimeSec) {
        this.stopTimeSec = Math.max(0f, stopTimeSec);
    }

    public void setStopRadiusPx(float stopRadiusPx) {
        this.stopRadiusPx = Math.max(0f, stopRadiusPx);
    }

    public void setStopTimeRangeSec(float minSec, float maxSec) {
        stopTimeMinSec = Math.max(0f, Math.min(minSec, maxSec));
        stopTimeMaxSec = Math.max(stopTimeMinSec, Math.max(minSec, maxSec));
    }

    public void checkStop(RouteStopIndex idx) {
        if (idx == null) return;
        if (waiting) return;

        float total = route.getTotalLength();
        if (total <= 0f) return;

        float nextStopDist = idx.getNextStopDistance(distance);

        float diff = nextStopDist - distance;
        if (loop) {
            if (diff < 0f) diff += total;
        } else {
            if (diff < 0f) return;
        }

        if (diff <= stopRadiusPx) {
            // prepreči, da bi isti stop sprožil večkrat
            if (Math.abs(nextStopDist - lastStopDistance) > stopRadiusPx * 0.75f) {
                waiting = true;
                waitTimer = MathUtils.random(stopTimeMinSec, stopTimeMaxSec);
                lastStopDistance = nextStopDist;
            }
        }
    }

    public void update(float dt) {
        float total = route.getTotalLength();
        if (total <= 0f) return;

        // ---- WAITING (ADDED) ----
        if (waiting) {
            waitTimer -= dt;
            if (waitTimer <= 0f) {
                waiting = false;
                waitTimer = 0f;
            }
            return;
        }

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

    public void render(ShapeRenderer sr) {
        Vector2 p = getPosition();

        // halo
        sr.circle(p.x, p.y, 8.5f, 20);

        // body (caller naj pred tem nastavi drugo barvo)
        sr.circle(p.x, p.y, 6.0f, 20);
    }
}
