package si.um.feri.vesligaj.transit;

public class TransitClock {
    private float timeSec = 0f;

    public void update(float dt) {
        timeSec += dt;
    }

    public float getTimeSec() {
        return timeSec;
    }

    public void reset() {
        timeSec = 0f;
    }
}
