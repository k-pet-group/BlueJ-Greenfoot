package bluej.guibuilder;

import java.awt.Window;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.ClassTarget;
import bluej.editor.*;
import bluej.editor.moe.*;


/**
 * A class used to store all listeners, checkbox groups and components that
 * make up a window.
 *
 * Created: Oct 3, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
public class StructureContainer implements Serializable
{
    private GUIComponentNormalNode guiTree;
    private ListenerContainer listeners = new ListenerContainer();
    private CheckboxGroupContainer groups = new CheckboxGroupContainer();
    private transient GUIBuilderApp app;
    private transient ClassTarget target = null;
    private StructureContainer structCont;

    private WindowHandler windowHandler = new WindowHandler();


    /**
     * Constructs a new StructureContainer with the specified component as root
     * of the GUIComponent tree.
     *
     * @param app	A reference to the GUIBuilder application.
     * @param treeTop	The component to use as the root. This is either a GUIFrame or a GUIDialog.
     */
    public StructureContainer (GUIBuilderApp app, GUIComponentNormalNode treeTop)
    {
	this.app = app;
	structCont = this;
	guiTree = treeTop;
	treeTop.setStructureContainer(this);
	treeTop.setGUILayout (new GUIBorderLayout (guiTree, this, app));

	Package pkg = app.getPackage();
	if (pkg!=null)
	{
	    target = new ClassTarget(app.getPackage(), guiTree.getName());
	    target.setAbstract(false);
	    target.setInterface(false);
	    pkg.addTarget(target);
	    target.invalidate();
//	    pkg.getFrame().setModified(true);
	}

	((Window)guiTree).addWindowListener(windowHandler);
	show();

	if (pkg!=null)
	    saveCode();
    }


    /**
     * Deletes this structure and everything it contains.
     */
    public void delete()
    {
	app.removeStructure(structCont);
	((Window)guiTree).dispose();
    }


    /**
     * Sets a reference to the main application.
     *
     * @param	The main GUIBuilderApp application.
     */
    public void setMainApp(GUIBuilderApp app)
    {
	this.app = app;
	guiTree.setMainApp(app);
    }


    /**
     * Returns the GUIComponent tree of this structure.
     *
     * @return The root of the tree.
     */
    public GUIComponentNormalNode getTree()
    {
	return guiTree;
    }


    /**
     * Returns the listener container of this structure.
     *
     * @return The container containing all listeners associated with this structure.
     */
    public ListenerContainer getListenerContainer()
    {
	return listeners;
    }


    /**
     * Returns the checkbox group container of this structure.
     *
     * @return The container containing all checkbox groups associated with this structure.
     */
    public CheckboxGroupContainer getCheckboxGroupContainer()
    {
	return groups;
    }



    public ClassTarget getClassTarget()
    {
	return target;
    }

    /**
     * Generates the Java code used to make this entire GUI-structure. The code
     * generates the visual elements and all associated listeners, and it is
     * indented according to normal standards.
     *
     * @return	The Java code used to generate this structure.
     */
    public String generateCode()
    {
	ComponentCode code = guiTree.generateCode();
	String buffer = new String(code.getGlobalCode()+code.getCreationCode());
	return ComponentCode.doIndent(buffer);
    }


    /**
     * Shows a preview of the GUI in this structure. It contains no
     * GUIContainers, and looks like the GUI generated with the generated code.
     */
    public void preview()
    {
	Window preview = (Window)(guiTree.display());
	preview.pack();
	preview.show();
    }


    /**
     * Shows this structure container on the screen. This is used after a
     * structure is loaded from disk.
     */
    public void show()
    {
	((Window)guiTree).pack();
	Dimension size = ((Window)guiTree).getSize();
	Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	((Window)guiTree).setLocation((screen.width-size.width)/2,(screen.height-size.height)/2);
	((Window)guiTree).show();
    }


    /**
     * Redraws this structure, expanding the size of the window in case this
     * is needed to fit all components.
     */
    public void redraw()
    {
	Dimension size = ((Window)guiTree).getSize();
	Dimension pref = ((Window)guiTree).getPreferredSize();
	Point location = ((Window)guiTree).getLocation();

	int height = size.height;
	int width = size.width;
	int x = location.x;
	int y = location.y;
	if (size.height<pref.height)
	{
	    y -= (pref.height-size.height)/2;
	    if (y<0)
		y=0;
	    height = pref.height;
	}
	if (size.width<pref.width)
	{
	    x -= (pref.width-size.width)/2;
	    if (x<0)
		x=0;
	    width = pref.width;
	}

	((Window)guiTree).setLocation(x, y);
	((Window)guiTree).setSize(width, height);
        ((Window)guiTree).validate();
        ((Window)guiTree).repaint();

	if (target!=null)
	    saveCode();
    }


    public void saveCode()
    {
	try
	{
	    PrintWriter pw = new PrintWriter(new FileOutputStream(target.sourceFile()));
	    pw.print(generateCode());
	    pw.flush();
	    pw.close();

	    if (target.editorOpen())
	    {
		Editor editor = target.getEditor();
	    	target.getEditor().reloadFile();
	    }

	    target.invalidate();
	}
	catch (IOException e)
	{
	    System.out.println("Exception: "+e.getMessage());
	}
    }


    /**
     * A class used to handle window events on the container.
     *
     * Created: Oct 3, 1998.
     *
     * @author Morten Knudsen & Kent Hansen
     * @version 1.0
     */
    private class WindowHandler extends WindowAdapter implements Serializable
    {
	public void windowActivated (WindowEvent e)
	{
	    // Select this structure when activated:
	    app.setSelectedStructure(structCont);
	}

	public void windowClosing (WindowEvent e)
	{
	    delete();
	}
    }
}
