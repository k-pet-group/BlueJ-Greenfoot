package bluej.guibuilder.graphics;

/**
 * Constants for border styles.
 *
 * This class may not be instantiated.
 *
 * @version 1.0, Apr 11 1996
 * @author  David Geary
 */
public class BorderStyle {
    public static final BorderStyle NONE   = new BorderStyle();
    public static final BorderStyle RAISED = new BorderStyle();
    public static final BorderStyle INSET  = new BorderStyle();
    public static final BorderStyle ETCHED = new BorderStyle();
    public static final BorderStyle SOLID  = new BorderStyle();

    public String toString() {
		String s = new String();

        if(this == BorderStyle.NONE)                      
            s = getClass().getName() + "=NONE";
        else if(this == BorderStyle.RAISED) 
            s = getClass().getName() + "=RAISED";
        else if(this == BorderStyle.INSET) 
            s = getClass().getName() + "=INSET";
        else if(this == BorderStyle.ETCHED) 
            s = getClass().getName() + "=ETCHED";
        else if(this == BorderStyle.SOLID) 
            s = getClass().getName() + "=SOLID";

		return s;
    }
    private BorderStyle() { }  // defeat instantiation
}
