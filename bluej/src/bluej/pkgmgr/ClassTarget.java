package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerClassLoader;
import bluej.debugger.Invoker;
import bluej.debugger.ObjectViewer;
import bluej.debugger.ResultWatcher;
import bluej.debugger.ObjectWrapper;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;
import bluej.editor.Editor;
import bluej.graph.GraphEditor;
import bluej.utility.Utility;
import bluej.utility.BlueJFileReader;
import bluej.views.ConstructorView;
import bluej.views.EditorPrintWriter;
import bluej.views.MemberView;
import bluej.views.CallableView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;


/** 
 ** A class target in a package, i.e. a target that is a class file
 ** built from Java source code
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** @author Bruce Quig
 **
 ** @version $Id: ClassTarget.java 284 1999-11-25 02:34:37Z ajp $
 **/
public class ClassTarget extends EditableTarget 

	implements ActionListener
{
    // Define Background Colours
    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color librarybg = Config.getItemColour("colour.class.bg.imported");
    static final Color abstractbg = Config.getItemColour("colour.class.bg.abstract");
    static final Color interfacebg = Config.getItemColour("colour.class.bg.interface");
    static final Color compbg = Config.getItemColour("colour.target.bg.compiling");

    static final Color colBorder = Config.getItemColour("colour.target.border");
    static final Color graphbg = Config.getItemColour("colour.graph.background");
    static final Color textfg = Config.getItemColour("colour.text.fg");

    static public final Font normalFont = new Font("SansSerif", Font.BOLD, Config.fontsize);
    static public final Font menuFont = new Font("SansSerif", Font.PLAIN, Config.fontsize);
    static final Font italicMenuFont = new Font("SansSerif", Font.ITALIC, Config.fontsize);
    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    static String usesArrowMsg = Config.getString("pkgmgr.usesArrowMsg");

    static final Image brokenImage = Toolkit.getDefaultToolkit().getImage(
					Config.getImageFilename("image.broken"));

    // variables
    private ClassRole role;

    protected int modifiers;
    protected Vector breakpoints = new Vector();
    protected int displayedView = Editor.IMPLEMENTATION;
    protected SourceInfo sourceInfo = new SourceInfo();

    // Fields used in Tarjan's algorithm:
    public int dfn, link;

  
    /**
     * Create a new class target in package 'pkg'.
     */
    public ClassTarget(Package pkg, String name)
    {
	this(pkg, name, false);
    }

    /**
     * Create a new class target in package 'pkg'.
     */
    public ClassTarget(Package pkg, String name, boolean isApplet)
    {
	super(pkg, name);
	if(isApplet)
	    role = new AppletClassRole();
	else
	    role = new StdClassRole();
    }

    /**
     *  Create a new class target in package 'pkg' without a name. The
     *  name must be set later.
     */
    public ClassTarget(Package pkg, boolean isApplet)
    {
	this(pkg, null, isApplet);
    }


    /**
     * load existing information about this class target
     * @param props the properties object to read
     * @param prefix an internal name used for this target to identify 
     * its properties in a properties file used by multiple targets.
     */
    public void load(Properties props, String prefix) throws NumberFormatException
    {
	super.load(props, prefix);
	role.load(props, prefix);
	String modifierStr = props.getProperty(prefix + ".modifiers", "0");
	modifiers = Integer.parseInt(modifierStr, 16);
	sourceInfo.load(props, prefix);
    }

    /**
     * save information about this class target
     * @param props the properties object to save to
     * @param prefix an internal name used for this target to identify 
     * its properties in a properties file used by multiple targets.
     */
    public void save(Properties props, String prefix)
    {
	super.save(props, prefix);
	role.save(props,modifiers, prefix);
	sourceInfo.save(props, prefix);
    }
	
    /**
     * Copy all the files belonging to this target to a new location.
     * For class targets, that is the source file, and possibly (if compiled)
     * class and context files.
     *
     * @param directory The directory to copy into (ending with "/")
     */
    public boolean copyFiles(String directory)
    {
	boolean okay = true;

	if (!BlueJFileReader.copyFile(sourceFile(), 
				      directory + name + ".java"))
	    okay = false;

	if(upToDate()) {
	    if(!BlueJFileReader.copyFile(classFile(), 
					 directory + name + ".class"))
		okay = false;
	    if(!BlueJFileReader.copyFile(contextFile(), 
					 directory + name + ".ctxt"))
		okay = false;
	}
	return okay;
    }

    /**
     * Check if the compiled class and the source are up to date
     * @return true if they are in sync otherwise false.
     */
    public boolean upToDate()
    {
	try {	
	    // Check if the class file is up to date
	    File src = new File(sourceFile());
	    File clss = new File(classFile());

	    if(!clss.exists()
	       || (src.exists() && (src.lastModified() > clss.lastModified())))
		return false;
	} catch(Exception e) {
	    e.printStackTrace();
	}
		
	return true;
    }
	
    /**
     * Mark this class as modified, and mark all dependent classes too
     */
    public void invalidate()
    {
	setState(S_INVALID);
		
	for(Enumeration e = dependents(); e.hasMoreElements(); ) {
	    Dependency d = (Dependency)e.nextElement();
	    Target dependent = d.getFrom();
	    dependent.setState(S_INVALID);
	}
    }


    /**
     * return the modifiers associated with this class target
     * @return int representing the modifiers
     */
    public int getModifiers()
    {
	return modifiers;
    }
	
    /**
     * verify whether this class target is an interface
     * @return true if class target is an interface, else returns false
     */
    public boolean isInterface()
    {
	return Modifier.isInterface(modifiers);
    }

    /**
     * change modifiers so that this class is either an interface or not
     * @param isInterface boolean value reperesenting whether target should be set
     *  to an interface or not
     */
    public void setInterface(boolean isInterface)
    {
	if(isInterface)
	    modifiers |= Modifier.INTERFACE;
	else
	    modifiers &= ~Modifier.INTERFACE;
    }

    /**
     * verify whether this class target is an abstract class
     * @return true if class target is an abstract class, else returns false
     */	
    public boolean isAbstract()
    {
	return Modifier.isAbstract(modifiers);
    }

    /**
     * change modifiers so that this class is either an interface or not
     * @param isAbstract boolean value reperesenting whether target should be set
     *  to abstract or not
     */
    public void setAbstract(boolean isAbstract)
    {
	if(isAbstract)
	    modifiers |= Modifier.ABSTRACT;
	else
	    modifiers &= ~Modifier.ABSTRACT;
    }
	

    Color getDefaultBackground()
    {
	if(isInterface())
	    return interfacebg;
	else if(isAbstract())
	    return abstractbg;
	else if(isLibrary())
	    return librarybg;
	else
	    return defaultbg;
    }

    // --- Target interface ---

    Color getBackgroundColour()
    {
	if(state == S_COMPILING)
	    return compbg;
	else
	    return getDefaultBackground();
    }

    Color getBorderColour()
    {
	return colBorder;
    }

    Color getTextColour()
    {
	return textfg;
    }

    Font getFont()
    {
	return normalFont;
    }

    // --- EditableTarget interface ---

    /**
     * @return a boolean indicating whether this target contains source code
     */
    protected boolean isCode()
    {
	return true;
    }

    /**
     * @return the name of the (text) file this target corresponds to.
     */
    public String sourceFile()
    {
	return pkg.getFileName(name) + ".java";
    }
    
    /**
     * @return the name of the context(.ctxt) file this target corresponds to.
     */
    public String contextFile()
    {
	return pkg.getFileName(name) + ".ctxt";
    }	
    /**
     ** @return the editor object associated with this target. May be null
     **  if there was a problem opening this editor.
     **/
    public Editor getEditor()
    {
	if(editor == null)
	    editor = pkg.editorManager.openClass(sourceFile(), name, this,
						 isCompiled(), breakpoints);
	return editor;
    }
	
    /**
     * @return the current view being shown - one of the Editor constants
     */
    public int getDisplayedView()
    {
	return displayedView;
    }
	
    // --- EditorWatcher interface ---

    /**
     * Called by Editor when a file is changed
     */
    public void modificationEvent(Editor editor)
    {
	invalidate();
    }

    /**
     * Called by Editor when a file is saved
     * @param editor	the editor object being saved
     */
    public void saveEvent(Editor editor) 
    {
	analyseDependencies();
    }

    /**
     * Called by Editor when a breakpoint is been set/cleared
     * @param filename	the name of the file that was modified
     * @param lineNo	the line number of the breakpoint
     * @param set	whether the breakpoint is set (true) or cleared
     *
     * @return  null if there was no problem, or an error string
     */
    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    {
	if(isCompiled()) {
	    DebuggerClassLoader loader = pkg.getRemoteClassLoader();
	    return Debugger.debugger.toggleBreakpoint(name, lineNo, set, 
						      loader);
	}
	else
	    return "Class has to be compiled to set breakpoints.";
    }

    /**
     * Called by Editor to change the view displayed by an editor
     * @param viewname	the name of the view to display, should be 
     * 		one of bluej.editor.Editor.PUBLIC, etc.
     * @return a boolean indicating if the change was allowed
     */
    public boolean changeView(Editor editor, int viewType)
    {
	showView(editor, viewType);
	return true;
    }


    public void compile(Editor editor)
    {
	pkg.compile(this);
    }

    // --- end of EditorWatcher interface ---

    // --- end of EditableTarget interface ---

    protected void removeBreakpoints()
    {
	if(editor != null)
	    editor.removeBreakpoints();
    }

    protected void removeStepMark()
    {
	if(editor != null)
	    editor.removeStepMark();
    }

    protected boolean isCompiled()
    {
	return (state == S_NORMAL);
    }


    public String classFile()
    {
	return pkg.getClassFileName(name) + ".class";
    }


    /**
     * generates a source code skeleton for this class	
     *
     */
    public void generateSkeleton()
    {
	// delegate to role object
	role.generateSkeleton(pkg, name, sourceFile(), isAbstract(), isInterface());
	// do we need to check whether skeleton generated before setting state?
	setState(Target.S_INVALID);
    }  


    /** 
     *  Analyse the current dependencies in the source code and update the
     *  dependencies in the graphical display accordingly.
     */
    public void analyseDependencies()
    {
	// currently we don't remove uses dependencies, but just warn
        
	//removeAllOutDependencies();
	removeInheritDependencies();
	unflagAllOutDependencies();

        try {
            ClassInfo info = ClassParser.parse(sourceFile(), 
					       pkg.getAllClassnames());
    
            if(info.isApplet()) {
                if( ! (role instanceof AppletClassRole))
                    role = new AppletClassRole();
            }
            else {
                if( ! (role instanceof StdClassRole))
                    role = new StdClassRole();
            }
    
            setInterface(info.isInterface());
            setAbstract(info.isAbstract());
    
            // handle superclass
    
            if(info.getSuperclass() != null) {
                Target superclass = pkg.getTarget(info.getSuperclass());
                if (superclass != null)
                    pkg.addDependency(
                              new ExtendsDependency(pkg, this, superclass), 
                              false);
            }
    
            // handle implemented interfaces
    
            Vector vect = info.getImplements();
            for(Enumeration e = vect.elements(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            Target interfce = pkg.getTarget(name);
            // Debug.message("Implements " + name);
            if (interfce != null) {
               pkg.addDependency(
                          new ImplementsDependency(pkg, this, interfce), 
                          false);
                }
            }
            // handle used classes
    
            vect = info.getUsed();
            for(Enumeration e = vect.elements(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            Target used = pkg.getTarget(name);
            if (used != null)
                pkg.addDependency(new UsesDependency(pkg, this, used), true);
            }
    
            sourceInfo.setValid(true);

	    checkForUsesInconsistencies();
        }
        catch(Exception e) {
            // exception during parsing
            sourceInfo.setValid(false);
        }
    
        pkg.repaint();
    }

    private void checkForUsesInconsistencies()
    {
	for(int i = 0; i < outUses.size(); i++) {
	    UsesDependency usesDep = ((UsesDependency)outUses.elementAt(i));
	    if(! usesDep.isFlagged())
		pkg.getFrame().setStatus(usesArrowMsg + usesDep);
	}
    }


    protected Class last_class = null;
    protected JPopupMenu menu = null;
    boolean compiledMenu = false;

    public void popupMenu(MouseEvent evt, int x, int y, GraphEditor editor)
    {
	if (state == S_NORMAL) {
//  	    Class cl = pkg.loadClass(fullname);
	    Class cl = pkg.loadClass(name);
	    if ((cl != null) && (last_class != cl)) {
		if (menu != null)
		    editor.remove(menu);
		menu = createMenu(cl, editor.getFrame());
		editor.add(menu);
		compiledMenu = true;
	    }
	    last_class = cl;
	}
	else {
	    if (compiledMenu || menu == null) {
		menu = createMenu(null, editor.getFrame());
		editor.add(menu);
		compiledMenu = false;
	    }
	}
	if (menu != null)
	    menu.show(editor, evt.getX(), evt.getY());
    }
	
    protected Hashtable actions;

    /**
     * creates a popup menu for this class target.
     *
     * @param cl Class object associated with this class target
     * @param editorFrame the parent editorFrame
     *
     * @return the created popup menu object
     */
    protected JPopupMenu createMenu(Class cl, JFrame editorFrame) {
        actions = new Hashtable();
	
        JPopupMenu menu = new JPopupMenu(getName() + " operations");
	
    	// call on role object to add any options needed
     	role.createMenu(menu, this, state);
    
    	if ((cl != null) && (!isAbstract()))
    	    createClassMenu(menu, cl);
    	
    	addMenuItem(menu, editStr, true);
    	addMenuItem(menu, publicStr, (state == S_NORMAL));
    	addMenuItem(menu, pkgStr, (state == S_NORMAL));
    	addMenuItem(menu, inheritedStr, (state == S_NORMAL));
    	menu.addSeparator();
    	addMenuItem(menu, compileStr, true);
    	addMenuItem(menu, removeStr, true);
    	
    	return menu;
    }

    /**
     * adds a menu item to a popup menu.
     *
     * @param menu the popup menu the item is to be added to
     * @param itemString the String to be displayed on menu item
     * @param enabled boolean value representing whether item should be enabled
     *
     */
    protected void addMenuItem(JPopupMenu menu, String itemString, boolean enabled)
    {
        //	 role.addMenuItem(menu, itemString, enabled);
    	JMenuItem item;
    
    	menu.add(item = new JMenuItem(itemString));
    	item.addActionListener(this);
    	item.setFont(menuFont);
    	item.setForeground(envOpColour);
    	if(!enabled)
    	    item.setEnabled(false);
    }

    /**
     * creates a class menu containing any constructors and static methods etc.
     *
     * @param menu the popup menu to add the class menu items to
     * @param cl Class object associated with this class target
     *
     */
    protected void createClassMenu(JPopupMenu menu, Class cl)
    {
	View view = View.getView(cl);
	ViewFilter filter= new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PACKAGE);
	ConstructorView[] constructors = view.getConstructors();
	
	if (createMenuItems(menu, constructors, filter, 0, constructors.length, "new "))
	    menu.addSeparator();
		
	filter = new ViewFilter(ViewFilter.STATIC | ViewFilter.PROTECTED);
	MethodView[] allMethods = view.getAllMethods();
	if(createMenuItems(menu, allMethods, filter, 0, allMethods.length, ""))
	    menu.addSeparator();
    }
	

    protected boolean createMenuItems(JPopupMenu menu,
				      CallableView[] members, ViewFilter filter, 
				      int first, int last, String prefix)
    {
	// Debug.message("Inside ClassTarget.createMenuItems\n first = " + first + " last = " + last);
	boolean hasEntries = false;
	JMenuItem item;
		
	for(int i = first; i < last; i++) {
	    try {
		CallableView m = members[last - i - 1];
		if(!filter.accept(m))
		    continue;
		// Debug.message("createSubMenu - creating MenuItem");
		item = new JMenuItem(prefix + m.getShortDesc());
		item.addActionListener(this);
		item.setFont(menuFont);
		actions.put(item, m);
		menu.add(item);
		hasEntries = true;
	    } catch(Exception e) {
		Debug.reportError("Exception accessing methods: " + e);
		e.printStackTrace();
	    }
	}
	return hasEntries;
    }


    /**
     *  Draw this target, redefined from Target.
     */
    public void draw(Graphics g)
    {
	super.draw(g);
	if(!sourceInfo.isValid())
	    g.drawImage(brokenImage, x + TEXT_BORDER, y + height - 22, null);
	// delegate extra functionality to role object
	role.draw(g, this, x, y, width, height);
    }


    // -- ActionListener interface --

    public void actionPerformed(ActionEvent e)
    {
      //role.actionPerformed(e, pkg, actions, state);

	MemberView member = (MemberView)actions.get(e.getSource());
	String cmd = e.getActionCommand();
		
	if(member != null) {
	    if(state != S_NORMAL) {
		Debug.reportError("Can't instantiate modified class");
		return;
	    }
			
	    ResultWatcher watcher = null;

	    // if we are constructing an object, create a watcher that waits for
	    // completion of the call and then places the object on the object
	    // bench

	    if(member instanceof ConstructorView)
		watcher = new ResultWatcher() {
		    public void putResult(DebuggerObject result, String name) {
			if((name == null) || (name.length() == 0))
			name = "result";
			if(result != null) {
			    ObjectWrapper wrapper = 
			    new ObjectWrapper(result.getInstanceFieldObject(0),
					      name, pkg);
			    pkg.getFrame().getObjectBench().add(wrapper);
			}
			else
			Debug.reportError("cannot get execution result");
		    }
		};

	    // if we are calling a method that has a result, create a watcher
	    // that waits for completion of the call and then displays the
	    // result

	    else if(!((MethodView)member).isVoid())
		watcher = new ResultWatcher() {
		    public void putResult(DebuggerObject result, String name) {
			ObjectViewer viewer = 
		        ObjectViewer.getViewer(false, result, name, pkg, true,
					       pkg.getFrame());
		    }
		};	

	    // create an Invoker to handle the actual invocation

	    new Invoker(pkg, (CallableView)member, null, watcher);
	}
	else if(editStr.equals(cmd)) {
	    displayedView = Editor.IMPLEMENTATION;
	    open();
	}
	else if(publicStr.equals(cmd)) {
	    showView(Editor.PUBLIC);
	}
	else if(pkgStr.equals(cmd)) {
	    showView(Editor.PACKAGE);
	}
	else if(inheritedStr.equals(cmd)) {
	    showView(Editor.INHERITED);
	}
	else if(compileStr.equals(cmd)) {
	    pkg.compile(this);
	}
	else if(removeStr.equals(cmd)) {
	    try {
		((PkgMgrFrame)pkg.getFrame()).removeClass(this);
	    } catch (ClassCastException cce) {
		System.err.println("Invalid cast to JFrame in ClassTarget");
	    }
	}
	else
	  // if not handled send to class role for consumption
	  role.actionPerformed(e, this);

    }

    int anchor_x = 0, anchor_y = 0;
    int last_x = 0, last_y = 0;

    public void mousePressed(MouseEvent evt, int x, int y, GraphEditor editor)
    {
	super.mousePressed(evt, x, y, editor);

	anchor_x = last_x = x;
	anchor_y = last_y = y;
    }
    
    public void singleClick(MouseEvent evt, int x, int y, GraphEditor editor)
    {
    }
	
    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor)
    {
	    open();
    }

    public void mouseDragged(MouseEvent evt, int x, int y, GraphEditor editor)
    {	
	if ((pkg.getState() == Package.S_CHOOSE_USES_TO) ||
	    (pkg.getState() == Package.S_CHOOSE_EXT_TO) ) {	
	    // Draw a line from this Target to the current Cursor position
	    Graphics g = editor.getGraphics();
	    g.setColor(colBorder);
	    g.setXORMode(graphbg);
	    g.drawLine( anchor_x , anchor_y , last_x , last_y );
	    g.drawLine( anchor_x , anchor_y , x , y );
	    last_x = x;
	    last_y = y;
	}
	else
	    super.mouseDragged(evt, x, y, editor);
    }
	
    public void mouseMoved(MouseEvent evt, int x, int y, GraphEditor editor)
    {	
	if (pkg.getState() != Package.S_IDLE)
	    {	
		// Draw a line from this Target to the current Cursor position
		Graphics g = editor.getGraphics();
		g.setColor(colBorder);
		g.setXORMode(graphbg);
		g.drawLine( anchor_x , anchor_y , last_x , last_y );
		g.drawLine( anchor_x , anchor_y , x , y );
		last_x = x;
		last_y = y;
	    }
	else
	    super.mouseMoved(evt, x, y, editor);
    }
	
    public void showView(int viewType)
    {
	if(viewType == displayedView)
	    open();
	else
	    showView(getEditor(), viewType);
    }
	
    public void showView(Editor editor, int viewType)
    {
	if(editor==null)
	    return;

	displayedView = viewType;
	editor.setReadOnly(false);

	if(viewType == Editor.IMPLEMENTATION)
	    reopen();
	else {
	    editor.clear();
//  	    Class cl = pkg.loadClass(fullname);   // -as- 11/99
	    Class cl = pkg.loadClass(name);
	    if(cl != null) {
		View view = View.getView(cl);
		int filterType = 0;
		if(viewType == Editor.PUBLIC)
		    filterType = ViewFilter.PUBLIC;
		else if(viewType == Editor.PACKAGE)
		    filterType = ViewFilter.PACKAGE;
		else if(viewType == Editor.INHERITED)
		    filterType = ViewFilter.PROTECTED;
		
		ViewFilter filter= (filterType != 0) ? new ViewFilter(filterType) : null;
		view.print(new EditorPrintWriter(editor), filter);
	    }
	    editor.setReadOnly(true);
	    setState(S_NORMAL);
	    editor.show(viewType);
	}
    }

    /**
     * 
     * Prepares this ClassTarget for removal from a Package.  
     * It removes dependency arrows and calls prepareFilesForRemoval() 
     * to remove applicable files.
     *
     */
    public void prepareForRemoval()
    {
	// flag dependent Targets as invalid
	//invalidate();

	removeAllInDependencies();
	removeAllOutDependencies();

	// remove associated files (.class, .java and .ctxt)
	prepareFilesForRemoval();
    }

    /**
     * 
     * Removes applicable files (.class, .java and .ctxt) prior to 
     * this ClassTarget being removed from a Package.
     *
     */
    public void prepareFilesForRemoval()
    {
	// delegated to role object
	role.prepareFilesForRemoval(sourceFile(), classFile(), contextFile());
    }


	
    // Internal strings
	
    static String editStr = Config.getString("pkgmgr.classmenu.edit");
    static String openStr = Config.getString("browser.classchooser.classmenu.open");
    static String useStr = Config.getString("browser.classchooser.classmenu.use");
    static String publicStr = Config.getString("pkgmgr.classmenu.public");
    static String pkgStr = Config.getString("pkgmgr.classmenu.package");
    static String inheritedStr = Config.getString("pkgmgr.classmenu.inherited");
    static String compileStr = Config.getString("pkgmgr.classmenu.compile");
    static String removeStr = Config.getString("pkgmgr.classmenu.remove");

    private boolean libraryTarget = false;

    public boolean isLibrary() {
	return libraryTarget;
    }
    
    public void setLibraryTarget(boolean libraryTarget) {
	this.libraryTarget = libraryTarget;
    }
}
