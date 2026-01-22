package si.um.feri.vesligaj.ui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import si.um.feri.vesligaj.transit.Bus;
import si.um.feri.vesligaj.transit.BusRoute;
import si.um.feri.vesligaj.transit.RouteStopIndex;
import si.um.feri.vesligaj.transit.Stop;

public class HudPanel {

    private int routePage = 0;
    private static final int ROUTES_PER_PAGE = 10;

    private float lastHudX, lastHudY, lastHudW, lastHudH;

    private boolean editMode = false;

    // add-stop flow state
    private boolean addingStop = false;
    private boolean nameFieldActive = false;
    private String stopNameBuffer = "";

    private int maxRoutePages(int totalRoutes) {
        return Math.max(1, (int) Math.ceil(totalRoutes / (float) ROUTES_PER_PAGE));
    }

    public enum ClickType {
        NONE,
        TOGGLE_SHOW_ONLY_SELECTED,
        RESET,
        SELECT_ROUTE,
        SWITCH_SOURCE,
        PREV_PAGE,
        NEXT_PAGE,
        TOGGLE_EDIT_MODE,
        EDIT_PICK_LOCATION,
        EDIT_ADD_STOP,
        EDIT_HIDE_SELECTED_STOP,
        EDIT_ADD_BUS_START,
        EDIT_ADD_BUS_END,
        EDIT_CREATE_BUS,
        EDIT_NAME_FIELD,
        ADD_STOP,
        TRAVEL_TO
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

        public static ClickResult travelTo() {
            ClickResult r = new ClickResult();
            r.type = ClickType.TRAVEL_TO;
            return r;
        }

        public static ClickResult hideSelectedStop() {
            ClickResult r = new ClickResult();
            r.type = ClickType.EDIT_HIDE_SELECTED_STOP;
            return r;
        }

        public static ClickResult addStop() {
            ClickResult r = new ClickResult();
            r.type = ClickType.ADD_STOP;
            return r;
        }

        public static ClickResult editName() {
            ClickResult r = new ClickResult();
            r.type = ClickType.EDIT_NAME_FIELD;
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

        public static ClickResult toggleEdit() {
            ClickResult r = new ClickResult();
            r.type = ClickType.TOGGLE_EDIT_MODE;
            return r;
        }

        public static ClickResult switchSource() {
            ClickResult r = new ClickResult();
            r.type = ClickType.SWITCH_SOURCE;
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

        public static ClickResult createBus() {
            ClickResult r = new ClickResult();
            r.type = ClickType.EDIT_CREATE_BUS;
            return r;
        }
    }

    private float HUD_PAD = 10f;
    private float HUD_BOX_W = 380f;
    private float HUD_TEXT_X_INNER = 12f;
    private float HUD_LINE_H = 22f;
    private float HUD_SECTION_GAP = 12f;
    private float HUD_ITEM_GAP = 6f;

    private boolean showOnlySelected = false;

    private float travelBtnX, travelBtnY, travelBtnW, travelBtnH;
    private boolean travelBtnVisible = false;

    public boolean isTravelBtnVisible() { return travelBtnVisible; }

    public boolean isShowOnlySelected() { return showOnlySelected; }
    public void setShowOnlySelected(boolean v) { showOnlySelected = v; }

    public boolean isEditMode() { return editMode; }
    public void setEditMode(boolean v) {
        editMode = v;
        if (!editMode) {
            addingStop = false;
            nameFieldActive = false;
            stopNameBuffer = "";
        }
    }

    public boolean isAddingStop() { return addingStop; }
    public boolean isNameFieldActive() { return nameFieldActive; }
    public void setNameFieldActive(boolean v) { nameFieldActive = v; }

    public String getStopNameBuffer() { return stopNameBuffer; }
    public void setStopNameBuffer(String s) { stopNameBuffer = (s == null) ? "" : s; }

    public void beginAddStop() {
        addingStop = true;
        nameFieldActive = true;
        stopNameBuffer = "";
    }

    public void cancelAddStop() {
        addingStop = false;
        nameFieldActive = false;
        stopNameBuffer = "";
    }

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
        ObjectMap<BusRoute, RouteStopIndex> routeStopIndex,
        RouteColorSetter routeColorSetter
    ) {
        uiViewport.apply();
        uiCamera.update();
        travelBtnVisible = false;

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

        int linesControls = 3;
        int linesHeader = 1;
        int linesNav = 1;
        int linesRoutes = Math.min(ROUTES_PER_PAGE, routes.size);

        int linesEdit = 0;
        if (editMode) {
            linesEdit = addingStop ? 5 : 4;
        }

        int linesInfoHeader = hasInfo ? 1 : 0;

        int linesBus = 0;
        if (selectedBus != null) {
            linesBus += 1;
            linesBus += 1;
            linesBus += 1;
            linesBus += 1;

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
            linesStop += 1;
            linesStop += 1;
            linesStop += 3;
        }

        float h = 0f;
        h += topMargin;
        h += innerPadY;
        h += linesControls * lineH;

        h += dividerGap;
        h += 1.5f;
        h += sectionGap;

        h += linesHeader * (lineH + 2f);
        h += linesNav * lineH;
        h += 6f;
        h += linesRoutes * lineH;

        if (editMode) {
            h += sectionGap;
            h += linesEdit * lineH;
        }

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

        lastHudX = hudX;
        lastHudY = hudY;
        lastHudW = hudW;
        lastHudH = hudH;

        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.25f);
        shapeRenderer.rect(hudX + 4f, hudY - 4f, hudW, hudH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.07f, 0.07f, 0.08f, 0.78f);
        shapeRenderer.rect(hudX, hudY, hudW, hudH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, 0.05f);
        shapeRenderer.rect(hudX, hudY + hudH - 30f, hudW, 30f);
        shapeRenderer.end();

        float dividerX1 = hudX + 10f;
        float dividerX2 = hudX + hudW - 10f;

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        float y = hudY + hudH - topMargin;
        y -= innerPadY;

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

        font.setColor(1f, 1f, 1f, 0.95f);
        font.draw(batch, "LINES", x, y);
        y -= (lineH + 2f);

        font.setColor(0.85f, 0.85f, 0.85f, 0.9f);
        font.draw(batch,
            "< Prev    Page " + (routePage + 1) + "/" + maxRoutePages(routes.size) + "    Next >",
            x, y
        );
        y -= (lineH + 6f);

        int start = routePage * ROUTES_PER_PAGE;
        int end = Math.min(start + ROUTES_PER_PAGE, routes.size);

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

        if (editMode) {
            y -= sectionGap;

            font.setColor(1f, 1f, 1f, 0.95f);
            font.draw(batch, "EDIT", x, y);
            y -= (lineH + 2f);

            if (!addingStop) {
                font.setColor(0.80f, 1f, 0.80f, 0.95f);
                font.draw(batch, "+ Add stop", x, y);
                y -= lineH;

                font.setColor(1f, 0.65f, 0.65f, 0.95f);
                font.draw(batch, "-- Hide selected stop", x, y);
                y -= lineH;

                font.setColor(0.80f, 0.90f, 1f, 0.95f);
                font.draw(batch, "+ Add bus (click)", x, y);
                y -= lineH;

            } else {
                font.setColor(1f, 1f, 1f, 0.95f);
                String name = (stopNameBuffer == null || stopNameBuffer.isEmpty()) ? "_" : stopNameBuffer;
                font.draw(batch, "Name: " + name, x, y);
                y -= lineH;

                font.setColor(0.80f, 1f, 0.80f, 0.95f);
                font.draw(batch, "Click map to place stop", x, y);
                y -= lineH;

                font.setColor(0.95f, 0.75f, 0.75f, 0.90f);
                font.draw(batch, "Cancel (click here)", x, y);
                y -= lineH;

                font.setColor(0.85f, 0.85f, 0.85f, 0.90f);
                font.draw(batch, "Type name on keyboard", x, y);
                y -= lineH;
            }
        }

        y -= (sectionGap * 0.5f);
        float divider2Y = y;
        y -= dividerGap;

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

            font.setColor(0.80f, 0.90f, 1f, 0.95f);
            font.draw(batch, "Potuj do... (click)", x, y);
            travelBtnVisible = true;
            travelBtnX = x;
            travelBtnY = y - lineH + 6f;
            travelBtnW = hudW - (innerPadX * 2f);
            travelBtnH = lineH + 6f;

            y -= (lineH + itemGap);
        }

        batch.end();

        float btnH = 28f;
        float btnPad = 10f;
        float btnW = 170f;
        float btnX = hudX + btnPad;
        float btnY = hudY + btnPad;

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, editMode ? 0.14f : 0.08f);
        shapeRenderer.rect(btnX, btnY, btnW, btnH);
        shapeRenderer.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.setColor(1f, 1f, 1f, 0.95f);
        font.draw(batch, "Mode: " + (editMode ? "EDIT" : "VIEW"), btnX + 10f, btnY + 20f);
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

        float hudLeft = lastHudX;
        float hudRight = lastHudX + lastHudW;
        float hudBottom = lastHudY;
        float hudTop = lastHudY + lastHudH;

        if (v.x < hudLeft || v.x > hudRight || v.y < hudBottom || v.y > hudTop) {
            return ClickResult.none();
        }

        if (travelBtnVisible) {
            if (v.x >= travelBtnX && v.x <= travelBtnX + travelBtnW && v.y >= travelBtnY && v.y <= travelBtnY + travelBtnH) {
                return ClickResult.travelTo();
            }
        }

        float btnH = 28f;
        float btnPad = 10f;
        float btnW = 170f;
        float btnX = lastHudX + btnPad;
        float btnY = lastHudY + btnPad;

        if (v.x >= btnX && v.x <= btnX + btnW && v.y >= btnY && v.y <= btnY + btnH) {
            editMode = !editMode;
            if (!editMode) {
                addingStop = false;
                nameFieldActive = false;
                stopNameBuffer = "";
            }
            return ClickResult.toggleEdit();
        }

        final float topMargin = 14f;
        final float innerPadY = 12f;
        final float lineH = HUD_LINE_H;
        final float sectionGap = HUD_SECTION_GAP;
        final float dividerGap = HUD_SECTION_GAP;

        float y = hudTop - topMargin;
        y -= innerPadY;

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
        if (v.y <= dataTop + 4f && v.y >= dataBottom - 4f) {
            return ClickResult.switchSource();
        }
        y -= lineH;

        y -= (sectionGap * 0.5f);
        y -= dividerGap;

        y -= (lineH + 2f);

        float navTop = y;
        float navBottom = y - lineH;

        float innerPadX = HUD_TEXT_X_INNER;

        float prevLeft = lastHudX + innerPadX;
        float prevRight = prevLeft + 110f;

        float nextRight = lastHudX + lastHudW - innerPadX;
        float nextLeft = nextRight - 110f;

        if (v.y <= navTop + 4f && v.y >= navBottom - 4f) {
            if (v.x >= prevLeft && v.x <= prevRight) {
                routePage = Math.max(0, routePage - 1);
                return ClickResult.prevPage();
            }
            if (v.x >= nextLeft && v.x <= nextRight) {
                routePage = Math.min(maxRoutePages(routes.size) - 1, routePage + 1);
                return ClickResult.nextPage();
            }
        }

        y -= (lineH + 6f);

        int start = routePage * ROUTES_PER_PAGE;
        int end = Math.min(start + ROUTES_PER_PAGE, routes.size);

        for (int i = start; i < end; i++) {
            float rowTop = y;
            float rowBottom = y - lineH;

            if (v.y <= rowTop + 4f && v.y >= rowBottom - 4f) {
                return ClickResult.select(routes.get(i));
            }
            y -= lineH;
        }

        if (editMode) {
            y -= sectionGap;

            float editHeaderTop = y;
            float editHeaderBottom = y - lineH;
            if (v.y <= editHeaderTop + 4f && v.y >= editHeaderBottom - 4f) {
                return ClickResult.none();
            }
            y -= (lineH + 2f);

            if (!addingStop) {
                float addTop = y;
                float addBottom = y - lineH;
                if (v.y <= addTop + 4f && v.y >= addBottom - 4f) {
                    beginAddStop();
                    return ClickResult.addStop();
                }
                y -= lineH;

                float hideTop = y;
                float hideBottom = y - lineH;
                if (v.y <= hideTop + 4f && v.y >= hideBottom - 4f) {
                    return ClickResult.hideSelectedStop();
                }
                y -= lineH;

                float busTop = y;
                float busBottom = y - lineH;
                if (v.y <= busTop + 4f && v.y >= busBottom - 4f) {
                    return ClickResult.createBus();
                }
                y -= lineH;

            } else {
                float nameTop = y;
                float nameBottom = y - lineH;
                if (v.y <= nameTop + 4f && v.y >= nameBottom - 4f) {
                    nameFieldActive = true;
                    return ClickResult.editName();
                }
                y -= lineH;

                y -= lineH;

                float cancelTop = y;
                float cancelBottom = y - lineH;
                if (v.y <= cancelTop + 4f && v.y >= cancelBottom - 4f) {
                    cancelAddStop();
                    return ClickResult.none();
                }
                y -= lineH;

                y -= lineH;
            }
        }

        return ClickResult.none();
    }
    public boolean isInsideHud(int screenX, int screenY, Viewport uiViewport) {
        Vector3 v = new Vector3(screenX, screenY, 0);
        uiViewport.unproject(v);

        return v.x >= lastHudX && v.x <= lastHudX + lastHudW
            && v.y >= lastHudY && v.y <= lastHudY + lastHudH;
    }
}
