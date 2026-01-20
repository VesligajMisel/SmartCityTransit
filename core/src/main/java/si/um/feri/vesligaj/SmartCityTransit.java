package si.um.feri.vesligaj;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.vesligaj.map.GeoUtils;
import si.um.feri.vesligaj.map.TileManager;
import si.um.feri.vesligaj.transit.Bus;
import si.um.feri.vesligaj.transit.BusRoute;
import si.um.feri.vesligaj.transit.SampleRoutes;
import si.um.feri.vesligaj.transit.SampleStops;
import si.um.feri.vesligaj.transit.Stop;

public class SmartCityTransit extends ApplicationAdapter {

    private static final int TILE_SIZE = 256;
    private static final int MAP_ZOOM = 14;

    private static final double START_LAT = 46.056946;
    private static final double START_LON = 14.505751;

    // pick/marker
    private static final float STOP_RADIUS = 6f;
    private static final float PICK_RADIUS = 14f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private TileManager tileManager;

    private int lastScreenX, lastScreenY;
    private int downScreenX, downScreenY;
    private boolean dragging = false;

    private BusRoute testRoute;
    private Bus bus;

    private Array<Stop> stops;
    private BitmapFont font;
    private Stop selectedStop = null;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        tileManager = new TileManager();

        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        viewport.apply();

        // center camera on Ljubljana
        int osmTileX = GeoUtils.lonToTileX(START_LON, MAP_ZOOM);
        int osmTileY = GeoUtils.latToTileY(START_LAT, MAP_ZOOM);

        int worldTileX = osmTileX;
        int worldTileY = GeoUtils.osmTileYToWorldTileY(osmTileY, MAP_ZOOM);

        camera.position.set(
            worldTileX * TILE_SIZE + TILE_SIZE * 0.5f,
            worldTileY * TILE_SIZE + TILE_SIZE * 0.5f,
            0f
        );
        camera.zoom = 1f;
        camera.update();

        // route + bus
        testRoute = SampleRoutes.createTestRoute(MAP_ZOOM);
        bus = new Bus(testRoute, 30f);

        // stops
        stops = SampleStops.ljubljanaStops();
        font = new BitmapFont();


        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                dragging = true;
                lastScreenX = screenX;
                lastScreenY = screenY;
                downScreenX = screenX;
                downScreenY = screenY;
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                dragging = false;

                // treat as click if mouse didn't move much
                int dx = screenX - downScreenX;
                int dy = screenY - downScreenY;
                if (dx * dx + dy * dy <= 6 * 6) {
                    Vector3 v = new Vector3(screenX, screenY, 0);
                    viewport.unproject(v);
                    selectedStop = pickStop(v.x, v.y);
                }

                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (!dragging) return false;

                int dx = screenX - lastScreenX;
                int dy = screenY - lastScreenY;

                camera.position.x -= dx * camera.zoom;
                camera.position.y += dy * camera.zoom;

                lastScreenX = screenX;
                lastScreenY = screenY;
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                float factor = (amountY > 0) ? 1.15f : 0.87f;
                camera.zoom *= factor;
                camera.zoom = Math.max(0.25f, Math.min(camera.zoom, 10.0f));
                return true;
            }
        });
    }

    @Override
    public void render() {
        camera.update();
        bus.update(Gdx.graphics.getDeltaTime());

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        float halfW = viewport.getWorldWidth() * 0.5f * camera.zoom;
        float halfH = viewport.getWorldHeight() * 0.5f * camera.zoom;

        float left = camera.position.x - halfW;
        float right = camera.position.x + halfW;
        float bottom = camera.position.y - halfH;
        float top = camera.position.y + halfH;

        int minTileX = (int) Math.floor(left / TILE_SIZE);
        int maxTileX = (int) Math.floor(right / TILE_SIZE);
        int minTileY = (int) Math.floor(bottom / TILE_SIZE);
        int maxTileY = (int) Math.floor(top / TILE_SIZE);

        // ---- MAP TILES ----
        batch.begin();
        for (int tx = minTileX - 1; tx <= maxTileX + 1; tx++) {
            for (int ty = minTileY - 1; ty <= maxTileY + 1; ty++) {
                int osmX = GeoUtils.wrapTileX(tx, MAP_ZOOM);
                int osmY = GeoUtils.worldTileYToOsmTileY(ty, MAP_ZOOM);

                if (osmY < 0 || osmY >= (1 << MAP_ZOOM)) continue;

                float drawX = tx * TILE_SIZE;
                float drawY = ty * TILE_SIZE;

                batch.draw(tileManager.getTile(osmX, osmY, MAP_ZOOM),
                    drawX, drawY, TILE_SIZE, TILE_SIZE);
            }
        }
        batch.end();

        // ---- ROUTE (red polyline) ----
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 0.2f, 0.2f, 1f);

        for (int i = 0; i < testRoute.points.size() - 1; i++) {
            Vector2 a = testRoute.points.get(i);
            Vector2 b = testRoute.points.get(i + 1);
            shapeRenderer.line(a, b);
        }
        shapeRenderer.end();

        // ---- BUS (filled circle) ----
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.6f, 1f, 1f);
        bus.render(shapeRenderer);
        shapeRenderer.end();

        // ---- STOPS + LABELS + SELECTION ----
        drawStops();
    }

    private void drawStops() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        float r = STOP_RADIUS * camera.zoom;

        // stops
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        for (int i = 0; i < stops.size; i++) {
            Stop s = stops.get(i);
            float x = (float) GeoUtils.lonToWorldX(s.lon, MAP_ZOOM);
            float y = (float) GeoUtils.latToWorldY(s.lat, MAP_ZOOM);
            shapeRenderer.circle(x, y, r);
        }
        shapeRenderer.end();

        // selection highlight
        if (selectedStop != null) {
            float sx = (float) GeoUtils.lonToWorldX(selectedStop.lon, MAP_ZOOM);
            float sy = (float) GeoUtils.latToWorldY(selectedStop.lat, MAP_ZOOM);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 1f, 0.2f, 1f);
            shapeRenderer.circle(sx, sy, (STOP_RADIUS + 6f) * camera.zoom);
            shapeRenderer.end();
        }

        // labels (only when zoomed-in enough)
        if (camera.zoom <= 1.2f) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            for (int i = 0; i < stops.size; i++) {
                Stop s = stops.get(i);
                float x = (float) GeoUtils.lonToWorldX(s.lon, MAP_ZOOM);
                float y = (float) GeoUtils.latToWorldY(s.lat, MAP_ZOOM);
                font.draw(batch, s.name, x + (10f * camera.zoom), y + (10f * camera.zoom));
            }

            // extra label for selected stop (always show, slightly offset)
            if (selectedStop != null) {
                float sx = (float) GeoUtils.lonToWorldX(selectedStop.lon, MAP_ZOOM);
                float sy = (float) GeoUtils.latToWorldY(selectedStop.lat, MAP_ZOOM);
                font.draw(batch, "Izbrano: " + selectedStop.name, sx + 12f, sy + 28f);
            }
            batch.end();
        }
    }

    private Stop pickStop(float worldX, float worldY) {
        Stop best = null;
        float bestD2 = Float.MAX_VALUE;

        for (int i = 0; i < stops.size; i++) {
            Stop s = stops.get(i);

            float sx = (float) GeoUtils.lonToWorldX(s.lon, MAP_ZOOM);
            float sy = (float) GeoUtils.latToWorldY(s.lat, MAP_ZOOM);

            float dx = worldX - sx;
            float dy = worldY - sy;
            float d2 = dx * dx + dy * dy;

            if (d2 <= (PICK_RADIUS * PICK_RADIUS) && d2 < bestD2) {
                bestD2 = d2;
                best = s;
            }
        }

        return best;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void dispose() {
        tileManager.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        if (font != null) font.dispose();
    }
}
