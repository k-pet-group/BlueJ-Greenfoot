package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.BlueJFileReader;
import bluej.utility.DialogManager;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClassLoader;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;
import bluej.editor.Editor;
import bluej.graph.GraphEditor;
import bluej.utility.*;
import bluej.views.ConstructorView;
import bluej.views.MemberView;
import bluej.views.CallableView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;
//import bluej.tester.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.applet.Applet;

/*import net.sourceforge.transmogrify.hook.bluej.BlueJHook;
import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.hook.Transmogrifier;
import net.sourceforge.transmogrify.refactorer.*;
import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;*/

/**
 * A class target in a package, i.e. a target that is a class file
 * built from Java source code
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 *
 * @version $Id: ClassTarget.java 1417 2002-10-18 07:56:39Z mik $
 */
public class ClassTarget extends EditableTarget
	implements ActionListener
{
    // Define Background Colours
    static final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    static final Color abstractbg = Config.getItemColour("colour.class.bg.abstract");
    static final Color interfacebg = Config.getItemColour("colour.class.bg.interface");
    static final Color compbg = Config.getItemColour("colour.target.bg.compiling");

    static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    static final Color stripeCol = Config.getItemColour("colour.target.stripes");

    static final Color colBorder = Config.getItemColour("colour.target.border");
    static final Color graphbg = Config.getItemColour("colour.graph.background");
    static final Color textfg = Config.getItemColour("colour.text.fg");

    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    static String usesArrowMsg = Config.getString("pkgmgr.usesArrowMsg");

    static final Image brokenImage =
	    Config.getImageAsIcon("image.broken").getImage();

    static final String STEREOTYPE_OPEN = "<<";
    static final String STEREOTYPE_CLOSE = ">>";
    static final String INTERFACE_LABEL = "interface";
    static final String APPLET_LABEL = "applet";
    static final String ABSTRACT_CLASS_LABEL = "abstract";
    static final String UNITTEST_LABEL = "unit test";
    static final String HTML_EXTENSION = ".html";


    // variables
    private ClassRole role;
    private String template = null;

    protected int modifiers;
    protected List breakpoints = new ArrayList();
    protected SourceInfo sourceInfo = new SourceInfo();

    // Fields used in Tarjan's algorithm:
    public int dfn, link;

    private boolean analysing = false;	// flag to prevent recursive
                                        // calls to analyseDependancies()

    /**
     * Create a new class target in package 'pkg'.
     */
    public ClassTarget(Package pkg, String baseName)
    {
        this(pkg, baseName, null);
    }

    /**
     * Create a new class target in package 'pkg'.
     */
    public ClassTarget(Package pkg, String baseName, String template)
    {
        super(pkg, baseName);

        boolean isApplet = (template!=null) && (template.startsWith("applet"));
//        boolean isUnitTest = (template!=null) && (template.startsWith("unittest"));

        boolean isAbstract = (template!=null) &&
                             (template.startsWith("abstract"));
        boolean isInterface = (template!=null) &&
                              (template.startsWith("interface"));
        if(isApplet)
            role = new AppletClassRole();
//        else if (isUnitTest)
//            role = new UnitTestClassRole();
        else
            role = new StdClassRole();

        setInterface(isInterface);
        setAbstract(isAbstract);
        this.template = template;
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
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        super.load(props, prefix);

        // check type in case it is an Applet or UnitTest.  Mainly useful in case
        // it inherits from Applet or UnitTest further up the inheritance tree
        String type = props.getProperty(prefix + ".type");
        if("AppletTarget".equals(type) && (!(role instanceof AppletClassRole)))
            role = new AppletClassRole();
//        if("UnitTestTarget".equals(type) && (!(role instanceof UnitTestClassRole)))
//            role = new UnitTestClassRole();

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

        if (!FileUtility.copyFile(getSourceFile(),
                              new File(directory, getBaseName() + ".java")))
            okay = false;

        if(upToDate()) {
            if(!FileUtility.copyFile(getClassFile(),
                              new File(directory, getBaseName() + ".class")))
                okay = false;
            if(!FileUtility.copyFile(getContextFile(),
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

        for(Iterator it = dependents(); it.hasNext(); ) {
            Dependency d = (Dependency)it.next();
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
        }
        else {
            modifiers &= ~Modifier.INTERFACE;
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
        if(isAbstract) {
            modifiers |= Modifier.ABSTRACT;
        }
        else
            modifiers &= ~Modifier.ABSTRACT;
    }

    /**
     * verify whether this class target is an Applet
     * @return true if class target is an Applet (or subclass), else returns false
     */
    public boolean isApplet()
    {
        ClassInfo classInfo = sourceInfo.getInfoIfAvailable();
        if(!(role instanceof AppletClassRole) && ((classInfo != null) && classInfo.isApplet()))
            role = new AppletClassRole();
        return (role instanceof AppletClassRole);
    }

    /**
     * Verify whether this class target is a UnitTest
     * @return true if class target is a UnitTest (or subclass), else returns false
     */
//    public boolean isUnitTest()
//    {
//        ClassInfo classInfo = sourceInfo.getInfoIfAvailable();
//        if(!(role instanceof UnitTestClassRole) && ((classInfo != null) && classInfo.isUnitTest()))
//            role = new UnitTestClassRole();
//        return (role instanceof UnitTestClassRole);
//    }

    Color getDefaultBackground()
    {
        if(isInterface())
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
        return PrefMgr.getTargetFont();
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
     * @return a FileFilter for all inner class files of this target
     */
    public FileFilter getInnerClassFiles()
    {
       return new InnerClassFileFilter();

    }

    class InnerClassFileFilter implements FileFilter
    {
        public boolean accept(File pathname)
        {
            return pathname.getName().startsWith(getBaseName() + "$");
        }
    }

    /**
     * @return the editor object associated with this target. May be null
     *  if there was a problem opening this editor.
     */
    public Editor getEditor()
    {
        if(editor == null)
            editor = getPackage().editorManager.openClass(
                                     getSourceFile().getPath(), getBaseName(),
                                     this, isCompiled(), breakpoints);
        return editor;
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
        analyseSource(true);
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

    public void compile(Editor editor)
    {
        getPackage().compile(this);
    }

/* ajp experiment
    public void refactorEvent(Editor editor, BlueJHook hook)
    {
       try {
            FileParser fileParser = new FileParser();

            fileParser.doFile( getPackage().getProject().getProjectDir() );

            TableMaker maker = new TableMaker( (SymTabAST)(fileParser.getTree()) );
            SymbolTable symbolTable = maker.getTable();

            Transmogrifier rf = new RenameVariable();
            rf.setup(symbolTable);

            if (rf.canApply(hook))
                rf.apply(hook);
            else
                System.out.println("can't refactor here");
        }
        catch (Exception e) {
          e.printStackTrace();
        }
    }
*/

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
     *  Called when this class target has just been successfully compiled.
     */
    public void endCompile()
    {
        Class cl = getPackage().loadClass(getQualifiedName());

        if (cl != null) {
            if (Applet.class.isAssignableFrom(cl)) {
                if( ! (role instanceof AppletClassRole))
                    role = new AppletClassRole();
            }
/*            else if (junit.framework.TestCase.class.isAssignableFrom(cl)) {
                if( ! (role instanceof UnitTestClassRole))
                    role = new UnitTestClassRole();
            } */
            else {
                if( ! (role instanceof StdClassRole)) {
                    role = new StdClassRole();
                }
            }
        }
    }

    public void runApplet(PkgMgrFrame parent)
    {
        if (role instanceof AppletClassRole) {
            AppletClassRole acr = (AppletClassRole) role;
            acr.runApplet(parent, this);
        }
    }

    /**
     * generates a source code skeleton for this class
     *
     */
    public void generateSkeleton()
    {
        // delegate to role object
        if(template == null)
            Debug.reportError("generate class skeleton error");
        else {
            role.generateSkeleton(template, getPackage(), getBaseName(),
                                  getSourceFile().getPath());
            setState(Target.S_INVALID);
        }
    }

    public void enforcePackage(String packageName) throws IOException
    {
        ClassInfo info;

        if(!JavaNames.isQualifiedIdentifier(packageName))
            throw new IllegalArgumentException();

        try {
            info = ClassParser.parse(getSourceFile());
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
                if (info.getPackage().equals(packageName))
                    return;
                // we must change just the package name
                fourCases = 3;
            }
            else {
                // we must insert all the "package" statement
                fourCases = 4;
            }
        }

        // this allows us to make the changes to the file
        FileEditor fed = new FileEditor(getSourceFile());

        // first delete or insert the semicolon
        if (fourCases == 1 || fourCases == 4) {
            Selection selSemi = info.getPackageSemiSelection();

            if (fourCases == 1) {
                fed.replaceSelection(selSemi, "");
            }
            else
                fed.replaceSelection(selSemi, ";\n\n");
        }

        // then delete or insert the package name
        Selection selName = info.getPackageNameSelection();

        if (fourCases == 1)
            fed.replaceSelection(selName, "");
        else
            fed.replaceSelection(selName, packageName);

        // finally delete or insert the package statement
        if (fourCases == 1 || fourCases == 4) {
            Selection selStatement = info.getPackageStatementSelection();

            if (fourCases == 1)
                fed.replaceSelection(selStatement, "");
            else
                fed.replaceSelection(selStatement, "package ");
        }

        // save changes back to disk
        fed.save();
    }

    /**
     * Analyse the source code.
     */
    public void analyseSource(boolean modifySource)
    {
        if(analysing)
            return;

        analysing = true;

        ClassInfo info = sourceInfo.getInfo(getSourceFile().getPath(),
                                            getPackage().getAllClassnames());

        // info will be null if the source was unparseable
        if(info != null) {

            // the following three may update the package display but they
            // will not modify the classes source code
            setInterface(info.isInterface());
            setAbstract(info.isAbstract());
            analyseDependencies(info);

            // these two however will potentially modify the source
            if (modifySource) {
                if(analyseClassName(info))
                    doClassNameChange(info.getName());
                if(analysePackageName(info))
                    doPackageNameChange(info.getPackage());
            }
        }

        getPackage().repaint();

        analysing = false;
    }

    public boolean analyseClassName(ClassInfo info)
    {
        String newName = info.getName();

        if ((newName == null) || (newName.length() == 0))
            return false;

        return (!getBaseName().equals(newName));
    }

    public boolean analysePackageName(ClassInfo info)
    {
        String newName = info.getPackage();

        return (!getPackage().getQualifiedName().equals(newName));
    }

    /**
     *  Analyse the current dependencies in the source code and update the
     *  dependencies in the graphical display accordingly.
     */
    public void analyseDependencies(ClassInfo info)
    {
    	// currently we don't remove uses dependencies, but just warn

    	//removeAllOutDependencies();
    	removeInheritDependencies();
    	unflagAllOutDependencies();

        // handle superclass dependency
        if(info.getSuperclass() != null) {
            DependentTarget superclass = (DependentTarget)getPackage().getTarget(info.getSuperclass());
            if (superclass != null)
                getPackage().addDependency(
                                  new ExtendsDependency(getPackage(), this, superclass),
                                  false);
        }

        // handle implemented interfaces
        List vect = info.getImplements();
        for(Iterator it = vect.iterator(); it.hasNext(); ) {
            String name = (String)it.next();
            DependentTarget interfce = getPackage().getDependentTarget(name);
            // Debug.message("Implements " + name);
            if (interfce != null) {
                getPackage().addDependency(
                                  new ImplementsDependency(getPackage(), this, interfce),
                                  false);
            }
        }

        // handle used classes
        vect = info.getUsed();
        for(Iterator it = vect.iterator(); it.hasNext(); ) {
            String name = (String)it.next();
            DependentTarget used = getPackage().getDependentTarget(name);
            if (used != null)
                getPackage().addDependency(new UsesDependency(getPackage(), this, used), true);
        }

        // check for inconsistent use dependencies
        for(int i = 0; i < outUses.size(); i++) {
            UsesDependency usesDep = ((UsesDependency)outUses.get(i));
            if(! usesDep.isFlagged())
                getPackage().setStatus(usesArrowMsg + usesDep);
        }
    }

    /**
     * Check to see that name has not changed.
     * If name has changed then update details.
     * Return true if the name has changed.
     */
    private boolean doClassNameChange(String newName)
    {
        //need to check that class does not already exist
        if(getPackage().getTarget(newName) != null) {
            getPackage().showError("duplicate-name");
            return false;
        }

        File newSourceFile = new File(getPackage().getPath(), newName + ".java");
        File oldSourceFile = getSourceFile();

        if(FileUtility.copyFile(oldSourceFile, newSourceFile)) {

            getPackage().updateTargetIdentifier(this, getIdentifierName(), newName);
            getEditor().changeName(newName, newSourceFile.getPath());

            role.prepareFilesForRemoval(oldSourceFile.getPath(),
                                         getClassFile().getPath(),
                                         getContextFile().getPath());

            // this is extremely dangerous code here.. must track all
            // variables which are set when ClassTarget is first
            // constructed and fix them up for new class name
            setIdentifierName(newName);
            setDisplayName(newName);

            return true;
        }

        return false;
    }

    private void doPackageNameChange(String newName)
    {
        Project proj = getPackage().getProject();
        Package dstPkg = proj.getPackage(newName);

        if(dstPkg == null) {
            DialogManager.showError(null, "package-name-invalid");
        }
        else {
            if(DialogManager.askQuestion(null, "package-name-changed") == 0) {
                switch(dstPkg.importFile(getSourceFile())) {
                default:
                    prepareForRemoval();
                    getPackage().removeTarget(this);
                    close();
                    return;
                }
            }
        }

        // all non working paths lead here.. lets fix the package line
        // up so it is back to what we expect

        try {
            enforcePackage(getPackage().getQualifiedName());
            getEditor().reloadFile();
        }
        catch(IOException ioe) { }
    }

    protected Class last_class = null;
    protected JPopupMenu menu = null;
    boolean compiledMenu = false;

    public void popupMenu(int x, int y, GraphEditor editor)
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
            menu.show(editor, x, y);
    }



    protected HashMap actions;

    /**
     * creates a popup menu for this class target.
     *
     * @param   cl  class object associated with this class target
     * @return      the created popup menu object
     */
    protected JPopupMenu createMenu(Class cl)
    {
        actions = new HashMap();

        JPopupMenu menu = new JPopupMenu(getBaseName() + " operations");

    	// call on role object to add any options needed
     	role.createMenu(menu, this, state);

    	if ((cl != null)) // && (!isUnitTest()))
    	    createClassMenu(menu, cl);

    	addMenuItem(menu, editStr, true);
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
    	item.setFont(PrefMgr.getPopupMenuFont());
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
                item.setFont(PrefMgr.getPopupMenuFont());
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

        drawUMLStyle(g);

        g.setColor(getBorderColour());
        drawBorders(g);

        if(!sourceInfo.isValid())
            g.drawImage(brokenImage, x + TEXT_BORDER, y + height - 22, null);

        // delegate extra functionality to role object
        role.draw(g, this, x, y, width, height);
    }


    /**
     * creates a stereotype name as a String if it is an interface,
     * abstract class or Applet.
     * @return String representing the type of stereotype or null if not applicable
     */
    public String getStereotype()
    {
        String type = null;

        if(isAbstract())
            type = ABSTRACT_CLASS_LABEL;
        else if(isInterface())
            type = INTERFACE_LABEL;
        else if(isApplet())
            type = APPLET_LABEL;
//        else if(isUnitTest())
//            type = UNITTEST_LABEL;

        return type;
    }

    /**
     * Draws UML specific parts of the representation of this ClassTarget.
     *
     */
    private void drawUMLStyle(Graphics2D g)
    {
        // call to getStereotype
        String stereotype = getStereotype();

        if(state != S_NORMAL) {
            g.setColor(stripeCol);
            int divider = (stereotype == null) ? 18 : 32;
            Utility.stripeRect(g, 0, divider, width, height - divider, 8, 3);
        }

        g.setColor(getTextColour());

        int currentY = 2;
        Font original = getFont();

        // draw stereotype if applicable
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


    /**
     * Redefinition of the method found in Target.
     * It draws a shadow around the ClassTarget
     */
   void drawShadow(Graphics2D g)
    {
        g.fillRect(SHAD_SIZE, height, width, SHAD_SIZE);
        g.fillRect(width, SHAD_SIZE, SHAD_SIZE, height);
    }

    /**
     * Redefinition of the method found in Target.
     * It draws a shadow around the ClassTarget
     */
    void drawBorders(Graphics2D g)
    {

        int thickness = ((flags & F_SELECTED) == 0) ? 1 : 4;
        Utility.drawThickRect(g, 0, 0, width, height, thickness);
        if((flags & F_SELECTED) == 0)
            return;
        // Draw lines showing resize tag
        g.drawLine(width - HANDLE_SIZE - 2, height,
                   width, height - HANDLE_SIZE - 2);
        g.drawLine(width - HANDLE_SIZE + 2, height,
                   width, height - HANDLE_SIZE + 2);
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
            open();
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

        if(editor != null)
            editor.close();

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
        if (getSourceFile().exists()) {
            // remove all inner class files starting with the same name as
            // sourceFile$
            File[] files = getPackage().getPath().listFiles(getInnerClassFiles());

            if (files != null) {
                for(int i=0; i<files.length; i++) {
                   files[i].delete();
                }
            }
        }

        role.prepareFilesForRemoval(getSourceFile().getPath(),
                                    getClassFile().getPath(),
                                    getContextFile().getPath());
    }


    // Internal strings

    static String editStr = Config.getString("pkgmgr.classmenu.edit");
    static String openStr = Config.getString("browser.classchooser.classmenu.open");
    static String useStr = Config.getString("browser.classchooser.classmenu.use");
    static String compileStr = Config.getString("pkgmgr.classmenu.compile");
    static String removeStr = Config.getString("pkgmgr.classmenu.remove");
}
