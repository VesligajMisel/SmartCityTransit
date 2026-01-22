package si.um.feri.vesligaj;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
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

import si.um.feri.vesligaj.data.SampleDataSource;
import si.um.feri.vesligaj.data.TransitDataSource;
import si.um.feri.vesligaj.data.GtfsDataSource;
import si.um.feri.vesligaj.map.GeoUtils;
import si.um.feri.vesligaj.map.TileManager;
import si.um.feri.vesligaj.transit.Bus;
import si.um.feri.vesligaj.transit.BusRoute;
import si.um.feri.vesligaj.transit.RouteStopIndex;
import si.um.feri.vesligaj.transit.SampleRoutes;
import si.um.feri.vesligaj.transit.SampleStops;
import si.um.feri.vesligaj.transit.Stop;
import si.um.feri.vesligaj.ui.HudPanel;

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
    private BusRoute selectedRoute = null;
    private Array<BusRoute> stopRoutes = null; // routes that pass through selectedStop
    private static final float ROUTE_PICK_TOLERANCE = 14f;

    private Vector2 cameraTarget = null;
    private float uiTime = 0f;
    private boolean showOnlySelected = false;
    private boolean followSelectedBus = false;
    private final Vector2 followTmp = new Vector2();


    // HUD layout constants
    private static final float HUD_PAD = 10f;
    private static final float HUD_BOX_W = 380f;
    private static final float HUD_BOX_H = 260f;
    private static final float HUD_TEXT_X = 18f;
    private static final float HUD_LINE_H = 22f;
    private static final float HUD_SECTION_GAP = 12f;
    private static final float HUD_ITEM_GAP = 6f;
    private HudPanel hudPanel;
    private TransitDataSource dataSource;
    private TransitDataSource demoSource;
    private TransitDataSource gtfsSource;
    private boolean useGtfs = false;


    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        tileManager = new TileManager();

        hudPanel = new HudPanel();
        hudPanel.setShowOnlySelected(showOnlySelected);

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

        // -- DEMO IN REAL --
        demoSource = new SampleDataSource();
        gtfsSource = new GtfsDataSource(Gdx.files.internal("Data"));

        dataSource = demoSource;
        reloadData();


        // build world cache + stop index for each route
        for (int i = 0; i < routes.size; i++) {
            BusRoute r = routes.get(i);
            r.rebuildWorld(MAP_ZOOM);
            routeStopIndex.put(r, new RouteStopIndex(r, r.stops, MAP_ZOOM));
        }

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

                int dx = screenX - downScreenX;
                int dy = screenY - downScreenY;
                if (dx * dx + dy * dy <= 6 * 6) {

                    //HUD clicK
                    HudPanel.ClickResult cr = hudPanel.handleClick(screenX, screenY, uiViewport, routes);

                    if (cr.type == HudPanel.ClickType.PREV_PAGE || cr.type == HudPanel.ClickType.NEXT_PAGE) {
                        return true;
                    }

                    if (cr.type == HudPanel.ClickType.SWITCH_SOURCE) {
                        useGtfs = !useGtfs;
                        dataSource = useGtfs ? gtfsSource : demoSource;
                        reloadData();

                        // reset selection
                        selectedRoute = null;
                        selectedBus = null;
                        selectedStop = null;
                        stopRoutes = null;
                        cameraTarget = null;
                        return true;
                    }

                    if (cr.type == HudPanel.ClickType.TOGGLE_SHOW_ONLY_SELECTED) {
                        showOnlySelected = !showOnlySelected;
                        return true;
                    }

                    if (cr.type == HudPanel.ClickType.RESET) {
                        selectedRoute = null;
                        selectedBus = null;
                        selectedStop = null;
                        stopRoutes = null;
                        cameraTarget = null;
                        followSelectedBus = false;
                        return true;
                    }

                    if (cr.type == HudPanel.ClickType.SELECT_ROUTE && cr.route != null) {
                        BusRoute hudRoute = cr.route;

                        if (selectedRoute == hudRoute) {
                            selectedRoute = null;
                            cameraTarget = null;
                        } else {
                            selectedRoute = hudRoute;
                            cameraTarget = computeRouteCenter(hudRoute);
                        }

                        selectedBus = null;
                        selectedStop = null;
                        stopRoutes = null;
                        followSelectedBus = false;
                        return true;
                    }

                    if (hudPanel.isInsideHud(screenX, screenY, uiViewport)) {
                        return true;
                    }


                    // Map click
                    Vector3 v = new Vector3(screenX, screenY, 0);
                    viewport.unproject(v);

                    //pick bus first
                    Bus b = pickBus(v.x, v.y);
                    if (b != null) {
                        selectedBus = b;
                        selectedStop = null;
                        stopRoutes = null;

                        selectedRoute = b.getRoute();

                        //Follow bus
                        followSelectedBus = true;
                        cameraTarget = null;

                        return true;
                    }


                    //pick stop
                    selectedStop = pickStop(v.x, v.y);
                    if (selectedStop != null) {
                        selectedBus = null;
                        selectedRoute = null;
                        cameraTarget = null;
                        stopRoutes = computeRoutesForStop(selectedStop);
                        followSelectedBus = false;
                        return true;
                    }

                    //pick route
                    BusRoute r = pickRoute(v.x, v.y);
                    if (r != null) {
                        selectedRoute = r;
                        selectedBus = null;
                        selectedStop = null;
                        stopRoutes = null;

                        cameraTarget = computeRouteCenter(r);
                        return true;
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

    private void reloadData() {
        // clear selections
        selectedRoute = null;
        selectedBus = null;
        selectedStop = null;
        stopRoutes = null;
        cameraTarget = null;

        // load data
        stops = dataSource.loadStops();
        routes = dataSource.loadRoutes(stops);

        Gdx.app.log("DATA", "useGtfs=" + useGtfs
            + " stops=" + (stops == null ? -1 : stops.size)
            + " routes=" + (routes == null ? -1 : routes.size)
            + " buses=" + (buses == null ? -1 : buses.size));

        // rebuild cache + stop index
        routeStopIndex.clear();
        for (int i = 0; i < routes.size; i++) {
            BusRoute r = routes.get(i);
            r.rebuildWorld(MAP_ZOOM);
            routeStopIndex.put(r, new RouteStopIndex(r, r.stops, MAP_ZOOM));
        }

        //create buses (DEMO + GTFS)
        buses = new Array<>();
        for (int i = 0; i < routes.size; i++) {
            BusRoute r = routes.get(i);
            float total = r.getTotalLength();
            if (total <= 0f) continue;

            float baseSpeed = 35f + (i % 6) * 6f;
            buses.add(new Bus(r, baseSpeed, total * 0.12f));
            buses.add(new Bus(r, baseSpeed * 0.92f, total * 0.58f));
        }
    }


    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        uiTime += dt;

        //posodobi buse
        for (int i = 0; i < buses.size; i++) {
            buses.get(i).update(dt);
        }

        //STOP CHECK (so buses wait at stations)
        for (int i = 0; i < buses.size; i++) {
            Bus b = buses.get(i);
            RouteStopIndex idx = routeStopIndex.get(b.getRoute());
            b.checkStop(idx);
        }

        //premakni kamero
        Vector2 target = null;

        if (followSelectedBus && selectedBus != null && !dragging) {
            followTmp.set(selectedBus.getPosition());
            target = followTmp;
        } else if (cameraTarget != null) {
            target = cameraTarget;
        }

        if (target != null) {
            float lerp = 0.07f; // malo bolj "snappy" za follow
            camera.position.x += (target.x - camera.position.x) * lerp;
            camera.position.y += (target.y - camera.position.y) * lerp;
        }

        camera.update();

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        // visible bounds
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

        // ---- ROUTES ----
        drawRoutes();

        // ---- BUSES + STOPS + SELECTION + LABELS ----
        drawStopsAndBuses();

        // ---- HUD ----
        hudPanel.setShowOnlySelected(showOnlySelected);
        hudPanel.draw(
            uiViewport,
            uiCamera,
            shapeRenderer,
            batch,
            font,
            routes,
            selectedRoute,
            selectedBus,
            selectedStop,
            routeStopIndex,
            this::setRouteColor
        );
    }

    private Vector2 computeRouteCenter(BusRoute route) {
        Array<Vector2> pts = route.getWorldPoints();
        Vector2 c = new Vector2();
        if (pts == null || pts.size == 0) return c;

        for (int i = 0; i < pts.size; i++) {
            c.add(pts.get(i));
        }
        c.scl(1f / pts.size);
        return c;
    }

    private Array<BusRoute> computeRoutesForStop(Stop stop) {
        if (stop == null) return null;

        Array<BusRoute> res = new Array<>();
        for (int i = 0; i < routes.size; i++) {
            BusRoute r = routes.get(i);
            if (r == null || r.stops == null) continue;

            for (int j = 0; j < r.stops.size; j++) {
                Stop s = r.stops.get(j);
                if (s == stop) {
                    res.add(r);
                    break;
                }
                if (s != null && stop.id != null && stop.id.equals(s.id)) {
                    res.add(r);
                    break;
                }
            }
        }
        return res;
    }

    private void drawRoutes() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        float base = 3.0f * camera.zoom;
        float shadow = 7.0f * camera.zoom;
        float fadedMult = 0.65f;

        // shadow
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int r = 0; r < routes.size; r++) {

            BusRoute route = routes.get(r);

            // --- show only selected ---
            if (showOnlySelected && selectedRoute != null && route != selectedRoute) continue;

            // --- stop filter ---
            if (stopRoutes != null && stopRoutes.size > 0 && !stopRoutes.contains(route, true)) continue;

            boolean isSelected = (selectedRoute == null) || (route == selectedRoute);
            float shadowAlpha = isSelected ? 0.40f : 0.08f;

            shapeRenderer.setColor(0f, 0f, 0f, shadowAlpha);

            Array<Vector2> pts = route.getWorldPoints();
            float thick = shadow * (isSelected ? 1.0f : fadedMult);

            for (int i = 0; i < pts.size - 1; i++) {
                shapeRenderer.rectLine(pts.get(i), pts.get(i + 1), thick);
            }
        }
        shapeRenderer.end();

        // main colored
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int r = 0; r < routes.size; r++) {
            BusRoute route = routes.get(r);

            // --- show only selected ---
            if (showOnlySelected && selectedRoute != null && route != selectedRoute) continue;

            // --- stop filter (selected stop) ---
            if (stopRoutes != null && stopRoutes.size > 0 && !stopRoutes.contains(route, true)) continue;

            boolean isSelected = (selectedRoute == null) || (route == selectedRoute);
            float alpha = isSelected ? 0.95f : 0.12f;

            setRouteColor(shapeRenderer, r, alpha);

            Array<Vector2> pts = route.getWorldPoints();
            float thick = base * (isSelected ? 1.0f : fadedMult);

            for (int i = 0; i < pts.size - 1; i++) {
                shapeRenderer.rectLine(pts.get(i), pts.get(i + 1), thick);
            }
        }
        shapeRenderer.end();

        // highlight for selected route
        if (selectedRoute != null) {
            int selIndex = routes.indexOf(selectedRoute, true);
            Array<Vector2> pts = selectedRoute.getWorldPoints();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1f, 1f, 1f, 0.35f);
            float outlineThick = 11.0f * camera.zoom;
            for (int i = 0; i < pts.size - 1; i++) {
                shapeRenderer.rectLine(pts.get(i), pts.get(i + 1), outlineThick);
            }

            setRouteColor(shapeRenderer, selIndex, 1.0f);
            float selectedThick = 5.0f * camera.zoom;
            for (int i = 0; i < pts.size - 1; i++) {
                shapeRenderer.rectLine(pts.get(i), pts.get(i + 1), selectedThick);
            }
            shapeRenderer.end();
        }
    }

    private void drawStopsAndBuses() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        float stopR = STOP_RADIUS * camera.zoom;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // stops
        for (int i = 0; i < stops.size; i++) {
            Stop s = stops.get(i);
            float x = (float) GeoUtils.lonToWorldX(s.point.lon, MAP_ZOOM);
            float y = (float) GeoUtils.latToWorldY(s.point.lat, MAP_ZOOM);

            shapeRenderer.setColor(0f, 0f, 0f, 0.30f);
            shapeRenderer.circle(x, y, stopR + (2.5f * camera.zoom), 18);

            shapeRenderer.setColor(1f, 0.20f, 0.20f, 0.95f);
            shapeRenderer.circle(x, y, stopR, 18);
        }

        // buses
        for (int i = 0; i < buses.size; i++) {
            Bus b = buses.get(i);
            Vector2 p = b.getPosition();

            boolean routeSelected = (selectedRoute != null);
            boolean isOnSelectedRoute = (!routeSelected) || (b.getRoute() == selectedRoute);

            float busAlpha = isOnSelectedRoute ? 0.95f : 0.20f;
            float haloAlpha = isOnSelectedRoute ? 0.30f : 0.08f;

            shapeRenderer.setColor(0f, 0f, 0f, haloAlpha);
            shapeRenderer.circle(p.x, p.y, 9.0f * camera.zoom, 20);

            int routeIndex = routes.indexOf(b.getRoute(), true);

            if (b.isWaiting()) {
                // yellow tint when stopped
                shapeRenderer.setColor(1f, 0.85f, 0.25f, busAlpha);
            } else {
                setRouteColor(shapeRenderer, routeIndex, busAlpha);
            }

            shapeRenderer.circle(p.x, p.y, 6.5f * camera.zoom, 20);

            shapeRenderer.setColor(1f, 1f, 1f, busAlpha);
            shapeRenderer.circle(p.x, p.y, 2.0f * camera.zoom, 12);
        }

        shapeRenderer.end();

        // selected stop
        if (selectedStop != null) {
            float sx = (float) GeoUtils.lonToWorldX(selectedStop.point.lon, MAP_ZOOM);
            float sy = (float) GeoUtils.latToWorldY(selectedStop.point.lat, MAP_ZOOM);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 1f, 0.2f, 1f);
            shapeRenderer.circle(sx, sy, (STOP_RADIUS + 7f) * camera.zoom, 24);
            shapeRenderer.end();
        }

        // selected bus
        if (selectedBus != null) {
            Vector2 p = selectedBus.getPosition();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 1f, 0.2f, 1f);
            shapeRenderer.circle(p.x, p.y, 12.0f * camera.zoom, 24);
            shapeRenderer.end();
        }

        // next stop highlight
        if (selectedBus != null) {
            BusRoute r = selectedBus.getRoute();
            RouteStopIndex idx = routeStopIndex.get(r);
            if (idx != null) {
                Stop next = idx.getNextStop(selectedBus.getDistanceOnRoute());
                if (next != null) {
                    float sx = (float) GeoUtils.lonToWorldX(next.point.lon, MAP_ZOOM);
                    float sy = (float) GeoUtils.latToWorldY(next.point.lat, MAP_ZOOM);

                    float pulse = 0.5f + 0.5f * (float) Math.sin(uiTime * 4.0f);
                    float radius = (STOP_RADIUS + 10f + pulse * 6f) * camera.zoom;

                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                    shapeRenderer.setColor(1f, 1f, 0.2f, 0.85f);
                    shapeRenderer.circle(sx, sy, radius, 30);

                    shapeRenderer.setColor(1f, 1f, 1f, 0.55f);
                    shapeRenderer.circle(sx, sy, (STOP_RADIUS + 6f) * camera.zoom, 24);
                    shapeRenderer.end();
                }
            }
        }

        // labels
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

    // ---------- HUD CLICK HELPERS ----------
    // returns:
    // -2 = reset
    // -1 = toggle showOnlySelected
    //  >=0 = select route index
    //  else = none
    private int pickHudAction(int screenX, int screenY) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        float worldH = uiViewport.getWorldHeight();

        float hudLeft = HUD_PAD;
        float hudRight = HUD_PAD + HUD_BOX_W;
        float hudTop = worldH - HUD_PAD;
        float hudBottom = hudTop - HUD_BOX_H;

        if (v.x < hudLeft || v.x > hudRight || v.y < hudBottom || v.y > hudTop) {
            return Integer.MIN_VALUE;
        }

        float y = worldH - 14f;
        float toggleTop = y;
        float toggleBottom = y - HUD_LINE_H;
        float extra = 4f;
        if (v.y <= (toggleTop + extra) && v.y >= (toggleBottom - extra)) return -1;
        y -= HUD_LINE_H;

        float resetTop = y;
        float resetBottom = y - HUD_LINE_H;
        if (v.y <= (resetTop + extra) && v.y >= (resetBottom - extra)) return -2;
        y -= HUD_LINE_H;

        y -= (HUD_SECTION_GAP * 0.5f);
        y -= HUD_SECTION_GAP;
        y -= (HUD_LINE_H + 2f);

        for (int i = 0; i < routes.size; i++) {
            float rowTop = y;
            float rowBottom = y - HUD_LINE_H;
            if (v.y <= (rowTop + extra) && v.y >= (rowBottom - extra)) return i;
            y -= HUD_LINE_H;
        }

        return Integer.MIN_VALUE;
    }

    private boolean isPointInsideHud(int screenX, int screenY) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        float hudLeft = HUD_PAD;
        float hudRight = HUD_PAD + HUD_BOX_W;
        float hudTop = uiViewport.getWorldHeight() - HUD_PAD;
        float hudBottom = hudTop - HUD_BOX_H;

        return v.x >= hudLeft && v.x <= hudRight && v.y >= hudBottom && v.y <= hudTop;
    }

    // --- PICK / UTILS ----

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
            case 0: sr.setColor(0.15f, 0.60f, 0.95f, alpha); break;
            case 1: sr.setColor(0.20f, 0.80f, 0.55f, alpha); break;
            case 2: sr.setColor(0.95f, 0.55f, 0.20f, alpha); break;
            case 3: sr.setColor(0.70f, 0.35f, 0.95f, alpha); break;
            default: sr.setColor(0.95f, 0.25f, 0.45f, alpha); break;
        }
    }

    private BusRoute pickRoute(float worldX, float worldY) {
        BusRoute best = null;
        float bestDist2 = Float.MAX_VALUE;

        float tol = ROUTE_PICK_TOLERANCE * camera.zoom;
        float tol2 = tol * tol;

        for (int r = 0; r < routes.size; r++) {
            BusRoute route = routes.get(r);
            Array<Vector2> pts = route.getWorldPoints();
            if (pts == null || pts.size < 2) continue;

            for (int i = 0; i < pts.size - 1; i++) {
                Vector2 a = pts.get(i);
                Vector2 b = pts.get(i + 1);
                float d2 = pointToSegmentDist2(worldX, worldY, a.x, a.y, b.x, b.y);
                if (d2 < bestDist2) {
                    bestDist2 = d2;
                    best = route;
                }
            }
        }

        return (bestDist2 <= tol2) ? best : null;
    }

    private float pointToSegmentDist2(float px, float py,
                                      float ax, float ay,
                                      float bx, float by) {
        float abx = bx - ax;
        float aby = by - ay;
        float apx = px - ax;
        float apy = py - ay;

        float abLen2 = abx * abx + aby * aby;
        if (abLen2 <= 0.000001f) {
            float dx = px - ax;
            float dy = py - ay;
            return dx * dx + dy * dy;
        }

        float t = (apx * abx + apy * aby) / abLen2;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        float cx = ax + abx * t;
        float cy = ay + aby * t;

        float dx = px - cx;
        float dy = py - cy;
        return dx * dx + dy * dy;
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
