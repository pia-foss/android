/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.model.PIAServer;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.PI;

public class RegionMapView extends FrameLayout {

    private Coordinates locationMeta;

    private double markerRadius = 2.0;

    // Top and bottom latitudes of map graphic
    private double topLat = 83.65;
    private double bottomLat = -56.00;

    // Left and right longitudes of map graphic
    private double leftLong = -168.12;
    private double rightLong = -169.65;

    private Drawable locationShape;
    private float shapeRadius;

    public RegionMapView(Context context) {
        super(context);
        init(context);
    }

    public RegionMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RegionMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);

        locationShape = context.getResources().getDrawable(R.drawable.shape_region_point);
        shapeRadius = context.getResources().getDimension(R.dimen.region_location_radius);
    }

    public void setServer(PIAServer server) {
        locationMeta = new Coordinates(server);
        locationShape.invalidateSelf();
        invalidate();
    }

    private double getLocationX() {
        if(locationMeta == null) {
            return -1.0;
        }

        // Longitude is locationMeta.long -> range [-180, 180]
        double x = locationMeta.longitude;
        // Adjust for actual left edge of graphic -> range [-168, 192]
        if(x < leftLong) {
            x += 360.0;
        }
        // Map to [0, width]
        double mapWidth = getWidth();
        double a = x - leftLong;
        double b = rightLong - leftLong;
        return (a / (b + 360.0)) * mapWidth;
    }

    private double getLocationY() {
        if(locationMeta == null) {
            return -1.0;
        }

        // Project the latitude -> range [-2.3034..., 2.3034...]
        double millerLat = millerProjectLat(locationMeta.latitude);

        // Map to the actual range shown by the map.  (If this point is outside
        // the map bound, it returns a negative value or a value greater than
        // height.)
        // Map to unit range -> [0, 1], where 0 is the bottom and 1 is the top
        double unitY = (millerLat - bottomMiller()) / (topMiller() - bottomMiller());

        // Flip and scale to height
        return (1.0-unitY) * (double)(getHeight());
    }

    private long mapPointX() {
        return Math.round(getLocationX());
    }

    private long mapPointY() {
        return Math.round(getLocationY());
    }

    private boolean showLocation() {
        return mapPointX() >= 0 && mapPointX() < getWidth() &&
                mapPointY() >= 0 && mapPointY() < getHeight();
    }

    private double x() {
        return mapPointX() - markerRadius;
    }

    private double y() {
        return mapPointY() - markerRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        locationShape.setBounds((int)(getLocationX() - shapeRadius / 2),
                                (int)(getLocationY() - shapeRadius / 2),
                                (int)(getLocationX() + shapeRadius / 2),
                                (int)(getLocationY() + shapeRadius / 2));
        locationShape.draw(canvas);
        DLog.d("RegionMapView", "Updated map");
        DLog.d("RegionMapView", "X: " + getLocationX() + " Y: " + getLocationY());
        DLog.d("RegionMapView", "Bounds: " + locationShape.getBounds().toString());
    }

    // Top and bottom, Miller-projected
    private double topMiller() {
        return millerProjectLat(topLat);
    }
    private double bottomMiller() {
        return millerProjectLat(bottomLat);
    }

    private double degToRad(double degrees){
        return degrees * PI / 180.0;
    }

    // Project latitude.  The map uses this projection:
    // https://en.wikipedia.org/wiki/Miller_cylindrical_projection
    private double millerProjectLat(double latitudeDeg) {
        return 1.25 * log2(Math.tan(PI * 0.25 + 0.4 * degToRad(latitudeDeg)));
    }

    private double log2(Double val) {
        return Math.log(val) / Math.log(2);
    }

    class Coordinates {
        double latitude;
        double longitude;

        Coordinates(PIAServer server) {

            // Set the initial default values
            Pair coords = coordinates.get(server);
            latitude = coords != null ? (double)coords.first : (double)defaultCoordinate.first;
            longitude = coords != null ? (double)coords.second : (double)defaultCoordinate.second;

            if (server != null) {
                String sanitizedLatitude =
                        server.getLatitude() == null
                                ? defaultCoordinate.first.toString()
                                : server.getLatitude().replaceAll("\\s", "");
                String sanitizedLongitude =
                        server.getLongitude() == null
                                ? defaultCoordinate.second.toString()
                                : server.getLongitude().replaceAll("\\s", "");

                boolean validLatitude = !TextUtils.isEmpty(sanitizedLatitude);
                boolean validLongitude = !TextUtils.isEmpty(sanitizedLongitude);

                if (validLatitude && validLongitude) {
                    latitude = Double.parseDouble(sanitizedLatitude);
                    longitude = Double.parseDouble(sanitizedLongitude);
                }
            }
        }

        // Pair(lat, long)
        private Pair defaultCoordinate = new Pair(40.463667, -3.74922);
        private Map<String, Pair> coordinates = Stream.of(new Object[][] {
                {"UAE", new Pair(23.424076, 53.847818)},
                {"Albania", new Pair(41.33165, 19.8318)},
                {"Argentina", new Pair(-38.416096, -63.616673)},
                {"AU Sydney", new Pair(-33.868820, 151.209296)},
                {"AU Melbourne", new Pair(-37.813628, 144.963058)},
                {"AU Perth", new Pair(-31.950527, 115.860458)},
                {"Austria", new Pair(47.516231, 14.550072)},
                {"Bosnia and Herzegovina", new Pair(43.858181, 18.412340)},
                {"Belgium", new Pair(50.503887, 4.469936)},
                {"Bulgaria", new Pair(42.655033, 25.231817)},
                {"Brazil", new Pair(-14.235004, -51.92528)},
                {"CA Montreal", new Pair(45.501689, -73.567256)},
                {"CA Ontario", new Pair(51.253777, -85.232212)},
                {"CA Toronto", new Pair(43.653226, -79.383184)},
                {"CA Vancouver", new Pair(49.282729, -123.120738)},
                {"Czech Republic", new Pair(50.075538, 14.4378)},
                {"DE Berlin", new Pair(52.520007, 13.404954)},
                {"Denmark", new Pair(56.263920, 9.501785)},
                {"Estonia", new Pair(59.436962, 24.753574)},
                {"Finland", new Pair(61.924110, 25.748151)},
                {"France", new Pair(46.227638, 2.213749)},
                {"DE Frankfurt", new Pair(50.110922, 8.682127)},
                {"Greece", new Pair(37.983810, 23.727539)},
                {"Hong Kong", new Pair(22.396428, 114.109497)},
                {"Croatia", new Pair(45.815399, 15.966568)},
                {"Hungary", new Pair(47.162494, 19.503304)},
                {"India", new Pair(20.593684, 78.96288)},
                {"Ireland", new Pair(53.142367, -7.692054)},
                {"Iceland", new Pair(64.852829, -18.301501)},
                {"Israel", new Pair(31.046051, 34.851612)},
                {"Italy", new Pair(41.871940, 12.56738)},
                {"Japan", new Pair(36.204824, 138.252924)},
                {"Lithuania", new Pair(54.687157, 25.279652)},
                {"Luxembourg", new Pair(49.815273, 6.129583)},
                {"Latvia", new Pair(56.946285, 24.105078)},
                {"Moldova", new Pair(47.265819, 28.598334)},
                {"Mexico", new Pair(23.634501, -102.552784)},
                {"North Macedonia", new Pair(41.608635, 21.745275)},
                {"Malaysia", new Pair(3.140853, 101.693207)},
                {"Netherlands", new Pair(52.132633, 5.291266)},
                {"Norway", new Pair(60.472024, 8.468946)},
                {"New Zealand", new Pair(-40.900557, 174.885971)},
                {"Poland", new Pair(51.919438, 19.145136)},
                {"Portugal", new Pair(38.736946, -9.142685)},
                {"Romania", new Pair(45.943161, 24.96676)},
                {"Serbia", new Pair(44.016421, 21.005859)},
                {"Singapore", new Pair(1.352083, 103.819836)},
                {"Slovenia", new Pair(46.075219, 14.882733)},
                {"Slovakia", new Pair(48.148598, 17.107748)},
                {"Spain", new Pair(40.463667, -3.74922)},
                {"Sweden", new Pair(60.128161, 18.643501)},
                {"Switzerland", new Pair(46.818188, 8.227512)},
                {"Turkey", new Pair(38.963745, 35.243322)},
                {"Ukraine", new Pair(48.379433, 31.165581)},
                {"UK London", new Pair(51.507351, -0.127758)},
                {"UK Manchester", new Pair(53.480759, -2.242631)},
                {"UK Southampton", new Pair(50.909700, -1.404351)},
                {"US East", new Pair(36.414652, -77.739258)},
                {"US West", new Pair(40.607697, -120.805664)},
                {"US Atlanta", new Pair(33.748995, -84.387982)},
                {"US California", new Pair(36.778261, -119.417932)},
                {"US Chicago", new Pair(41.878114, -87.629798)},
                {"US Denver", new Pair(39.739236, -104.990251)},
                {"US Florida", new Pair(27.664827, -81.515754)},
                {"US Houston", new Pair(29.760427, -95.369803)},
                {"US Las Vegas", new Pair(36.169941, -115.13983)},
                {"US New York City", new Pair(40.712775, -74.005973)},
                {"US Seattle", new Pair(47.606209, -122.332071)},
                {"US Silicon Valley", new Pair(37.593392, -122.04383)},
                {"US Texas", new Pair(33.623962, -109.654814)},
                {"US Washington DC", new Pair(38.907192, -77.036871)},
                {"South Africa", new Pair(-30.559482, 22.937506)}
        }).collect(Collectors.toMap(data -> (String) data[0], data -> (Pair) data[1]));
    }
}
