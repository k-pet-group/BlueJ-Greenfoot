package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.BlueJFileReader;
import bluej.utility.DialogManager;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerClassLoader;
import bluej.debugger.Invoker;
import bluej.debugger.ObjectViewer;
import bluej.debugger.ResultWatcher;
import bluej.debugger.ObjectWrapper;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.editor.Editor;
import bluej.graph.GraphEditor;
import bluej.utility.*;
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
 * A class target in a package, i.e. a target that is a class file
 * built from Java source code
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 *
 * @version $Id: ClassTarget.java 522 2000-06-01 02:34:58Z bquig $
 */
public class ClassTarget extends EditableTarget
	implements ActionListener
{
    // Define Background Colours
    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color librarybg = Config.getItemColour("colour.class.bg.imported");
    static final Color abstractbg = Config.getItemColour("colour.class.bg.abstract");
    static final Color interfacebg = Config.getItemColour("colour.class.bg.interface");
    static final Color compbg = Config.getItemColour("colour.target.bg.compiling");
    static final Color umldefaultbg = Config.getItemColour("colour.class.bg.uml.default");
    static final Color umlcompbg = Config.getItemColour("colour.target.bg.uml.compiling");
    static final Color umlShadowCol = Config.getItemColour("colour.target.uml.shadow");

    static final Color colBorder = Config.getItemColour("colour.target.border");
    static final Color graphbg = Config.getItemColour("colour.graph.background");
    static final Color textfg = Config.getItemColour("colour.text.fg");

    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    static String usesArrowMsg = Config.getString("pkgmgr.usesArrowMsg");

    static final Image brokenImage = Toolkit.getDefaultToolkit().getImage(
                                                                          Config.getImageFilename("image.broken"));

    static final String STEREOTYPE_OPEN = "<<";
    static final String STEREOTYPE_CLOSE = ">>";
    static final String INTERFACE_LABEL = "interface";
    static final String APPLET_LABEL = "applet";


    // variables
    private ClassRole role;

    protected int modifiers;
    protected Vector breakpoints = new Vector();
    protected int displayedView = Editor.IMPLEMENTATION;
    protected SourceInfo sourceInfo = new SourceInfo();

    // Fields used in Tarjan's algorithm:
    public int dfn, link;

    private String stereotype;

    /**
     * Create a new class target in package 'pkg'.
     */
    public ClassTarget(Package pkg, String baseName)
    {
        this(pkg, baseName, false);
    }

    /**
     * Create a new class target in package 'pkg'.
     */
    public ClassTarget(Package pkg, String baseName, boolean isApplet)
    {
        super(pkg, baseName);

        if(isApplet)
            role = new AppletClassRole();
        else
            role = new StdClassRole();

    }

    /**
     * Return the target's name, including the package name.
     * eg. bluej.pkgmgr.Target
     */
    public String getQualifiedName()
    {
        return getPackage().getQualifiedName(getBaseName());
    }

    /**
     * Return the target's base name (ie the name without the package name).
     * eg. Target
     */
    public String getBaseName()
    {
        return getIdentifierName();
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

        if (!BlueJFileReader.copyFile(getSourceFile(),
                                      new File(directory, getBaseName() + ".java")))
            okay = false;

        if(upToDate()) {
            if(!BlueJFileReader.copyFile(getClassFile(),
                                         new File(directory, getBaseName() + ".class")))
                okay = false;
            if(!BlueJFileReader.copyFile(getContextFile(),
                                         new File(directory, getBaseName() + ".ctxt")))
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
            File src = getSourceFile();
            File clss = getClassFile();

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
        if(isInterface) {
            modifiers |= Modifier.INTERFACE;
            setStereotypeName(INTERFACE_LABEL);
        }
        else {
            modifiers &= ~Modifier.INTERFACE;
            if(INTERFACE_LABEL.equals(stereotype))
               stereotype = null;
        }
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
       if(PrefMgr.isUML())
          return umldefaultbg; //bq add def for own colour
        else if(isInterface())
            return interfacebg;
        else if(isAbstract())
            return abstractbg;
        else
            return defaultbg;
    }

    // --- Target interface ---

    Color getBackgroundColour()
    {
        if(state == S_COMPILING) {
            return compbg;
        }
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
        return PrefMgr.getStandardFont();
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
    public File getSourceFile()
    {
        return new File(getPackage().getPath(), getBaseName() + ".java");
    }

    /**
     * @return the name of the context(.ctxt) file this target corresponds to.
     */
    public File getContextFile()
    {
        return new File(getPackage().getPath(), getBaseName() + ".ctxt");
    }

    /**
     * @return the name of the class (.class) file this target corresponds to.
     */
    public File getClassFile()
    {
        return new File(getPackage().getPath(), getBaseName() + ".class");
    }


    /**
     ** @return the editor object associated with this target. May be null
     **  if there was a problem opening this editor.
     **/
    public Editor getEditor()
    {
        if(editor == null)
            editor = getPackage().editorManager.openClass(getSourceFile().getPath(), getBaseName(), this,
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

        sourceInfo.setSourceModified();
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
            DebuggerClassLoader loader = getPackage().getRemoteClassLoader();
            return Debugger.debugger.toggleBreakpoint(getQualifiedName(), lineNo, set,
                                                      loader);
        }
        else
            return Config.getString("pkgmgr.breakpointMsg");
    }

    /**
     * Called by Editor to change the view displayed by an editor
     * @param viewname	the name of the view to display, should be
     * 		one of bluej.editor.Editor.PUBLIC, etc.
     * @return a boolean indicating if the change was allowed
     */
    public boolean changeView(Editor editor, int viewType)
    {
        generateView(editor, viewType);
        return true;
    }


    public void compile(Editor editor)
    {
        getPackage().compile(this);
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



    /**
     * generates a source code skeleton for this class
     *
     */
    public void generateSkeleton()
    {
        // delegate to role object
        role.generateSkeleton(getPackage(), getBaseName(), getSourceFile().getPath(),
                                isAbstract(), isInterface());
        // do we need to check whether skeleton generated before setting state?
        setState(Target.S_INVALID);
    }

    public static void enforcePackage(String sourceFileName, String packageName) throws IOException
    {
        ClassInfo info;

        packageName = packageName.trim();

        try {
            info = ClassParser.parse(sourceFileName);
        }
        catch(Exception e) {
            return;
        }

        if (info == null) {
            return;
        }

        int fourCases = 0;

        // there are four possible combinations of
        // packageName.length == 0 and
        // info.hasPackageStatement

        if (packageName.length() == 0) {
            if (info.hasPackageStatement()) {
                // we must delete all parts of the "package" statement
                fourCases = 1;
            }
            else {
                // if we have no package statement we do not need
                // to do anything to turn it into an anonymous package
                return;
            }
        }
        else {
            if (info.hasPackageStatement()) {
                // it is trivial to make the package name the same
                if (info.getPackage() == packageName)
                    return;
                // we must change just the package name
                fourCases = 3;
            }
            else {
                // we must insert all the "package" statement
                fourCases = 4;
            }
        }

        FileEditor fed = new FileEditor(new File(sourceFileName));

        if (fourCases == 1 || fourCases == 4) {
            Selection selSemi = info.getPackageSemiSelection();

            if (fourCases == 1) {
                fed.replaceSelection(selSemi, "");
            }
            else
                fed.replaceSelection(selSemi, ";");
        }

        Selection selName = info.getPackageNameSelection();

        if (fourCases == 1)
            fed.replaceSelection(selName, "");
        else
            fed.replaceSelection(selName, packageName);

        if (fourCases == 1 || fourCases == 4) {

            Selection selStatement = info.getPackageStatementSelection();

            if (fourCases == 1)
                fed.replaceSelection(selStatement, "");
            else
                fed.replaceSelection(selStatement, "package ");
        }

        fed.save();
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

        ClassInfo info = sourceInfo.getInfo(getSourceFile().getPath(),
                                            getPackage().getAllClassnames());

        // info will be null if the source was unparseable
        if(info == null) {
            getPackage().repaint();
            return;
        }

        if(checkName(info))
            return;

        if(info.isApplet()) {
            if( ! (role instanceof AppletClassRole))
                role = new AppletClassRole();
            stereotype = APPLET_LABEL;
        }
        else {
            if( ! (role instanceof StdClassRole))
                role = new StdClassRole();
            stereotype = null;
        }

        setInterface(info.isInterface());
        setAbstract(info.isAbstract());

        // handle superclass

        if(info.getSuperclass() != null) {
            Target superclass = getPackage().getTarget(info.getSuperclass());
            if (superclass != null)
                getPackage().addDependency(
                                  new ExtendsDependency(getPackage(), this, superclass),
                                  false);
        }

        // handle implemented interfaces

        Vector vect = info.getImplements();
        for(Enumeration e = vect.elements(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            Target interfce = getPackage().getTarget(name);
            // Debug.message("Implements " + name);
            if (interfce != null) {
                getPackage().addDependency(
                                  new ImplementsDependency(getPackage(), this, interfce),
                                  false);
            }
        }
        // handle used classes

        vect = info.getUsed();
        for(Enumeration e = vect.elements(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            Target used = getPackage().getTarget(name);
            if (used != null)
                getPackage().addDependency(new UsesDependency(getPackage(), this, used), true);
        }

        checkForUsesInconsistencies();

        getPackage().repaint();
    }

    private void checkForUsesInconsistencies()
    {
        for(int i = 0; i < outUses.size(); i++) {
            UsesDependency usesDep = ((UsesDependency)outUses.elementAt(i));
            if(! usesDep.isFlagged())
                getPackage().setStatus(usesArrowMsg + usesDep);
        }
    }


    /**
     * Check to see that name has not changed.
     * If name has changed then update details.
     * Return true if the name has changed.
     */
    private boolean checkName(ClassInfo info)
    {
        String newName = info.getName();
        Package myPkg = getPackage();

        if(!getBaseName().equals(newName)) {
            //need to check that class does not already exist
            if(getPackage().getTarget(newName) != null) {
                getPackage().showError("duplicate-name");
                return false;
            }

            File newSourceFile = new File(myPkg.getPath(), newName + ".java");
            File oldSourceFile = getSourceFile();

            if(BlueJFileReader.copyFile(oldSourceFile, newSourceFile)) {

                ClassTarget newTarget = new ClassTarget(myPkg, newName);
                newTarget.setPos(this.x, this.y);

/*  XXX              getPackage().updateTargetIdentifier(this, info.getName());
                getEditor().changeName(info.getName(), newSourceFileName);
                role.prepareFilesForRemoval(oldSourceFileName, getClassFile().getPath(), getContextFile().getPath());
                name = info.getName(); */

                myPkg.removeClass(this);
                myPkg.addTarget(newTarget);

                newTarget.analyseDependencies();

                return true;
            }
        }

        return false;
    }


    protected Class last_class = null;
    protected JPopupMenu menu = null;
    boolean compiledMenu = false;

    public void popupMenu(MouseEvent evt, int x, int y, GraphEditor editor)
    {
        if (state == S_NORMAL) {
            Class cl = getPackage().loadClass(getQualifiedName());
            if ((cl != null) && (last_class != cl)) {
                if (menu != null)
                    editor.remove(menu);
                menu = createMenu(cl);
                editor.add(menu);
                compiledMenu = true;
            }
            last_class = cl;
        }
        else {
            if (compiledMenu || menu == null) {
                menu = createMenu(null);
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
     * @param   cl  class object associated with this class target
     * @return      the created popup menu object
     */
    protected JPopupMenu createMenu(Class cl)
    {
        actions = new Hashtable();

        JPopupMenu menu = new JPopupMenu(getBaseName() + " operations");

    	// call on role object to add any options needed
     	role.createMenu(menu, this, state);

    	if ((cl != null)) // && (!isAbstract()))
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
    	item.setFont(PrefMgr.getStandardMenuFont());
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
        ViewFilter filter;
        View view = View.getView(cl);

        if(!isAbstract()) {
            filter = new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PACKAGE);
            ConstructorView[] constructors = view.getConstructors();

            if (createMenuItems(menu, constructors, filter, 0, constructors.length, "new "))
                menu.addSeparator();
        }

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

                Action callAction = new CallAction(prefix + m.getShortDesc(),
                                                    this, m);

                item = menu.add(callAction);
                item.setFont(PrefMgr.getStandardMenuFont());
                hasEntries = true;
            } catch(Exception e) {
                Debug.reportError("Exception accessing methods: " + e);
                e.printStackTrace();
            }
        }
        return hasEntries;
    }

    private class CallAction extends AbstractAction
    {
        private CallableView cv;
        private Target t;

        public CallAction(String menu, Target t, CallableView cv)
        {
            super(menu);
            this.cv = cv;
            this.t = t;
        }

        public void actionPerformed(ActionEvent e)
        {
            if(state != S_NORMAL) {
                Debug.reportError("Can't instantiate modified class");
                return;
            }
            getPackage().getEditor().raiseMethodCallEvent(t, cv);
        }
    }

    /**
     *  Draw this target, redefined from Target.
     */
    public void draw(Graphics2D g)
    {
        super.draw(g);

      //   if(state != S_NORMAL) {
//             // Debug.message("Target: drawing invalid target " + this);
//             g.setColor(compbg); 

//             int divider = 0;

//             // set divider if UML, different position if stereotype is present
//             if(PrefMgr.isUML())
//                 divider = (stereotype == null) ? 18 : 32; 

//             Utility.stripeRect(g, 0, divider, width, height - divider, 8, 3);
//         }



        if(PrefMgr.isUML())
            drawUMLStyle(g);
        else
            drawBlueStyle(g);

        g.setColor(getBorderColour());
        drawBorders(g);
        
        if(!sourceInfo.isValid())
            g.drawImage(brokenImage, x + TEXT_BORDER, y + height - 22, null);

        // delegate extra functionality to role object
        role.draw(g, this, x, y, width, height);
    }

    private void drawUMLStyle(Graphics2D g)
    {
        if(state != S_NORMAL) {
            g.setColor(umlShadowCol); 
            // set divider if UML, different position if stereotype is present
            int divider = (stereotype == null) ? 18 : 32; 
            Utility.stripeRect(g, 0, divider, width, height - divider, 8, 3);
        }

        g.setColor(getTextColour());
        
        int currentY = 2;
        // draw stereotype if applicable
        Font original = getFont();
        if(stereotype != null) {
            String stereotypeLabel = STEREOTYPE_OPEN + stereotype + STEREOTYPE_CLOSE;
            Font stereotypeFont = original.deriveFont((float)(original.getSize() - 2));
            g.setFont(stereotypeFont);
            Utility.drawCentredText(g, stereotypeLabel,
                                    TEXT_BORDER, currentY,
                                    width - 2 * TEXT_BORDER, TEXT_HEIGHT);
            currentY += TEXT_HEIGHT -2;
        }
        g.setFont(original);
        
        Utility.drawCentredText(g, getIdentifierName(),
                                TEXT_BORDER, currentY,
                                width - 2 * TEXT_BORDER, TEXT_HEIGHT);
        currentY += ( TEXT_HEIGHT);
        g.drawLine(0, currentY, width, currentY);
    }

    private void drawBlueStyle(Graphics2D g)
    {
        if(state != S_NORMAL) {
            g.setColor(shadowCol); 
            Utility.stripeRect(g, 0, 0, width, height, 8, 3);
        }

        g.setColor(textbg);
        g.fillRect(TEXT_BORDER, TEXT_BORDER,
                   width - 2 * TEXT_BORDER, TEXT_HEIGHT);

        g.setColor(getBorderColour());
        g.drawRect(TEXT_BORDER, TEXT_BORDER,
                   width - 2 * TEXT_BORDER, TEXT_HEIGHT);

        g.setColor(getTextColour());
        g.setFont(getFont());
        Utility.drawCentredText(g, getIdentifierName(),
                                TEXT_BORDER, TEXT_BORDER,
                                width - 2 * TEXT_BORDER, TEXT_HEIGHT);
    }

   void drawShadow(Graphics2D g)
    {
        g.fillRect(SHAD_SIZE, height, width, SHAD_SIZE);
        g.fillRect(width, SHAD_SIZE, SHAD_SIZE, height);
        if(!PrefMgr.isUML())
            Utility.drawThickLine(g, width - HANDLE_SIZE, height,
                                  width, height - HANDLE_SIZE, 3);
    }

    void drawBorders(Graphics2D g)
    {
        
        int thickness = ((flags & F_SELECTED) == 0) ? 1 : 4;
        Utility.drawThickRect(g, 0, 0, width, height, thickness);
        if(PrefMgr.isUML()) 
            if((flags & F_SELECTED) == 0)
                return;
        // Draw lines showing resize tag
        g.drawLine(width - HANDLE_SIZE - 2, height,
                   width, height - HANDLE_SIZE - 2);
        g.drawLine(width - HANDLE_SIZE + 2, height,
                   width, height - HANDLE_SIZE + 2);
    }

    /**
     * Set a sterotype name eg. applet or Interface
     */
    public void setStereotypeName(String stereotypeName)
    {
        stereotype = stereotypeName;
    }


    // -- ActionListener interface --

    public void actionPerformed(ActionEvent e)
    {
        //role.actionPerformed(e, pkg, actions, state);

        MemberView member = (MemberView)actions.get(e.getSource());
        String cmd = e.getActionCommand();

        if(member != null) {
        }
        else if(editStr.equals(cmd)) {
            showView(Editor.IMPLEMENTATION);
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
            getPackage().compile(this);
        }
        else if(removeStr.equals(cmd)) {
            getPackage().getEditor().raiseRemoveTargetEvent(this);
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
        if ((getPackage().getState() == Package.S_CHOOSE_USES_TO) ||
            (getPackage().getState() == Package.S_CHOOSE_EXT_TO) ) {
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
        if (getPackage().getState() != Package.S_IDLE)
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

    /*
     */
    public void showView(int viewType)
    {
        if(viewType == displayedView)
            open();
        else {
            Editor editor = getEditor();
            editor.setView(viewType);
            editor.setVisible(true);
        }
    }

    /**
     * Show a view in the editor. If 'open' is true, the editor needs to be
     * opened, otherwise it is already open and we only need to create the
     * content.
     */
    private void generateView(Editor editor, int viewType)
    {
        if(editor==null)
            return;

        displayedView = viewType;
        editor.setReadOnly(false);

        if(viewType == Editor.IMPLEMENTATION)
            reopen();
        else {
            editor.clear();
            Class cl = getPackage().loadClass(getQualifiedName());
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
            editor.setSelection(1, 1, 0);  // move cursor to top of editor
        }
    }

    /**
     * Prepares this ClassTarget for removal from a Package.
     * It removes dependency arrows and calls prepareFilesForRemoval()
     * to remove applicable files.
     *
     */
    public void prepareForRemoval()
    {
        // flag dependent Targets as invalid
        //invalidate();
        getEditor().close();

        removeAllInDependencies();
        removeAllOutDependencies();

        // remove associated files (.class, .java and .ctxt)
        prepareFilesForRemoval();
    }

    /**
     *
     * Removes applicable files (.class, .java and .ctxt) prior to
     * this ClassTarget being removed from a Package.
     */
    public void prepareFilesForRemoval()
    {
        // delegated to role object
        role.prepareFilesForRemoval(getSourceFile().getPath(),
                                    getClassFile().getPath(),
                                    getContextFile().getPath());
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
}
