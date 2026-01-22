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

    // ---------------- Pagination ----------------
    private int routePage = 0;
    private static final int ROUTES_PER_PAGE = 10;

    private int maxRoutePages(int totalRoutes) {
        return Math.max(1, (int) Math.ceil(totalRoutes / (float) ROUTES_PER_PAGE));
    }

    // ---------------- Click result ----------------
    public enum ClickType {
        NONE,
        TOGGLE_SHOW_ONLY_SELECTED,
        RESET,
        SELECT_ROUTE,
        SWITCH_SOURCE,
        PREV_PAGE,
        NEXT_PAGE
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

        public static ClickResult prevPage() {
            ClickResult r = new ClickResult();
            r.type = ClickType.PREV_PAGE;
            return r;
        }

        public static ClickResult nextPage() {
            ClickResult r = new ClickResult();
            r.type = ClickType.NEXT_PAGE;
            return r;
        }

        public static ClickResult switchSource() {
            ClickResult r = new ClickResult();
            r.type = ClickType.SWITCH_SOURCE;
            return r;
        }
    }

    // ---------------- Layout ----------------
    private float HUD_PAD = 10f;
    private float HUD_BOX_W = 380f;
    private float HUD_TEXT_X_INNER = 12f;
    private float HUD_LINE_H = 22f;
    private float HUD_SECTION_GAP = 12f;
    private float HUD_ITEM_GAP = 6f;

    // ---------------- State ----------------
    private boolean showOnlySelected = false;

    public boolean isShowOnlySelected() { return showOnlySelected; }
    public void setShowOnlySelected(boolean v) { showOnlySelected = v; }

    // ---------------- Internal cached layout (for correct click hitboxes) ----------------
    private float lastHudX = 0f, lastHudY = 0f, lastHudW = 0f, lastHudH = 0f;

    private float lastToggleY = Float.NaN;
    private float lastResetY  = Float.NaN;
    private float lastDataY   = Float.NaN;
    private float lastNavY    = Float.NaN;
    private float lastRoutesStartY = Float.NaN;
    private int lastRoutesStartIdx = 0;
    private int lastRoutesEndIdx = 0;

    private float lastLineH = 22f;
    private float lastRowExtra = 6f; // click padding

    // ---------------- Formatting ----------------
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

        // NOTE: height calc is "ok enough"; click will use cached positions from actual draw below
        int linesControls = 3; // toggle + reset + data switch
        int linesHeader = 1;   // "LINES"
        int linesNav = 1;      // "< Prev ... Next >"
        int linesRoutes = Math.min(ROUTES_PER_PAGE, routes.size);
        int linesInfoHeader = hasInfo ? 1 : 0;

        int linesBus = 0;
        if (selectedBus != null) {
            linesBus += 1; // title
            linesBus += 1; // line
            linesBus += 1; // speed
            linesBus += 1; // next stop
            // eta (optional)
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
            linesBus += 1; // gap
        }

        int linesStop = 0;
        if (selectedStop != null) {
            linesStop += 1; // title
            linesStop += 1; // name
            linesStop += 1; // lat
            linesStop += 1; // lon
        }

        float h = 0f;
        h += topMargin;
        h += innerPadY;

        h += linesControls * lineH;

        h += (sectionGap * 0.5f);
        h += 1.5f;
        h += dividerGap;

        h += linesHeader * (lineH + 2f);
        h += linesNav * (lineH + 6f);
        h += linesRoutes * lineH;

        h += (sectionGap * 0.5f);
        h += 1.5f;
        h += dividerGap;

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

        // cache HUD rect for click handling
        lastHudX = hudX;
        lastHudY = hudY;
        lastHudW = hudW;
        lastHudH = hudH;
        lastLineH = lineH;

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

        // ---------- TEXT ----------
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        float y = hudY + hudH - topMargin;

        // Controls
        String chk = showOnlySelected ? "[x] " : "[ ] ";
        font.setColor(0.92f, 0.92f, 0.92f, 0.95f);
        lastToggleY = y;
        font.draw(batch, chk + "Show only selected line", x, y);
        y -= lineH;

        font.setColor(0.98f, 0.75f, 0.75f, 0.95f);
        lastResetY = y;
        font.draw(batch, "Reset selection", x, y);
        y -= lineH;

        font.setColor(0.85f, 0.85f, 1f, 0.95f);
        lastDataY = y;
        font.draw(batch, "Data: DEMO / GTFS (click)", x, y);
        y -= lineH;

        y -= (sectionGap * 0.5f);
        float divider1Y = y;
        y -= dividerGap;

        // Lines
        font.setColor(1f, 1f, 1f, 0.95f);
        font.draw(batch, "LINES", x, y);
        y -= (lineH + 2f);

        // Navigation row
        font.setColor(0.85f, 0.85f, 0.85f, 0.9f);
        lastNavY = y;
        font.draw(batch,
            "< Prev    Page " + (routePage + 1) + "/" + maxRoutePages(routes.size) + "            Next >",
            x, y
        );
        y -= (lineH + 6f);

        int start = routePage * ROUTES_PER_PAGE;
        int end = Math.min(start + ROUTES_PER_PAGE, routes.size);

        lastRoutesStartIdx = start;
        lastRoutesEndIdx = end;
        lastRoutesStartY = y;

        for (int i = start; i < end; i++) {
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

        // dividers
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, 0.06f);
        shapeRenderer.rectLine(dividerX1, divider1Y, dividerX2, divider1Y, 1.2f);
        shapeRenderer.rectLine(dividerX1, divider2Y, dividerX2, divider2Y, 1.2f);
        shapeRenderer.end();
    }

    // Row hit test: we treat each clickable row as a band of height lineH under its baseline.
    // This matches how you're decrementing y in draw().
    private boolean hitRow(float vy, float baselineY, float lineH, float extra) {
        float top = baselineY + extra;
        float bottom = baselineY - lineH - extra;
        return vy <= top && vy >= bottom;
    }

    public ClickResult handleClick(int screenX, int screenY, Viewport uiViewport, Array<BusRoute> routes) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        // Use last drawn HUD rect (this fixes your "click is lower than text" problem)
        float hudLeft = lastHudX;
        float hudRight = lastHudX + lastHudW;
        float hudBottom = lastHudY;
        float hudTop = lastHudY + lastHudH;

        // Fallback (if draw() hasn't run yet)
        if (lastHudW <= 0f || lastHudH <= 0f) {
            float worldH = uiViewport.getWorldHeight();
            hudLeft = HUD_PAD;
            hudRight = HUD_PAD + HUD_BOX_W;
            hudTop = worldH - HUD_PAD;
            hudBottom = worldH - 600f;
        }

        if (v.x < hudLeft || v.x > hudRight || v.y < hudBottom || v.y > hudTop) {
            return ClickResult.none();
        }

        float lineH = lastLineH > 0f ? lastLineH : HUD_LINE_H;
        float extra = lastRowExtra;

        // 1) Toggle
        if (!Float.isNaN(lastToggleY) && hitRow(v.y, lastToggleY, lineH, extra)) {
            showOnlySelected = !showOnlySelected;
            return ClickResult.toggle();
        }

        // 2) Reset
        if (!Float.isNaN(lastResetY) && hitRow(v.y, lastResetY, lineH, extra)) {
            return ClickResult.reset();
        }

        // 3) Switch source
        if (!Float.isNaN(lastDataY) && hitRow(v.y, lastDataY, lineH, extra)) {
            return ClickResult.switchSource();
        }

        // 4) Nav row: "< Prev ... Next >"
        if (!Float.isNaN(lastNavY) && hitRow(v.y, lastNavY, lineH, extra)) {
            // Make these hit areas bigger than the text; this fixes "NEXT needs clicking more right"
            float prevLeft = hudLeft + 10f;
            float prevRight = prevLeft + 110f;

            float nextRight = hudRight - 10f;
            float nextLeft = nextRight - 110f;

            if (v.x >= prevLeft && v.x <= prevRight) {
                routePage = Math.max(0, routePage - 1);
                return ClickResult.prevPage();
            }
            if (v.x >= nextLeft && v.x <= nextRight) {
                routePage = Math.min(maxRoutePages(routes.size) - 1, routePage + 1);
                return ClickResult.nextPage();
            }
            // click on nav row but not on buttons -> eat it
            return ClickResult.none();
        }

        // 5) Route rows (paged)
        int start = Math.max(0, Math.min(routePage * ROUTES_PER_PAGE, Math.max(0, routes.size)));
        int end = Math.min(start + ROUTES_PER_PAGE, routes.size);

        // Prefer cached values from draw (most accurate)
        if (!Float.isNaN(lastRoutesStartY)) {
            start = lastRoutesStartIdx;
            end = lastRoutesEndIdx;

            // Guard if route count changed after draw
            start = Math.max(0, Math.min(start, routes.size));
            end = Math.max(start, Math.min(end, routes.size));

            float y = lastRoutesStartY;
            for (int i = start; i < end; i++) {
                if (hitRow(v.y, y, lineH, extra)) {
                    return ClickResult.select(routes.get(i));
                }
                y -= lineH;
            }
            return ClickResult.none();
        }

        // Fallback (should rarely happen)
        float y = hudTop - 14f - (3f * lineH) - (HUD_SECTION_GAP * 0.5f) - HUD_SECTION_GAP - (lineH + 2f) - (lineH + 6f);
        for (int i = start; i < end; i++) {
            if (hitRow(v.y, y, lineH, extra)) {
                return ClickResult.select(routes.get(i));
            }
            y -= lineH;
        }

        return ClickResult.none();
    }

    public boolean isInsideHud(int screenX, int screenY, Viewport uiViewport) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        float hudLeft = lastHudX;
        float hudRight = lastHudX + lastHudW;
        float hudBottom = lastHudY;
        float hudTop = lastHudY + lastHudH;

        // Fallback
        if (lastHudW <= 0f || lastHudH <= 0f) {
            float worldH = uiViewport.getWorldHeight();
            hudLeft = HUD_PAD;
            hudRight = HUD_PAD + HUD_BOX_W;
            hudTop = worldH - HUD_PAD;
            hudBottom = worldH - 600f;
        }

        return v.x >= hudLeft && v.x <= hudRight && v.y >= hudBottom && v.y <= hudTop;
    }
}
