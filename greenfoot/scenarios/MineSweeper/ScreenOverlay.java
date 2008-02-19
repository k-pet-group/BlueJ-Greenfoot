import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;

/**
 * This is an Actor that can be placed anywhere inside the World
 * and it's image will always be the size of the entire world.
 * This allows you to draw to an image that fills the entire world.
 * 
 * Due to technical reasons, it is advised for users to NOT replce
 * the background of this image with a new image.
 * Instead the user should simply erase the contents of the image and draw over it.
 * If the image is replaced then may be shown off-screen.
 * 
 * The width and height of the image is also double that of the width and height
 * of the world. As such, the user should call the getWidth and getHeight method
 * of the actor instead, as this will return the width and height of the world instead.
 * 
 * @author Joseph Lenton - JL235@Kent.ac.uk
 * @version 13/02/08
 */
public class ScreenOverlay extends Actor
{
    /**
     * Trivial constructor
     */
    public ScreenOverlay()
    {
        // do nothing
    }
    
    /**
     * Must always be called,
     * it sets up the appropriate image for the actor.
     */
    public void addedToWorld(World world) {
        setImage(new OverlayImage(world, this));
    }
    
    public int getWidth() {
        return getImage().getWidth()/2;
    }
    
    public int getHeight() {
        return getImage().getHeight()/2;
    }
    
    /**
     * An OverlayImage which is the size of the world and draws according to where
     * the actor that uses this image is placed inside the world.
     * It overrides all of the GreenfootImage methods to allow the user to draw to
     * the image as though it was placed over the world.
     * 
     * It needs to be associated with an Actor and World to give it the correct size.
     * This works by creating an image big enough to fill the world.
     * Because actor images are centred, this means that it can be up to twice
     * the width and height of the world if the actor is placed at 0,0 in the world (up to 4 times)
     * However it will act as though the image only fills the world from 0,0 to the world width by height.
     * 
     * Note that getWidth and getHeight do not return the true width and height of the image,
     * but the width and height of the image covering the world.
     * 
     * The co-ordinates 0,0 on the image is the top left corner of the cell at 0,0 in the world.
     * 
     * Important: drawPolygon, fillPolygon, mirrorHorizontall, mirrorVertically, scale and rotate
     * are not fully implemented. It is advised NOT to use them as they will almost certainly not
     * work as expected.
     */
    private class OverlayImage extends GreenfootImage
    {
        // the world and actor associated with the image
        private Actor actor;
        private World world;
        
        // the width and height which the user will see
        private int width;
        private int height;
        
        /**
         * Creates an OverlayImage that will fill the entire world.
         * It is associated with the given actor and world.
         */
        private OverlayImage(World world, Actor actor) {
            // create an image that will fill the entire world, this actually means it must
            // be bigger then the world because it is centred when drawn by the Actor
//            super(
//                (world.getWidth()*world.getCellSize() - world.getCellSize()/2) * 2,
//                (world.getHeight()*world.getCellSize() - world.getCellSize()/2) * 2);
            super(
                world.getWidth()*world.getCellSize()*2,
                world.getHeight()*world.getCellSize()*2);
            
            // trivial setting of variables
            this.actor = actor;
            this.world = world;
        }
        
        /**
         * @return Where in the image the top left of the World is, along the X-axis.
         */
        private int getXOrigin() {
            return getWidth()/2 - ((actor.getX()*world.getCellSize()) + (world.getCellSize()/2));
        }
        
        /**
         * @return Where in the image the top left of the World is, along the Y-axis.
         */
        private int getYOrigin() {
            return getHeight()/2 - ((actor.getY()*world.getCellSize()) + (world.getCellSize()/2));
        }
        
        /**
         * Draw an image onto this image, compensating so that it is drawn in the correct place.
         * @param img The image to draw.
         * @param x The x position to draw it at.
         * @param y The y position to draw it at.
         */
        public void drawImage(GreenfootImage img, int x, int y) {
            super.drawImage(img, getXOrigin() + x, getYOrigin() + y);
        }
        
        /**
         * Draws a line, compensating so that it is drawn in the correct place.
         * @param x1 start of line x position
         * @param y1 start of line y position
         * @param x2 end of line x position
         * @param y2 end of line y position
         */
        public void drawLine(int x1, int y1, int x2, int y2) {
            int x = getXOrigin();
            int y = getYOrigin();
            setColor(Color.GREEN);
            super.drawLine(x1 + x, y1 + y, x2 + x, y2 + y);
        }
        
        /**
         * Fills the entire image with the draw colour.
         */
        public void fill() {
            fillRect(0, 0, getWidth(), getHeight());
        }
        
        /**
         * Draws a rectangle, compensating so that it is drawn in the correct place.
         * @param x X co-ordinate of the top-left corner of the rectangle.
         * @param y Y co-ordinate of the top-left corner of the rectangle.
         * @param width The width of the rectangle.
         * @param height The height of the rectangle.
         */
        public void drawRect(int x, int y, int width, int height) {
            super.drawRect(getXOrigin() + x, getYOrigin() + y, width, height);
        }
        
        /**
         * Draws an oval, compensating so that it is drawn in the correct place.
         * @param x X co-ordinate of the top-left corner of the oval.
         * @param y Y co-ordinate of the top-left corner of the oval.
         * @param width The width of the oval.
         * @param height The height of the oval.
         */
        public void drawOval(int x, int y, int width, int height) {
            super.drawOval(getXOrigin() + x, getYOrigin() + y, width, height);
        }
        
        /**
         * Fills a rectangle, compensating so that it is drawn in the correct place.
         * @param x X co-ordinate of the top-left corner of the rectangle.
         * @param y Y co-ordinate of the top-left corner of the rectangle.
         * @param width The width of the rectangle.
         * @param height The height of the rectangle.
         */
        public void fillRect(int x, int y, int width, int height) {
            super.fillRect(getXOrigin() + x, getYOrigin() + y, width, height);
        }
        
        /**
         * Fills an oval, compensating so that it is drawn in the correct place.
         * @param x X co-ordinate of the top-left corner of the oval.
         * @param y Y co-ordinate of the top-left corner of the oval.
         * @param width The width of the oval.
         * @param height The height of the oval.
         */
        public void fillOval(int x, int y, int width, int height) {
            super.fillOval(getXOrigin() + x, getYOrigin() + y, width, height);
        }
        
        /**
         * Draws the given string, compensating so that it is drawn in the correct place.
         * @param x X co-ordinate of where to draw the string.
         * @param y Y co-ordinate of where to draw the string.
         */
        public void drawString(String string, int x, int y) {
            super.drawString(string, getXOrigin() + x, getYOrigin() + y);
        }
        
        /**
         * @param x The X co-ordinate of where to get the color.
         * @param y The Y co-ordinate of where to get the color.
         * @return the color at x,y
         */
        public Color getColorAt(int x, int y) {
            return super.getColorAt(getXOrigin() + x, getYOrigin() + y);
        }
        
        /**
         * Sets the pixel at the given co-ordinates to the given colour.
         * This is also compensating for where it gets the colour from.
         * @param x X co-ordinate of where to set the colour.
         * @param y Y co-ordinate of where to set the colour.
         * @param color The colour to set at the given co-ordinates.
         */
        public void setColorAt(int x, int y, Color color) {
            super.setColorAt(getXOrigin() + x, getYOrigin() + y, color);
        }
    }
}
