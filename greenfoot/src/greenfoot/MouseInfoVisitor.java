package greenfoot;




/**
 * To get access to package private methods in MouseInfo.
 * 
 * @author Poul Henriksen
 *
 */
public class MouseInfoVisitor
{
    public static void setActor(MouseInfo info, Actor actor) {
        info.setActor(actor);
    }    

    public static void setLoc(MouseInfo info, int x, int y) {
        info.setLoc(x, y);
    }

    public static void setButton(MouseInfo info, int button)
    {
        info.setButton(button);
    }    
    
    public static MouseInfo newMouseInfo()
    {
        return new MouseInfo();
    }
}
