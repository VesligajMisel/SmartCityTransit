package si.um.feri.vesligaj.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Array;

public class TileManager {

    private static final int TILE_SIZE = 256;

    private static final String TILE_URL =
        "https://basemaps.cartocdn.com/light_all/%d/%d/%d.png";
    private static final int MAX_TILES = 800;

    private static int maxIndex(int zoom) {
        return (1 << zoom) - 1;
    }

    private static int wrapX(int x, int zoom) {
        int n = 1 << zoom;
        int r = x % n;
        return (r < 0) ? (r + n) : r;
    }

    private static int clampY(int y, int zoom) {
        int max = maxIndex(zoom);
        if (y < 0) return 0;
        if (y > max) return max;
        return y;
    }

    private final ObjectMap<String, Texture> cache = new ObjectMap<>();
    private final ObjectMap<String, Boolean> loading = new ObjectMap<>();

    // simple access order for soft LRU eviction
    private final Array<String> accessOrder = new Array<>(false, 1024);

    private final Texture placeholder;

    public TileManager() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0.85f, 0.85f, 0.85f, 1f);
        pm.fill();
        placeholder = new Texture(pm);
        pm.dispose();
    }

    public Texture getTile(int x, int y, int zoom) {
        int xx = wrapX(x, zoom);
        int yy = clampY(y, zoom);

        String key = zoom + "_" + xx + "_" + yy;

        Texture ready = cache.get(key);
        if (ready != null) {
            touch(key);
            return ready;
        }

        if (!loading.containsKey(key)) {
            loading.put(key, true);

            String url = String.format(TILE_URL, zoom, xx, yy);

            HttpRequestBuilder builder = new HttpRequestBuilder();
            Net.HttpRequest req = builder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(url)
                .build();

            req.setHeader("User-Agent", "SmartCityTransit/1.0 (FERI project)");

            Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    int status = httpResponse.getStatus().getStatusCode();
                    if (status != HttpStatus.SC_OK) {
                        Gdx.app.log("TILES", "HTTP " + status + " url=" + url);
                        Gdx.app.postRunnable(() -> loading.remove(key));
                        return;
                    }

                    byte[] bytes = httpResponse.getResult();
                    Gdx.app.postRunnable(() -> {
                        try {
                            Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                            Texture tex = new Texture(pixmap);
                            pixmap.dispose();

                            cache.put(key, tex);
                            touch(key);
                            evictIfNeeded();

                        } catch (Exception e) {
                            Gdx.app.log("TILES", "Decode failed url=" + url + " err=" + e.getMessage());
                        } finally {
                            loading.remove(key);
                        }
                    });
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.postRunnable(() -> loading.remove(key));
                    Gdx.app.log("TILES", "FAILED url=" + url + " err=" + t.getMessage());
                }

                @Override
                public void cancelled() {
                    Gdx.app.postRunnable(() -> loading.remove(key));
                    Gdx.app.log("TILES", "CANCELLED url=" + url);
                }
            });
        }

        return placeholder;
    }

    public int getTileSize() {
        return TILE_SIZE;
    }

    public Texture getPlaceholder() {
        return placeholder;
    }

    private void touch(String key) {
        for (int i = accessOrder.size - 1; i >= 0; i--) {
            if (accessOrder.get(i).equals(key)) {
                accessOrder.removeIndex(i);
                break;
            }
        }
        accessOrder.add(key);
    }

    private void evictIfNeeded() {
        while (cache.size > MAX_TILES && accessOrder.size > 0) {
            String oldest = accessOrder.removeIndex(0);
            Texture t = cache.remove(oldest);
            if (t != null) {
                t.dispose();
            }
        }
    }

    public void dispose() {
        for (Texture t : cache.values()) t.dispose();
        cache.clear();
        accessOrder.clear();
        placeholder.dispose();
        loading.clear();
    }
}
