package si.um.feri.vesligaj.ui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.vesligaj.transit.Bus;
import si.um.feri.vesligaj.transit.BusRoute;
import si.um.feri.vesligaj.transit.RouteStopIndex;
import si.um.feri.vesligaj.transit.Stop;

public class HudPanel {

    public enum ClickType {
        NONE,
        TOGGLE_SHOW_ONLY_SELECTED,
        RESET,
        SELECT_ROUTE,
        SWITCH_SOURCE
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

    // ---- Layout---
    private float HUD_PAD = 10f;
    private float HUD_BOX_W = 380f;
    private float HUD_TEXT_X_INNER = 12f;
    private float HUD_LINE_H = 22f;
    private float HUD_SECTION_GAP = 12f;
    private float HUD_ITEM_GAP = 6f;

    // State
    private boolean showOnlySelected = false;

    public boolean isShowOnlySelected() { return showOnlySelected; }
    public void setShowOnlySelected(boolean v) { showOnlySelected = v; }

    // formatiranje
    private String format1(float v) {
        return String.format(java.util.Locale.US, "%.1f", v);
    }
    private String format5(double v) {
        return String.format(java.util.Locale.US, "%.5f", v);
    }
    public interface RouteColorSetter {
        void setColor(ShapeRenderer sr, int routeIndex, float alpha);
    }

    public void draw(
        Viewport uiViewport,
        OrthographicCamera uiCamera,
        ShapeRenderer shapeRenderer,
        SpriteBatch batch,
        BitmapFont font,
        Array<BusRoute> routes,
        BusRoute selectedRoute,
        Bus selectedBus,
        Stop selectedStop,
        com.badlogic.gdx.utils.ObjectMap<BusRoute, RouteStopIndex> routeStopIndex,
        RouteColorSetter routeColorSetter
    ) {
        uiViewport.apply();
        uiCamera.update();

        final float pad = HUD_PAD;
        final float hudX = pad;
        final float hudW = HUD_BOX_W;

        final float topMargin = 14f;
        final float innerPadX = HUD_TEXT_X_INNER;
        final float innerPadY = 12f;

        final float x = hudX + innerPadX;
        final float lineH = HUD_LINE_H;
        final float sectionGap = HUD_SECTION_GAP;
        final float itemGap = HUD_ITEM_GAP;
        final float dividerGap = HUD_SECTION_GAP;

        boolean hasInfo = (selectedBus != null) || (selectedStop != null);

        int linesControls = 2; // checkbox + reset
        int linesHeader = 1;   // "LINES"
        int linesRoutes = routes.size;
        int linesInfoHeader = hasInfo ? 1 : 0;

        int linesBus = 0;
        if (selectedBus != null) {
            linesBus += 1; // "Selected BUS"
            linesBus += 1; // Line
            linesBus += 1; // Speed
            linesBus += 1; // Next stop

            RouteStopIndex idxTmp = routeStopIndex.get(selectedBus.getRoute());
            float etaSecTmp = -1f;
            if (idxTmp != null) {
                float nextDist = idxTmp.getNextStopDistance(selectedBus.getDistanceOnRoute());
                float busDist = selectedBus.getDistanceOnRoute();
                float remain = nextDist - busDist;
                float total = selectedBus.getRoute().getTotalLength();
                if (remain < 0f && total > 0f) remain += total;
                float spd = selectedBus.getSpeedPx();
                if (spd > 0.001f) etaSecTmp = remain / spd;
            }
            if (etaSecTmp >= 0f) linesBus += 1;
            if (selectedBus.isWaiting()) linesBus += 1;

            linesBus += 1;
        }

        int linesStop = 0;
        if (selectedStop != null) {
            linesStop += 1; // "Selected STOP"
            linesStop += 1; // Name
            linesStop += 1; // Lat/Lon
        }

        float h = 0f;
        h += topMargin;
        h += innerPadY;
        h += linesControls * lineH;
        h += dividerGap;
        h += 1.5f;
        h += sectionGap;

        h += linesHeader * (lineH + 2f);
        h += linesRoutes * lineH;
        h += dividerGap;
        h += 1.5f;
        h += sectionGap;

        if (hasInfo) {
            h += linesInfoHeader * (lineH + 2f);
            h += linesBus * lineH;
            h += linesStop * lineH;
        } else {
            h += 6f;
        }

        h += innerPadY;

        float hudH = Math.max(160f, Math.min(h, uiViewport.getWorldHeight() - 2f * pad));
        float hudY = uiViewport.getWorldHeight() - hudH - pad;

        // ---------- PANEL ----------
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        // shadow
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.25f);
        shapeRenderer.rect(hudX + 4f, hudY - 4f, hudW, hudH);
        shapeRenderer.end();

        // body
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.07f, 0.07f, 0.08f, 0.78f);
        shapeRenderer.rect(hudX, hudY, hudW, hudH);
        shapeRenderer.end();

        // top band
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, 0.05f);
        shapeRenderer.rect(hudX, hudY + hudH - 30f, hudW, 30f);
        shapeRenderer.end();

        float dividerX1 = hudX + 10f;
        float dividerX2 = hudX + hudW - 10f;

        // -- TEXT ---
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        float y = hudY + hudH - topMargin;

        // Controls
        String chk = showOnlySelected ? "[x] " : "[ ] ";
        font.setColor(0.92f, 0.92f, 0.92f, 0.95f);
        font.draw(batch, chk + "Show only selected line", x, y);
        y -= lineH;

        font.setColor(0.98f, 0.75f, 0.75f, 0.95f);
        font.draw(batch, "Reset selection", x, y);
        y -= lineH;

        font.setColor(0.85f, 0.85f, 1f, 0.95f);
        font.draw(batch, "Data: DEMO / GTFS (click)", x, y);
        y -= lineH;

        y -= (sectionGap * 0.5f);
        float divider1Y = y;
        y -= dividerGap;

        // Lines
        font.setColor(1f, 1f, 1f, 0.95f);
        font.draw(batch, "LINES", x, y);
        y -= (lineH + 2f);

        for (int i = 0; i < routes.size; i++) {
            BusRoute r = routes.get(i);
            boolean sel = (r == selectedRoute);

            if (sel) {
                batch.end();
                shapeRenderer.setProjectionMatrix(uiCamera.combined);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(1f, 1f, 1f, 0.06f);
                shapeRenderer.rect(hudX + 8f, y - lineH + 3f, hudW - 16f, lineH + 6f);
                shapeRenderer.end();
                batch.begin();
            }

            // color bullet
            batch.end();
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            routeColorSetter.setColor(shapeRenderer, i, sel ? 0.95f : 0.55f);
            shapeRenderer.circle(hudX + 16f, y - 6f, 4.5f, 18);
            shapeRenderer.end();
            batch.begin();

            font.setColor(sel ? 1f : 0.88f, sel ? 1f : 0.88f, sel ? 1f : 0.88f, sel ? 0.98f : 0.88f);
            String marker = sel ? "> " : "  ";
            font.draw(batch, marker + r.id + " - " + r.name, x + 8f, y);

            y -= lineH;
        }

        y -= (sectionGap * 0.5f);
        float divider2Y = y;
        y -= dividerGap;

        // Info
        if (hasInfo) {
            font.setColor(1f, 1f, 1f, 0.95f);
            font.draw(batch, "INFO", x, y);
            y -= (lineH + 2f);
        }

        if (selectedBus != null) {
            BusRoute r = selectedBus.getRoute();
            RouteStopIndex idx = routeStopIndex.get(r);
            Stop next = (idx != null) ? idx.getNextStop(selectedBus.getDistanceOnRoute()) : null;

            float etaSec = -1f;
            if (idx != null) {
                float nextDist = idx.getNextStopDistance(selectedBus.getDistanceOnRoute());
                float busDist = selectedBus.getDistanceOnRoute();
                float remain = nextDist - busDist;

                float total = r.getTotalLength();
                if (remain < 0f && total > 0f) remain += total;

                float spd = selectedBus.getSpeedPx();
                if (spd > 0.001f) etaSec = remain / spd;
            }

            int routeIndex = routes.indexOf(r, true);

            // small color chip
            batch.end();
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            routeColorSetter.setColor(shapeRenderer, routeIndex, 0.95f);
            shapeRenderer.rect(hudX + 10f, y - 11f, 4f, 12f);
            shapeRenderer.end();
            batch.begin();

            font.setColor(1f, 1f, 1f, 0.95f);
            font.draw(batch, "Selected BUS", x, y); y -= (lineH + itemGap);

            font.setColor(0.92f, 0.92f, 0.92f, 0.95f);
            font.draw(batch, "Line: " + r.id + " - " + r.name, x, y); y -= (lineH + itemGap);
            font.draw(batch, "Speed: " + format1(selectedBus.getSpeedPx()) + " px/s", x, y); y -= (lineH + itemGap);
            font.draw(batch, "Next stop: " + (next != null ? next.name : "-"), x, y); y -= (lineH + itemGap);

            if (etaSec >= 0f) {
                font.draw(batch, "ETA: " + format1(etaSec) + " s", x, y);
                y -= (lineH + itemGap);
            }

            if (selectedBus.isWaiting()) {
                font.draw(batch, "Status: STOPPED (" + format1(selectedBus.getWaitTimer()) + " s)", x, y);
                y -= (lineH + itemGap);
            }

            y -= sectionGap;
        }

        if (selectedStop != null) {
            font.setColor(1f, 1f, 1f, 0.95f);
            font.draw(batch, "Selected STOP", x, y); y -= (lineH + itemGap);

            font.setColor(0.92f, 0.92f, 0.92f, 0.95f);
            font.draw(batch, "Name: " + selectedStop.name, x, y); y -= (lineH + itemGap);

            font.draw(batch, "Lat: " + format5(selectedStop.point.lat), x, y);
            y -= (lineH + itemGap);

            font.draw(batch, "Lon: " + format5(selectedStop.point.lon), x, y);
            y -= (lineH + itemGap);
        }

        batch.end();

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, 0.06f);
        shapeRenderer.rectLine(dividerX1, divider1Y, dividerX2, divider1Y, 1.2f);
        shapeRenderer.rectLine(dividerX1, divider2Y, dividerX2, divider2Y, 1.2f);
        shapeRenderer.end();
    }

    public ClickResult handleClick(int screenX, int screenY, Viewport uiViewport, Array<BusRoute> routes) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        float worldH = uiViewport.getWorldHeight();

        float hudLeft = HUD_PAD;
        float hudRight = HUD_PAD + HUD_BOX_W;

        if (v.x < hudLeft || v.x > hudRight) return ClickResult.none();
        if (v.y < worldH - 600f) return ClickResult.none();

        float topMargin = 14f;
        float innerPadY = 12f;
        float lineH = HUD_LINE_H;
        float sectionGap = HUD_SECTION_GAP;
        float dividerGap = HUD_SECTION_GAP;

        float y = worldH - HUD_PAD - topMargin - innerPadY;

        float toggleTop = y;
        float toggleBottom = y - lineH;
        if (v.y <= toggleTop + 4f && v.y >= toggleBottom - 4f) {
            showOnlySelected = !showOnlySelected;
            return ClickResult.toggle();
        }
        y -= lineH;

        float resetTop = y;
        float resetBottom = y - lineH;
        if (v.y <= resetTop + 4f && v.y >= resetBottom - 4f) {
            return ClickResult.reset();
        }
        y -= lineH;

        float dataTop = y;
        float dataBottom = y - lineH;

        if (v.y <= dataTop && v.y >= dataBottom) {
            ClickResult r = new ClickResult();
            r.type = ClickType.SWITCH_SOURCE;
            return r;
        }
        y -= lineH;

        y -= (sectionGap * 0.5f);
        y -= dividerGap;

        y -= (lineH + 2f);

        for (int i = 0; i < routes.size; i++) {
            float rowTop = y;
            float rowBottom = y - lineH;
            if (v.y <= rowTop + 4f && v.y >= rowBottom - 4f) {
                return ClickResult.select(routes.get(i));
            }
            y -= lineH;
        }

        return ClickResult.none();
    }

    public boolean isInsideHud(int screenX, int screenY, Viewport uiViewport) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        float worldH = uiViewport.getWorldHeight();
        float hudLeft = HUD_PAD;
        float hudRight = HUD_PAD + HUD_BOX_W;

        return (v.x >= hudLeft && v.x <= hudRight && v.y >= worldH - 600f);
    }
}
