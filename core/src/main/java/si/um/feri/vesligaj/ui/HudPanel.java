package si.um.feri.vesligaj.ui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.vesligaj.transit.BusRoute;

public class HudPanel {

    public enum ClickType {
        NONE,
        TOGGLE_SHOW_ONLY_SELECTED,
        RESET,
        SELECT_ROUTE
    }

    public static class ClickResult {
        public ClickType type = ClickType.NONE;
        public BusRoute route = null;

        public static ClickResult none() { return new ClickResult(); }
        public static ClickResult toggle() {
            ClickResult r = new ClickResult();
            r.type = ClickType.TOGGLE_SHOW_ONLY_SELECTED;
            return r;
        }
        public static ClickResult reset() {
            ClickResult r = new ClickResult();
            r.type = ClickType.RESET;
            return r;
        }
        public static ClickResult select(BusRoute route) {
            ClickResult r = new ClickResult();
            r.type = ClickType.SELECT_ROUTE;
            r.route = route;
            return r;
        }
    }

    // Layout
    private float pad = 10f;
    private float boxW = 400f;
    private float boxH = 240f;

    private float textX = 18f;
    private float titleYInset = 18f;

    private float lineH = 16f;

    // Sections
    private float afterTitleGap = 20f;
    private float afterLinesLabelGap = 16f;

    // Interactive rows
    private float toggleRowExtraClick = 4f;
    private float routeRowExtraClick = 4f;

    // State
    private boolean showOnlySelected = false;

    public boolean isShowOnlySelected() {
        return showOnlySelected;
    }

    public void setShowOnlySelected(boolean showOnlySelected) {
        this.showOnlySelected = showOnlySelected;
    }
    
    public void draw(Viewport uiViewport,
                     OrthographicCamera uiCamera,
                     ShapeRenderer shapeRenderer,
                     SpriteBatch batch,
                     BitmapFont font,
                     Array<BusRoute> routes,
                     BusRoute selectedRoute) {

        uiViewport.apply();
        uiCamera.update();

        float worldH = uiViewport.getWorldHeight();

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
        shapeRenderer.rect(pad, worldH - boxH - pad, boxW, boxH);
        shapeRenderer.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        float x = textX;
        float y = worldH - titleYInset;

        font.setColor(1f, 1f, 1f, 0.95f);
        font.draw(batch, "SmartCityTransit", x, y);
        y -= afterTitleGap;

        String chk = showOnlySelected ? "[x] " : "[ ] ";
        font.setColor(0.92f, 0.92f, 0.92f, 0.95f);
        font.draw(batch, chk + "Show only selected line", x, y);
        y -= lineH;

        font.setColor(0.90f, 0.90f, 0.90f, 0.90f);
        font.draw(batch, "Reset selection", x, y);
        y -= (lineH + 6f);

        font.setColor(1f, 1f, 1f, 0.95f);
        font.draw(batch, "Lines:", x, y);
        y -= afterLinesLabelGap;

        for (int i = 0; i < routes.size; i++) {
            BusRoute r = routes.get(i);
            boolean sel = (r == selectedRoute);
            String marker = sel ? "> " : "  ";

            if (sel) font.setColor(1f, 1f, 1f, 0.98f);
            else font.setColor(0.90f, 0.90f, 0.90f, 0.88f);

            font.draw(batch, marker + r.id + " - " + r.name, x, y);
            y -= lineH;
        }

        batch.end();
    }

    public ClickResult handleClick(int screenX, int screenY,
                                   Viewport uiViewport,
                                   Array<BusRoute> routes) {

        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        float worldH = uiViewport.getWorldHeight();

        float hudLeft = pad;
        float hudRight = pad + boxW;
        float hudTop = worldH - pad;
        float hudBottom = hudTop - boxH;

        if (v.x < hudLeft || v.x > hudRight || v.y < hudBottom || v.y > hudTop) {
            return ClickResult.none();
        }

        float y = worldH - titleYInset;
        y -= afterTitleGap;

        float toggleTop = y;
        float toggleBottom = y - lineH;
        if (v.y <= (toggleTop + toggleRowExtraClick) && v.y >= (toggleBottom - toggleRowExtraClick)) {
            showOnlySelected = !showOnlySelected;
            return ClickResult.toggle();
        }
        y -= lineH;

        float resetTop = y;
        float resetBottom = y - lineH;
        if (v.y <= (resetTop + toggleRowExtraClick) && v.y >= (resetBottom - toggleRowExtraClick)) {
            return ClickResult.reset();
        }
        y -= (lineH + 6f);

        y -= afterLinesLabelGap;

        for (int i = 0; i < routes.size; i++) {
            float rowTop = y;
            float rowBottom = y - lineH;

            if (v.y <= (rowTop + routeRowExtraClick) && v.y >= (rowBottom - routeRowExtraClick)) {
                return ClickResult.select(routes.get(i));
            }

            y -= lineH;
        }

        return new ClickResult();
    }

    public boolean isInsideHud(int screenX, int screenY, Viewport uiViewport) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        float worldH = uiViewport.getWorldHeight();

        float hudLeft = pad;
        float hudRight = pad + boxW;
        float hudTop = worldH - pad;
        float hudBottom = hudTop - boxH;

        return v.x >= hudLeft && v.x <= hudRight && v.y >= hudBottom && v.y <= hudTop;
    }
}
