package greenfoot;
/**
 * This class contains information about the current status of the mouse.
 * 
 * @author Poul Henriksen
 */
public class MouseInfo
{    
    
    private Actor actor;
    private int button;
    private int x;
    private int y;;
    
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
     * The pressed or clicked button (if any),
     * 
     * TODO maybe it would be more convenient with isLeftButton, isRightButton
     * 
     * @return The button {@link Button}
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
    
}
