package javablue.GUIBuilder;

import java.util.Vector;
import java.io.Serializable;
import java.awt.*;


/**
 * A class encapsulating generic information about a component.
 * It is as a Utility class used in the various components to avoid tedious typing.
 * It can generate code for setting generic properties.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class ComponentDescriptor implements Serializable
{
    private String colorString = new String();
    private String bcolorString = new String();
    private Vector listenerVector = new Vector();
    private GUIComponent component = null;
    private int initlevel = ComponentCode.METHODLEVEL;

    private int[] cursors ={ Cursor.CROSSHAIR_CURSOR,Cursor.DEFAULT_CURSOR,Cursor.E_RESIZE_CURSOR,Cursor.HAND_CURSOR,Cursor.MOVE_CURSOR,Cursor.N_RESIZE_CURSOR,Cursor.NE_RESIZE_CURSOR,Cursor.NW_RESIZE_CURSOR,Cursor.S_RESIZE_CURSOR,Cursor.SE_RESIZE_CURSOR,Cursor.SW_RESIZE_CURSOR,Cursor.TEXT_CURSOR,Cursor.W_RESIZE_CURSOR,Cursor.WAIT_CURSOR 
    };


     /**
     * Constructs a ComponentDescriptor.
     *
     * @param component	    The component of the ComponentDescriptor.
     */
    public ComponentDescriptor(GUIComponent component)
    {
            this.component = component;
    }

    
     /**
     * Sets the Initialization level of the component. Possible values are:
     * ComponentCode.CLASSLEVEL, ComponentCode.METHODLEVEL, ComponentCode.UNREFERENCEABLE
     *
     * @param level	The level on which the component should be declared. 
     */
    public void setInitLevel(int level)
    {
	initlevel = level;
    }


    /**
     * Returns the initialization level of the component. Possible values are:
     * ComponentCode.CLASSLEVEL, ComponentCode.METHODLEVEL, ComponentCode.UNREFERENCEABLE
     *
     * @return	The level of which the component will be declared.
     */
    public int getInitLevel()
    {
	return initlevel;
    }

    
    /**
       * Get the value of colorString, which contains the description of the foreground color of the component.
       * @return Value of colorString.
       */
    public String getColorString()
    {
        return colorString;
    }

    
    /**
       * Set the value of colorString, which contains the description of the foreground color of the component.
       * @param v  Value to assign to colorString.
       */
    public void setColorString(String  v)
    {
        this.colorString = v;
    }


    /**
       * Get the value of bcolorString, which contains the description of the background color of the component.
       * @return Value of bcolorString.
       */
    public String getBcolorString()
    {
        return bcolorString;
    }

    
    /**
       * Set the value of bcolorString, which contains the description of the background color of the component.
       * @param v  Value to assign to bcolorString.
       */
    public void setBcolorString(String v) 
    {
        this.bcolorString = v;
    }
    

     /**
     * Returns a Vector of listeners assigned to the component.
     *
     * @return	Vector of listeners assigned to the component.
     */
    public Vector getListenerVector()
    {
        return listenerVector;
    }


     /**
     * Returns a String containing the code to set the foreground color of the component.
     * If the parent container has the same foreground color, an empty String is returned, in order to avoid unnecessary code.
     *
     * @return	Code for the color.
     */
    public String getColorCode(String name)
    {
        Color fgc = ((Component)component).getForeground();
        GUIComponentNode parent = getNearestContainer();
        if(colorString.indexOf("System") != -1)
        {
            if(parent==null)
                return "setForeground("+colorString+");\n";
            else
                return name+".setForeground("+colorString+");\n";
        }
            
            
        if(parent == null)
        {
            if(colorString.equals(""))
                return "";
            else
                return "setForeground("+colorString+");\n";
        }
        Color cfgc = ((Container)parent).getForeground();
        if(fgc.getRGB() == cfgc.getRGB())
            return "";
        else
            return name+".setForeground("+colorString+");\n";
            
            
    }


     /**
     * Returns a String containing the code to set the background color of the component.
     * If the parent container has the same background color, an empty String is returned, in order to avoid unnecessary code.
     *
     * @return	Code for the color.
     */
    public String getBcolorCode(String name)
    {
        String answerString = "setBackground("+bcolorString+");\n";
        
        Color bgc = ((Component)component).getBackground();
        GUIComponentNode parent = getNearestContainer();
        if(bcolorString.indexOf("System") != -1)
        {
            if(parent == null)
                return answerString;
            else
                return name+"."+answerString;
        }
            
        if(parent == null)
        {
            if(bcolorString.equals(""))
                return "";
            else
                return answerString;
        }
        Color cbgc = ((Container)parent).getBackground();
        if(bgc.getRGB() == cbgc.getRGB())
            return "";
        else
        {
            return name+"."+answerString;
        }
    }
    


    private GUIComponentNode getNearestContainer()
    {
        // return the nearest container of the component. Null, if none. 
        GUIComponentNode gui = component.getTreeParent();
        if(gui == null)
            return null;
        
        if(gui instanceof Container)
        {
            return gui;
        }
        else if(gui.getTreeParent() instanceof Container)
        {
            return gui.getTreeParent();
        }
        else
            return null;
            
    }


     /**
     * Returns a String containing the code to set the Font of the component.
     * If the parent container has the same Font color, an empty String is returned, in order to avoid unnecessary code.
     *
     * @return	Code for the Font.
     */
    public String getFontCode(String name)
    {
        Font font = ((Component)component).getFont();
        String face = new String("Font.PLAIN");
        if(font.isBold())
            face = "Font.BOLD";
        if(font.isItalic())
            face = "Font.ITALIC";
        if(font.isBold() && font.isItalic())
            face = "Font.BOLD|Font.ITALIC";
        
        GUIComponentNode gui = getNearestContainer();
        if(gui == null)
        {
            String answerString = "setFont(new Font(\""+font.getName()+"\","+face+","+font.getSize()+"));\n";
            return answerString;
        }
        Font pFont = ((Component)gui).getFont();
            
        if(!font.equals(pFont))
        {
            String answerString = name+".setFont(new Font(\""+font.getName()+"\","+face+","+font.getSize()+"));\n";
            return answerString;
        }

        return "";
    }

     /**
     * Returns a String containing the code to set the colors, Font and the Cursor of the component.
     * Only the code to set the properties that differ from the parent container is returned, in order to avoid unnecessary code.
     *
     * @return	Code for the properties.
     */
    public String getDescriptionCode(String name)
    {
        StringBuffer temp = new StringBuffer();
        temp.append(getColorCode(name));
        temp.append(getBcolorCode(name));
        temp.append(getFontCode(name));
        temp.append(getCursorCode(name));
        
        return temp.toString();
    }


     /**
     * Makes a copy of the component. This is used for the preview function.
     * It is used in all the subclasses of GUIConcreteComponent.
     *
     * @see StructureContainer#preview()
     */
    public void cloneComponent(Component cloneComponent)
    {
        Component cmpComponent = (Component)component;
            // for speed
        cloneComponent.setForeground(cmpComponent.getForeground());
        cloneComponent.setBackground(cmpComponent.getBackground());
        cloneComponent.setFont(cmpComponent.getFont());
        cloneComponent.setCursor(cmpComponent.getCursor());
    }

    
    public String getCursorCode(String name)
    {
        Cursor cursor = ((Component)component).getCursor();
            
        GUIComponentNode parent = getNearestContainer();
        if(parent == null)
        {
            if(cursor.getType() == Cursor.DEFAULT_CURSOR)
                return "";
            else
                return "setCursor(new Cursor("+getCursorTypeString(cursor)+"));\n";
        }
        Cursor pCursor = ((Container)parent).getCursor();
            
        if(cursor.getType() == pCursor.getType())
            return "";
        else
            return name+".setCursor(new Cursor("+getCursorTypeString(cursor)+"));\n";
    }


         /**
     * Returns a String containing the code to set the cursor of the component.
     * If the parent container has the same cursor, an empty String is returned, in order to avoid unnecessary code.
     *
     * @return	Code for the cursor.
     */

    public String getCursorTypeString(Cursor cursor)
    {
        int cursorType = cursor.getType();
        
        if(cursorType == Cursor.CROSSHAIR_CURSOR)
            return "Cursor.CROSSHAIR_CURSOR";
        if(cursorType == Cursor.DEFAULT_CURSOR)
            return "Cursor.DEFAULT_CURSOR";
        if(cursorType == Cursor.E_RESIZE_CURSOR)
            return "Cursor.E_RESIZE_CURSOR";
        if(cursorType == Cursor.HAND_CURSOR)
                return "Cursor.HAND_CURSOR";
        if(cursorType == Cursor.MOVE_CURSOR)
            return "Cursor.MOVE_CURSOR";
        if(cursorType == Cursor.N_RESIZE_CURSOR)
                return "Cursor.N_RESIZE_CURSOR";
        if(cursorType == Cursor.NE_RESIZE_CURSOR)
            return "Cursor.NE_RESIZE_CURSOR";
        if(cursorType == Cursor.NW_RESIZE_CURSOR)
                return "Cursor.NW_RESIZE_CURSOR";
        if(cursorType == Cursor.S_RESIZE_CURSOR)
            return "Cursor.S_RESIZE_CURSOR";
        if(cursorType == Cursor.SE_RESIZE_CURSOR)
            return "Cursor.SE_RESIZE_CURSOR";
        if(cursorType == Cursor.SW_RESIZE_CURSOR)
            return "Cursor.SW_RESIZE_CURSOR";
        if(cursorType == Cursor.TEXT_CURSOR)
                return "Cursor.TEXT_CURSOR";
        if(cursorType == Cursor.W_RESIZE_CURSOR)
            return "Cursor.W_RESIZE_CURSOR";
        if(cursorType == Cursor.WAIT_CURSOR)
            return "Cursor.WAIT_CURSOR";
        return "Cursor.DEAFULT_CURSOR";
    }
}
