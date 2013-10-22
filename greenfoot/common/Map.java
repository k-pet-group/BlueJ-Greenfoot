import greenfoot.*;
import javax.imageio.ImageIO;

/**
 * A helper class that fetch a googleMap for a specific location
 * as an Image.
 * 
 * <pre>
 * public class mapActor extends Actor
 * {
 *     Map map;
 *     public mapActor() 
 *     {
 *         map = new Map("Brazil");
 *         setImage(map.getImage());
 *     }
 *     
 *     public void act() 
 *     {
 *         map.zoomIn(1);
 *         setImage(map.getImage());
 *     }
 *     
 *     public void setType(String type) 
 *     {
 *         map.setType(type);
 *         setImage(map.getImage());
 *     }
 * }
 * </pre>
 * 
 * @author amjad
 * @version 1.0
 */
public class Map {

    private final String urlBase = "http://maps.googleapis.com/maps/api/staticmap?sensor=false";
    
    private int zoom;
    private int width;
    private int height;
    private String location;

    private GreenfootImage image;
    
    private enum maptype { roadmap, satellite, hybrid, terrain;}
    private maptype type = maptype.roadmap;
    
    /**
     * Construct a map object that build a map of a specific location,
     * e.g."-15.800513, -47.91378" or "Brooklyn Bridge, New York, NY".
     * The width and height fields will be set to default values as 600 x 400
     * The zoom field will be set to default as 5
     * 
     *  @param location  the location represents the center the map
     */
    public Map(String location)
    {
        this(location, 600, 400, 5);
    }
    
    /**
     * Construct a map object that build a map of a specific location,
     * e.g."-15.800513, -47.91378" or "Brooklyn Bridge, New York, NY".
     * 
     *  @param location  the location represents the center the map
     *  @param width     the image's width
     *  @param height    the image's height
     *  @param zoom      the zoom factor of the map [0-20]
     */
    public Map(String location, int width, int height, int zoom)
    {
        this.location = location;
        this.width = width;
        this.height = height;
        setZoom(zoom);
    }
    
    public void setZoom(int value)
    {
        zoom = value;
        if (zoom > 20) {
            zoom = 20;
        }
        if (zoom < 0) {
            zoom = 0;
        }
        buildImage();
    }
    
    private void buildImage()
    {
        String urlAddress = urlBase;
        urlAddress += "&center=" + location;
        urlAddress += "&size=" + width + "x" + height;
        urlAddress += "&zoom=" + zoom;
        urlAddress += "&maptype=" + type;
        
        image = new GreenfootImage(urlAddress);
    }
    
    public GreenfootImage getImage()
    {
        return image;
    }
    
    public void setType(String type)
    {
        try {
            this.type = maptype.valueOf(type);
        }
        catch(IllegalArgumentException ex) {
            System.err.println(type + " is not a valid map type. Please use: roadmap, satellite, hybrid or terrain");
        }
        buildImage();
    }
    
    public void zoomIn(int value)
    {
        setZoom(zoom + value);
    }
    
    public void zoomOut(int value)
    {
        setZoom(zoom - value);
    }
}