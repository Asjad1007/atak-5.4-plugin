package com.atakmap.android.feature4boundaryalert.plugin;

import android.util.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages polygon boundaries and performs point-in-polygon detection
 * using the ray casting algorithm.
 */
public class BoundaryManager {
    private static final String TAG = "BoundaryManager";
    
    private List<GeoPoint> polygonVertices;
    private boolean isPolygonComplete;
    private boolean wasInside;
    
    public BoundaryManager() {
        this.polygonVertices = new ArrayList<>();
        this.isPolygonComplete = false;
        this.wasInside = false;
    }
    
    /**
     * Add a vertex to the polygon
     */
    public void addVertex(GeoPoint point) {
        if (!isPolygonComplete) {
            polygonVertices.add(point);
            Log.d(TAG, "Added vertex: " + point.getLatitude() + ", " + point.getLongitude());
        }
    }
    
    /**
     * Complete the polygon (requires at least 3 vertices)
     */
    public boolean completePolygon() {
        if (polygonVertices.size() >= 3) {
            isPolygonComplete = true;
            Log.d(TAG, "Polygon completed with " + polygonVertices.size() + " vertices");
            return true;
        }
        Log.w(TAG, "Cannot complete polygon - need at least 3 vertices");
        return false;
    }
    
    /**
     * Clear the polygon
     */
    public void clearPolygon() {
        polygonVertices.clear();
        isPolygonComplete = false;
        wasInside = false;
        Log.d(TAG, "Polygon cleared");
    }
    
    /**
     * Check if a point is inside the polygon using ray casting algorithm
     * @param point The point to check
     * @return true if inside, false if outside
     */
    public boolean isPointInside(GeoPoint point) {
        if (!isPolygonComplete || polygonVertices.size() < 3) {
            return false;
        }
        
        double lat = point.getLatitude();
        double lon = point.getLongitude();
        
        boolean inside = false;
        int j = polygonVertices.size() - 1;
        
        for (int i = 0; i < polygonVertices.size(); i++) {
            double lat_i = polygonVertices.get(i).getLatitude();
            double lon_i = polygonVertices.get(i).getLongitude();
            double lat_j = polygonVertices.get(j).getLatitude();
            double lon_j = polygonVertices.get(j).getLongitude();
            
            if ((lon_i > lon) != (lon_j > lon) &&
                (lat < (lat_j - lat_i) * (lon - lon_i) / (lon_j - lon_i) + lat_i)) {
                inside = !inside;
            }
            j = i;
        }
        
        return inside;
    }
    
    /**
     * Check for boundary crossing and return event type
     * @param point Current position
     * @return "ENTERED", "EXITED", or null if no crossing
     */
    public String checkBoundaryCrossing(GeoPoint point) {
        if (!isPolygonComplete) {
            return null;
        }
        
        boolean isInside = isPointInside(point);
        String event = null;
        
        if (isInside && !wasInside) {
            event = "ENTERED";
            Log.i(TAG, "Boundary ENTERED at: " + point.getLatitude() + ", " + point.getLongitude());
        } else if (!isInside && wasInside) {
            event = "EXITED";
            Log.i(TAG, "Boundary EXITED at: " + point.getLatitude() + ", " + point.getLongitude());
        }
        
        wasInside = isInside;
        return event;
    }
    
    /**
     * Get current polygon vertices
     */
    public List<GeoPoint> getVertices() {
        return new ArrayList<>(polygonVertices);
    }
    
    /**
     * Check if polygon is complete
     */
    public boolean isComplete() {
        return isPolygonComplete;
    }
    
    /**
     * Get number of vertices
     */
    public int getVertexCount() {
        return polygonVertices.size();
    }
    
    /**
     * Check if currently inside boundary
     */
    public boolean isCurrentlyInside() {
        return wasInside;
    }
}
