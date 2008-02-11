package greenfoot;
/**
 * This class contains information about the current status of the mouse. You
 * can get a MouseInfo object via {@link Greenfoot.#getMouseInfo()}.
 * 
 * @see Greenfoot.#getMouseInfo()
 * @author Poul Henriksen
 * @version 1.4.0
 */
public class MouseInfo
{    
    
    private Actor actor;
    private int button;
    private int x;
    private int y;;
    
    /**
     * Do not create your own MouseInfo objects. Use
     * {@link Greenfoot.#getMouseInfo()}.
     * 
     * @see Greenfoot.#getMouseInfo()
     */
    MouseInfo() {        
    }
    
    /**
     * Returns the current x position of the mouse cursor.
     * 
     * @return the x position in grid coordinates
     */
    public int getX() 
    {
        return x;
    }

    /**
     * Returns the current y position of the mouse cursor.
     * 
     * @return the y position in grid coordinates
     */
    public int getY() 
    {
        return y;
    }
    
    /**
     * Return the actor (if any) that the current mouse behaviour is related to.
     * If the mouse was clicked or pressed the actor it was clicked on will be
     * returned. If the mouse was dragged or a drag ended, the actor where the
     * drag started will be returned. If the mouse was moved, it will return the
     * actor that the mouse is currently over.
     * 
     * @return Actor that the current mouse behaviour relates to, or null if
     *         there is no actor related to current behaviour. 
     */
    public Actor getActor()
    {
        return actor;
    }
    
    /**
     * The number of the pressed or clicked button (if any).
     * 
     * @return The button number. Usually 1 is the left button, 2 is the middle
     *         button and 3 is the right button.
     */
    public int getButton() {
        return button;
    }

    void setButton(int button)
    {
        this.button = button;
    }

    void setLoc(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    void setActor(Actor actor)
    {
        this.actor = actor;
    }
    
    public String toString() 
    {
        return "MouseInfo. Actor: " + actor + "  Location: (" + x + "," + y + ")  Button: " + button;
    }    
}
