package si.um.feri.vesligaj;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.vesligaj.map.GeoUtils;
import si.um.feri.vesligaj.map.TileManager;

/**
 * Glavni razred aplikacije.
 * Skrbi za prikaz zemljevida, upravljanje kamere (pan/zoom)
 * in izris mapnih tile-ov.
 */
public class SmartCityTransit extends ApplicationAdapter {

    // ---- Map settings ----
    private static final int TILE_SIZE = 256;
    private static final int MAP_ZOOM = 14;

    // Ljubljana (approx)
    private static final double START_LAT = 46.056946;
    private static final double START_LON = 14.505751;

    // ---- Rendering ----
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private TileManager tileManager;

    // Drag state
    private int lastScreenX, lastScreenY;
    private boolean dragging = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        tileManager = new TileManager();
        // Kamera in viewport za prikaz zemljevida.
        // Uporabljamo FitViewport, da se razmerje ohranja ob resize-u.
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        viewport.apply();

        // Začetna pozicija kamere je nastavljena na Ljubljano.
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

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                dragging = true;
                lastScreenX = screenX;
                lastScreenY = screenY;
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                dragging = false;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (!dragging) return false;

                int dx = screenX - lastScreenX;
                int dy = screenY - lastScreenY;

                // Premik kamere v world koordinatah glede na premik miške.
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

                camera.zoom = Math.max(0.25f, Math.min(camera.zoom, 6.0f));
                return true;
            }

            public boolean resize(int width, int height) {
                viewport.update(width, height, false);
                return true;
            }
        });
    }

    @Override
    public void render() {
        camera.update();

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

        batch.begin();

        for (int tx = minTileX - 1; tx <= maxTileX + 1; tx++) {
            for (int ty = minTileY - 1; ty <= maxTileY + 1; ty++) {

                // OSM uporablja izvor zgoraj-levo, libGDX pa spodaj-levo (Y-up).
                int osmX = GeoUtils.wrapTileX(tx, MAP_ZOOM);
                int osmY = GeoUtils.worldTileYToOsmTileY(ty, MAP_ZOOM);

                if (osmY < 0 || osmY >= (1 << MAP_ZOOM)) continue;

                float drawX = tx * TILE_SIZE;
                float drawY = ty * TILE_SIZE;

                batch.draw(tileManager.getTile(osmX, osmY, MAP_ZOOM), drawX, drawY, TILE_SIZE, TILE_SIZE);
            }
        }

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void dispose() {
        tileManager.dispose();
        batch.dispose();
    }
}
