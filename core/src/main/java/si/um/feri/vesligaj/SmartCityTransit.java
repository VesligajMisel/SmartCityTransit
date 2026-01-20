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
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.vesligaj.map.GeoUtils;
import si.um.feri.vesligaj.map.TileManager;
import si.um.feri.vesligaj.transit.Bus;
import si.um.feri.vesligaj.transit.BusRoute;
import si.um.feri.vesligaj.transit.RouteStopIndex;
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
    private static final float BUS_PICK_RADIUS = 18f;

    private OrthographicCamera camera;
    private Viewport viewport;

    // UI camera (screen-space HUD)
    private OrthographicCamera uiCamera;
    private Viewport uiViewport;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private TileManager tileManager;

    private int lastScreenX, lastScreenY;
    private int downScreenX, downScreenY;
    private boolean dragging = false;

    private Array<Stop> stops;
    private Array<BusRoute> routes;
    private Array<Bus> buses;

    private ObjectMap<BusRoute, RouteStopIndex> routeStopIndex = new ObjectMap<>();

    private BitmapFont font;
    private Stop selectedStop = null;
    private Bus selectedBus = null;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        tileManager = new TileManager();

        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        viewport.apply();

        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(1280, 720, uiCamera);
        uiViewport.apply();

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

        // ---- DATA ----
        stops = SampleStops.ljubljanaStops();
        routes = SampleRoutes.ljubljanaRoutes(stops);

        // build world cache + stop index for each route
        for (int i = 0; i < routes.size; i++) {
            BusRoute r = routes.get(i);
            r.rebuildWorld(MAP_ZOOM);
            routeStopIndex.put(r, new RouteStopIndex(r, r.stops, MAP_ZOOM));
        }

        // create multiple buses (2 per route)
        buses = new Array<>();
        for (int i = 0; i < routes.size; i++) {
            BusRoute r = routes.get(i);
            float total = r.getTotalLength();
            if (total <= 0f) continue;

            float baseSpeed = 35f + i * 8f;

            buses.add(new Bus(r, baseSpeed, total * 0.12f));
            buses.add(new Bus(r, baseSpeed * 0.92f, total * 0.58f));
        }

        font = new BitmapFont();
        font.getData().setScale(1.4f);
        font.setColor(1f, 1f, 1f, 1f);

        // ---- INPUT ----
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

                    // 1) try pick bus first
                    Bus b = pickBus(v.x, v.y);
                    if (b != null) {
                        selectedBus = b;
                        selectedStop = null;
                        return true;
                    }

                    // 2) then pick stop
                    selectedStop = pickStop(v.x, v.y);
                    if (selectedStop != null) {
                        selectedBus = null;
                    }
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

        float dt = Gdx.graphics.getDeltaTime();
        for (int i = 0; i < buses.size; i++) {
            buses.get(i).update(dt);
        }

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        // visible bounds in world pixels
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

        // ---- ROUTES (shadow + colored) ----
        drawRoutes();

        // ---- BUSES + STOPS + SELECTION + LABELS ----
        drawStopsAndBuses();

        // ---- HUD (screen-space) ----
        drawHud();
    }

    private void drawRoutes() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        // shadow
        Gdx.gl.glLineWidth(4f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0f, 0f, 0f, 0.35f);

        for (int r = 0; r < routes.size; r++) {
            Array<Vector2> pts = routes.get(r).getWorldPoints();
            for (int i = 0; i < pts.size - 1; i++) {
                shapeRenderer.line(pts.get(i), pts.get(i + 1));
            }
        }
        shapeRenderer.end();

        // main colored
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for (int r = 0; r < routes.size; r++) {
            setRouteColor(shapeRenderer, r, 0.95f);
            Array<Vector2> pts = routes.get(r).getWorldPoints();
            for (int i = 0; i < pts.size - 1; i++) {
                shapeRenderer.line(pts.get(i), pts.get(i + 1));
            }
        }

        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);
    }

    private void drawStopsAndBuses() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        float stopR = STOP_RADIUS * camera.zoom;

        // --- FILLED (stops + buses) ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // stops: halo + fill
        for (int i = 0; i < stops.size; i++) {
            Stop s = stops.get(i);
            float x = (float) GeoUtils.lonToWorldX(s.point.lon, MAP_ZOOM);
            float y = (float) GeoUtils.latToWorldY(s.point.lat, MAP_ZOOM);

            // halo
            shapeRenderer.setColor(0f, 0f, 0f, 0.30f);
            shapeRenderer.circle(x, y, stopR + (2.5f * camera.zoom), 18);

            // fill
            shapeRenderer.setColor(1f, 0.20f, 0.20f, 0.95f);
            shapeRenderer.circle(x, y, stopR, 18);
        }

        // buses: halo then body colored by route
        for (int i = 0; i < buses.size; i++) {
            Bus b = buses.get(i);
            Vector2 p = b.getPosition();

            // halo
            shapeRenderer.setColor(0f, 0f, 0f, 0.30f);
            shapeRenderer.circle(p.x, p.y, 9.0f * camera.zoom, 20);

            // body
            int routeIndex = routes.indexOf(b.getRoute(), true);
            setRouteColor(shapeRenderer, routeIndex, 0.95f);
            shapeRenderer.circle(p.x, p.y, 6.5f * camera.zoom, 20);

            // small white dot
            shapeRenderer.setColor(1f, 1f, 1f, 0.85f);
            shapeRenderer.circle(p.x, p.y, 2.0f * camera.zoom, 12);
        }

        shapeRenderer.end();

        // --- SELECTION (outline) ---
        if (selectedStop != null) {
            float sx = (float) GeoUtils.lonToWorldX(selectedStop.point.lon, MAP_ZOOM);
            float sy = (float) GeoUtils.latToWorldY(selectedStop.point.lat, MAP_ZOOM);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 1f, 0.2f, 1f);
            shapeRenderer.circle(sx, sy, (STOP_RADIUS + 7f) * camera.zoom, 24);
            shapeRenderer.end();
        }

        if (selectedBus != null) {
            Vector2 p = selectedBus.getPosition();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 1f, 0.2f, 1f);
            shapeRenderer.circle(p.x, p.y, 12.0f * camera.zoom, 24);
            shapeRenderer.end();
        }

        // --- LABELS (world-space, only when zoomed-in enough) ---
        if (camera.zoom <= 1.2f) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();

            for (int i = 0; i < stops.size; i++) {
                Stop s = stops.get(i);
                float x = (float) GeoUtils.lonToWorldX(s.point.lon, MAP_ZOOM);
                float y = (float) GeoUtils.latToWorldY(s.point.lat, MAP_ZOOM);
                font.draw(batch, s.name, x + (10f * camera.zoom), y + (12f * camera.zoom));
            }

            if (selectedStop != null) {
                float sx = (float) GeoUtils.lonToWorldX(selectedStop.point.lon, MAP_ZOOM);
                float sy = (float) GeoUtils.latToWorldY(selectedStop.point.lat, MAP_ZOOM);
                font.draw(batch, "Izbrano: " + selectedStop.name, sx + 12f, sy + 30f);
            }

            batch.end();
        }
    }

    private void drawHud() {
        uiViewport.apply();
        uiCamera.update();

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.65f);

        // HUD background box
        float pad = 10f;
        float boxW = 360f;
        float boxH = 120f;

        shapeRenderer.rect(
            pad,
            uiViewport.getWorldHeight() - boxH - pad,
            boxW,
            boxH
        );

        shapeRenderer.end();


        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        float x = 16f;
        float y = uiViewport.getWorldHeight() - 16f;

        // Title
        font.setColor(1f, 1f, 1f, 0.92f);
        font.draw(batch, "SmartCityTransit", x, y);
        y -= 18f;

        // Selected bus info
        if (selectedBus != null) {
            BusRoute r = selectedBus.getRoute();
            Stop next = null;

            RouteStopIndex idx = routeStopIndex.get(r);
            if (idx != null) {
                next = idx.getNextStop(selectedBus.getDistanceOnRoute());
            }

            font.setColor(1f, 1f, 1f, 0.92f);
            font.draw(batch, "Selected BUS", x, y); y -= 16f;

            font.setColor(0.9f, 0.9f, 0.9f, 0.90f);
            font.draw(batch, "Line: " + r.id + " - " + r.name, x, y); y -= 16f;

            font.draw(batch, "Speed: " + format1(selectedBus.getSpeedPx()) + " px/s", x, y); y -= 16f;

            if (next != null) {
                font.draw(batch, "Next stop: " + next.name, x, y); y -= 16f;
            } else {
                font.draw(batch, "Next stop: -", x, y); y -= 16f;
            }
        }

        // Selected stop info
        if (selectedStop != null) {
            font.setColor(1f, 1f, 1f, 0.92f);
            font.draw(batch, "Selected STOP", x, y); y -= 16f;

            font.setColor(0.9f, 0.9f, 0.9f, 0.90f);
            font.draw(batch, "Name: " + selectedStop.name, x, y); y -= 16f;
            font.draw(batch, "Lat: " + format5(selectedStop.point.lat) + "  Lon: " + format5(selectedStop.point.lon), x, y); y -= 16f;
        }

        batch.end();

        // restore world viewport for next frame
        viewport.apply();
    }

    private Bus pickBus(float worldX, float worldY) {
        Bus best = null;
        float bestD2 = Float.MAX_VALUE;

        float r = BUS_PICK_RADIUS * camera.zoom;
        float r2 = r * r;

        for (int i = 0; i < buses.size; i++) {
            Bus b = buses.get(i);
            Vector2 p = b.getPosition();

            float dx = worldX - p.x;
            float dy = worldY - p.y;
            float d2 = dx * dx + dy * dy;

            if (d2 <= r2 && d2 < bestD2) {
                bestD2 = d2;
                best = b;
            }
        }
        return best;
    }

    private Stop pickStop(float worldX, float worldY) {
        Stop best = null;
        float bestD2 = Float.MAX_VALUE;

        float r2 = (PICK_RADIUS * PICK_RADIUS);

        for (int i = 0; i < stops.size; i++) {
            Stop s = stops.get(i);

            float sx = (float) GeoUtils.lonToWorldX(s.point.lon, MAP_ZOOM);
            float sy = (float) GeoUtils.latToWorldY(s.point.lat, MAP_ZOOM);

            float dx = worldX - sx;
            float dy = worldY - sy;
            float d2 = dx * dx + dy * dy;

            if (d2 <= r2 && d2 < bestD2) {
                bestD2 = d2;
                best = s;
            }
        }

        return best;
    }

    private void setRouteColor(ShapeRenderer sr, int routeIndex, float alpha) {
        int idx = (routeIndex % 5 + 5) % 5;
        switch (idx) {
            case 0:
                sr.setColor(0.15f, 0.60f, 0.95f, alpha); // blue
                break;
            case 1:
                sr.setColor(0.20f, 0.80f, 0.55f, alpha); // green
                break;
            case 2:
                sr.setColor(0.95f, 0.55f, 0.20f, alpha); // orange
                break;
            case 3:
                sr.setColor(0.70f, 0.35f, 0.95f, alpha); // purple
                break;
            default:
                sr.setColor(0.95f, 0.25f, 0.45f, alpha); // pink-red
                break;
        }
    }

    private String format1(float v) {
        return String.format(java.util.Locale.US, "%.1f", v);
    }

    private String format5(double v) {
        return String.format(java.util.Locale.US, "%.5f", v);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiViewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        tileManager.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        if (font != null) font.dispose();
    }
}
