package javablue.GUIBuilder;

import java.awt.*;
import java.awt.event.*;


/**
 * GUICanvasPropertyDialog.java
 * A Class for showing a Dialog to edit the properties of a Canvas
 *
 * Created: Oct  1 11:19:22 1998
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class GUICanvasPropertyDialog extends GUIPropertyDialog
{

     /**
       * Constructs a GUICanvasPropertyDialog. It enables the user to edit the properties of a Canvas.
       @param f Frame
       @param component The GUICanvas to be changed.
       @param componentStr A String that describes the component. "Canvas"
       @param structCont The StructureContainer in which the component resides.
       */
    public GUICanvasPropertyDialog(Frame f, GUIComponent component,String componentStr, StructureContainer structCont)
    {
        super(f,component,componentStr,structCont);
        setTitle("Canvas Properties");
        init();
    }


     /**
       * Modify the component, so that the changes in the component becomes persistent.
       */
    public void modifyComponent()
    {
        super.modifyComponent();
    }
}
