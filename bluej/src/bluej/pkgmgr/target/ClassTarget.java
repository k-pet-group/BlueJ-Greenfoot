
package bluej.pkgmgr.target;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

import javax.swing.*;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugmgr.inspector.*;
import bluej.editor.Editor;
import bluej.graph.GraphEditor;
import bluej.parser.ClassParser;
import bluej.parser.symtab.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.dependency.*;
import bluej.pkgmgr.target.role.*;
import bluej.prefmgr.PrefMgr;
import bluej.utility.*;
import bluej.extmgr.*;

/**
 * A class target in a package, i.e. a target that is a class file
 * built from Java source code
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 *
 * @version $Id: ClassTarget.java 2563 2004-05-28 12:30:41Z polle $
 */
public class ClassTarget extends EditableTarget
{	
    static final int MIN_WIDTH = 80;
	static final int MIN_HEIGHT = 50;
    private static final String editStr = Config.getString("pkgmgr.classmenu.edit");
    private static final String openStr = Config.getString("browser.classchooser.classmenu.open");
    private static final String useStr = Config.getString("browser.classchooser.classmenu.use");
    private static final String compileStr = Config.getString("pkgmgr.classmenu.compile");
    private static final String inspectStr = Config.getString("pkgmgr.classmenu.inspect");
    private static final String removeStr = Config.getString("pkgmgr.classmenu.remove");
	private static final String createTestStr = Config.getString("pkgmgr.classmenu.createTest");

    // Define Background Colours
    private static final Color compbg = Config.getItemColour("colour.target.bg.compiling");

    private static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    private static final Color stripeCol = Config.getItemColour("colour.target.stripes");

    private static final Color colBorder = Config.getItemColour("colour.target.border");
    private static final Color graphbg = Config.getItemColour("colour.graph.background");
    private static final Color textfg = Config.getItemColour("colour.text.fg");

    private static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private static String usesArrowMsg = Config.getString("pkgmgr.usesArrowMsg");

    private static final Image brokenImage =
        Config.getImageAsIcon("image.broken").getImage();

    private static final String STEREOTYPE_OPEN = "<<";
    private static final String STEREOTYPE_CLOSE = ">>";
    // temporary file name extension to trick windows if changing case only in class name
    private static String TEMP_FILE_EXTENSION = "-temp";

    // the role object represents the changing roles that are class
    // target can have ie changing from applet to an interface etc
    // 'role' should never be null
    // role should be accessed using getRole() and set using
    // setRole(). A role should not contain important state information
    // because role objects are thrown away at a whim.
    private ClassRole role = new StdClassRole();
    
    // a flag indicating whether an editor, when opened for the first
    // time, should display the interface of this class
    private boolean openWithInterface = false;

    // the set of breakpoints set in this class
    protected List breakpoints = new ArrayList();

    // cached information obtained by parsing the source code
    // automatically becomes invalidated when the source code is
    // edited
    private SourceInfo sourceInfo = new SourceInfo();

    // fields used in Tarjan's algorithm:
    public int dfn, link;

    // flag to prevent recursive calls to analyseDependancies()
    private boolean analysing = false;

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

        // we can take a guess at what the role is going to be for the
        // object based on the start of the template name. If we get this
        // wrong, its no great shame as it'll be fixed the first time they
        // successfully analyse/compile the source.
        if (template != null) {
            if (template.startsWith("applet"))
            role = new AppletClassRole();
            else if (template.startsWith("unittest"))
                role = new UnitTestClassRole();
            else if (template.startsWith("abstract"))
                role = new AbstractClassRole();
            else if (template.startsWith("interface"))
                role = new InterfaceClassRole();
            else if (template.startsWith("enum"))
                role = new EnumClassRole();
        }
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

    public SourceInfo getSourceInfo(){
        return sourceInfo;
    }
    
    /**
     * Returns the text which the target is displaying as its label. For normal
     * classes this is just the identifier name. For generic classes the generic
     * parameters are shown as well
     */
    public String getDisplayName() {
        return super.getDisplayName() + getTypeParameters();
    }
    
    /**
     * Returns the type parameters for a genric class as declared in the source file.  
     */
    private String getTypeParameters() {
        //TODO When the parser supports generics this should return something usefull
        return "";
    }

    /**
     * Change the state of this target. The target will be repainted to show
     * the new state.
     */
    public void setState(int newState)
    {	
     	Inspector.removeInstance(getQualifiedName());
    	super.setState(newState);
    }

    /**
     * Return the role object for this class target.
     */
    public ClassRole getRole()
    {
        return role;
    }

    /**
     * Set the role for this class target.
     *
     * Avoids changing over the role object if the new one is of the same
     * type
     */
    protected void setRole(ClassRole newRole)
    {
        if ((role == null) || !(newRole.getClass().equals(role.getClass())))
            role = newRole;
    }

    /**
     * Use a variety of tests to determine what our role is.
     *
     * All tests must be very quick and should not rely on any
     * significant computation (ie. reparsing). If computation is
     * required, the existing role will do for the time being.
     */
    public void determineRole(Class cl)
    {
        if (cl != null) {
            Class junitClass = null;
        	try {
				junitClass = cl.getClassLoader().loadClass("junit.framework.TestCase");
        	}
			catch (ClassNotFoundException cnfe) {
				junitClass = junit.framework.TestCase.class;
			}        	

			Class appletClass = null;
			try {
				appletClass = cl.getClassLoader().loadClass("java.applet.Applet");
			}
			catch (ClassNotFoundException cnfe) {
				appletClass = java.applet.Applet.class;
			}        	
		
			
			
            // if cl is non-null then it is the definitive information
            // source ie. if it thinks its an applet who are we to argue
            // with it.
            if (appletClass.isAssignableFrom(cl))
                setRole(new AppletClassRole());
            else if (junitClass.isAssignableFrom(cl))
                setRole(new UnitTestClassRole());
            else if (Modifier.isInterface(cl.getModifiers()))
                setRole(new InterfaceClassRole());
            else if (Modifier.isAbstract(cl.getModifiers())) 
                setRole(new AbstractClassRole());
            else if (Config.isJava15() && cl.isEnum())
                setRole(new EnumClassRole());            
        }
        else {
            // try the parsed source code
            ClassInfo classInfo = sourceInfo.getInfoIfAvailable();

            if (classInfo != null) {
                if (classInfo.isApplet())
                    setRole(new AppletClassRole());
                else if (classInfo.isUnitTest())
                    setRole(new UnitTestClassRole());
                else if (classInfo.isInterface())
                    setRole(new InterfaceClassRole());
                else if (classInfo.isAbstract())
                    setRole(new AbstractClassRole());
                //TODO: When ClassInfo implements isEnum() this should be added:
                // else if (Config.isJava15() && classInfo.isEnum())
                //    setRole(new EnumClassRole());
                else 
                    setRole(new StdClassRole()); 

            }
        }
        // everything failed, lets leave the role as it was
    }

    /**
     * Load existing information about this class target
     * @param props the properties object to read
     * @param prefix an internal name used for this target to identify
     * its properties in a properties file used by multiple targets.
     */
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        super.load(props, prefix);

        // try to determine if any role was set when we saved
        // the class target. Be careful here as if you add role types
        // you need to add them here as well.
        String type = props.getProperty(prefix + ".type");
        
        String intf = props.getProperty(prefix + ".showInterface");
        openWithInterface = Boolean.valueOf(intf).booleanValue();
        
        if (AppletClassRole.APPLET_ROLE_NAME.equals(type))
            setRole(new AppletClassRole());
        else if (UnitTestClassRole.UNITTEST_ROLE_NAME.equals(type))
            setRole(new UnitTestClassRole());
        else if (AbstractClassRole.ABSTRACT_ROLE_NAME.equals(type))
            setRole(new AbstractClassRole());
        else if (InterfaceClassRole.INTERFACE_ROLE_NAME.equals(type))
            setRole(new InterfaceClassRole());
        else if (EnumClassRole.ENUM_ROLE_NAME.equals(type))
            setRole(new EnumClassRole());


        getRole().load(props, prefix);
    }

    /**
     * Save information about this class target
     * @param props the properties object to save to
     * @param prefix an internal name used for this target to identify
     * its properties in a properties file used by multiple targets.
     */
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        if (getRole().getRoleName() != null) 
            props.put(prefix + ".type", getRole().getRoleName());

        if (editorOpen()) {
            openWithInterface = getEditor().isShowingInterface();
        }
        props.put(prefix + ".showInterface", 
                    new Boolean(openWithInterface).toString());

        getRole().save(props, 0, prefix);        
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
     * Check if the compiled class and the source are up to date.
     * @return true if they are in sync otherwise false.
     */
    public boolean upToDate()
    {
        // check if the class file is up to date
        File src = getSourceFile();
        File clss = getClassFile();

        // if just a .class file with no src, it better be up to date
		if(!hasSourceCode())
			return true;
				
        if(!clss.exists()
             || (src.exists() && (src.lastModified() > clss.lastModified())))
            return false;

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
     * Verify whether this class target is an interface class
     * @return true if class target is an interface class, else returns false
     */
    public boolean isInterface()
    {
        return (getRole() instanceof InterfaceClassRole);
    }

	/**
	 * Verify whether this class target is an unit test class
	 * @return true if class target is a unit test class, else returns false
	 */
	public boolean isUnitTest()
	{
		return (getRole() instanceof UnitTestClassRole);
	}

    // --- Target interface ---

    Color getBackgroundColour()
    {
        if(state == S_COMPILING) {
            return compbg;
        } else
            return getRole().getBackgroundColour();
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

    public boolean hasSourceCode()
    {
        return getSourceFile().canRead();
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
        if(editor == null){
            String filename = getSourceFile().getPath();
            String docFilename = getPackage().getProject().getDocumentationFile(filename);
            editor = Package.editorManager.openClass(filename, 
                    docFilename, 
                    getBaseName(), 
                    this, 
                    isCompiled(), 
                    breakpoints, 
                    getPackage().getProject().getLocalClassLoader() );
            if(openWithInterface)
                editor.showInterface(true);
        }
        return editor;
    }

    // --- end of EditableTarget interface ---

    // --- user interface function implementation ---

    /**
     *
     */
    private void inspect()
    {
    	try {
			DebuggerClass clss = getPackage().getDebugger().getClass(getQualifiedName());

			ClassInspector insp = 
				ClassInspector.getInstance(clss, getPackage(), PkgMgrFrame.findFrame(getPackage()));
    	}
		catch (ClassNotFoundException cnfe) { }
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
     * @param editor    the editor object being saved
     */
    public void saveEvent(Editor editor)
    {
        analyseSource(true);
        determineRole(null);
    }

    /**
     * Called by Editor when a breakpoint is been set/cleared
     * @param filename  the name of the file that was modified
     * @param lineNo    the line number of the breakpoint
     * @param set   whether the breakpoint is set (true) or cleared
     *
     * @return  null if there was no problem, or an error string
     */
    public String breakpointToggleEvent(Editor editor, int lineNo, boolean set)
    {
        if(isCompiled()) {
            return getPackage().getDebugger().toggleBreakpoint(getQualifiedName(), lineNo, set);
        }
        else
            return Config.getString("pkgmgr.breakpointMsg");
    }

    // --- end of EditorWatcher interface ---

    public void removeBreakpoints()
    {
        if(editor != null)
            editor.removeBreakpoints();
    }

    public void removeStepMark()
    {
        if(editor != null)
            editor.removeStepMark();
    }

    public boolean isCompiled()
    {
        return (state == S_NORMAL);
    }


    public void compile(Editor editor)
    {
        getPackage().compile(this);
    }

    /**
     *  Called when this class target has just been successfully compiled.
     *
     * We load the compiled class if possible and check it the compilation
     * has resulted in it taking a different role (ie abstract to applet)
     */
    public void endCompile()
    {
        Class cl = getPackage().loadClass(getQualifiedName());

        determineRole(cl);
    }

    /**
     * generates a source code skeleton for this class
     *
     */
    public void generateSkeleton(String template)
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
            // the following may update the package display but it
            // will not modify the classes source code
            determineRole(null);
            analyseDependencies(info);

            // these two however will potentially modify the source
            if (modifySource) {
                if(analyseClassName(info)) {
                    if(nameEqualsIgnoreCase(info.getName())) {
                        // this means file has same name but different case
                        // to trick Windows OS to do a name change we need to
                        // rename to temp name and then rename to desired name
                        doClassNameChange(info.getName()+ TEMP_FILE_EXTENSION);
                    }
                    doClassNameChange(info.getName());
                }
                if(analysePackageName(info))
                    doPackageNameChange(info.getPackage());
            }
        }

        getPackage().repaint();

        analysing = false;
    }

    /**
     * Analyses class name of Classtarget with that of parsed src file.
     * Aim is to detect any textual changes of class name and modify resources 
     * to suit
     * @param info contains parsed class information
     * @return true if class name is different
     */
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
            DependentTarget superclass = getPackage().getDependentTarget(info.getSuperclass());
            
            if (superclass != null) {
                getPackage().addDependency(
                                  new ExtendsDependency(getPackage(), this, superclass),
                                  false);
            }
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
            if (used != null) {
				if (used.getAssociation() == this || this.getAssociation() == used)
					continue;

				getPackage().addDependency(new UsesDependency(getPackage(), this, used), true);
            	
            }
        }

        // check for inconsistent use dependencies
        for(Iterator it = usesDependencies(); it.hasNext();) {
            UsesDependency usesDep = ((UsesDependency)it.next());
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

            role.prepareFilesForRemoval(this,
                                         oldSourceFile.getPath(),
                                         getClassFile().getPath(), getContextFile().getPath());

            // this is extremely dangerous code here.. must track all
            // variables which are set when ClassTarget is first
            // constructed and fix them up for new class name
            setIdentifierName(newName);
            setDisplayName(newName);

            return true;
        }
        return false;
    }
    
    /**
     * Checks for ClassTarget name equality if case is ignored.
     * @param newName 
     * @return true if name is equal ignoring case.
     */
    private boolean nameEqualsIgnoreCase(String newName)
    {
        return (getBaseName().equalsIgnoreCase(newName));      
    }

	/**
	 * Change the package of a class target to something else.
     *
	 * @param newName the new fully qualified package name
	 */
    private void doPackageNameChange(String newName)
    {
        Project proj = getPackage().getProject();

        // This should be a WIzard one, Damiano
        Package dstPkg = proj.getPackage(newName);

        if(dstPkg == null) {
            DialogManager.showError(null, "package-name-invalid");
        }
        else {
        	// fix for bug #382. Potentially could clash with a package
        	// in the destination package with the same name
        	if (dstPkg.getTarget(getBaseName()) != null) {
        		DialogManager.showError(null, "package-name-clash");
        		return;
        	}
        	
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

	/**
	 * Construct a popup menu for the class target, including caching
	 * of results.
	 */
    protected JPopupMenu menu = null;
    boolean compiledMenu = false;

    public void popupMenu(int x, int y, GraphEditor editor)
    {
        Class cl = null;

        if (state == S_NORMAL) {
            // handle error causes when loading 1.4 compiled classes
            // on a 1.3 VM
            // we detect the error, remove the class file, and invalidate
            // to allow them to be recompiled
            try {
                cl = getPackage().loadClass(getQualifiedName());
            }
            catch (LinkageError le) {
                Debug.message(le.toString());    

                // trouble loading the class
                // remove the class file and invalidate the target
                if (hasSourceCode()) {
                    getClassFile().delete();    
                    invalidate();
                }
                cl = null;               
            }
        }

		if (menu != null)
			 editor.remove(menu);

        // check that the class loading hasn't changed out state
        if (state == S_NORMAL) {
            menu = createMenu(cl);
            editor.add(menu);
        }
        else {
            menu = createMenu(null);
            editor.add(menu);
        }

        if (menu != null)
            menu.show(editor, x, y);
    }

    /**
     * Creates a popup menu for this class target.
     *
     * @param   cl  class object associated with this class target
     * @return      the created popup menu object
     */
    protected JPopupMenu createMenu(Class cl)
    {
        JPopupMenu menu = new JPopupMenu(getBaseName() + " operations");

        // call on role object to add any options needed at top
        role.createRoleMenu(menu, this, cl, state);

        if (cl != null)
            if (role.createClassConstructorMenu(menu, this, cl))
                menu.addSeparator();

        if (cl != null)
			if (role.createClassStaticMenu(menu, this, cl))
				menu.addSeparator();
        role.addMenuItem(menu, new EditAction(), true);
        role.addMenuItem(menu, new CompileAction(), hasSourceCode());
        role.addMenuItem(menu, new InspectAction(), cl != null);
        role.addMenuItem(menu, new RemoveAction(), true);

        // call on role object to add any options needed at bottom
        role.createRoleMenuEnd(menu, this, state);

        MenuManager menuManager = new MenuManager( menu );
        menuManager.setAttachedObject(this);
        menuManager.addExtensionMenu(getPackage().getProject());
        
        return menu;
    }

    public class CreateTestAction extends AbstractAction
    {
        public CreateTestAction()
        {
            putValue(NAME, createTestStr);
        }

        public void actionPerformed(ActionEvent e)
        {
			PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
			
			if (pmf != null) {
                String testClassName = getIdentifierName() + "Test";
				pmf.createNewClass(testClassName, "unittest");
                // we want to check that the previous called actually 
                // created a unit test class as a name clash with an existing
                // class would not.  This prevents a non unit test becoming
                // associated with a class unintentionally
                Target target = getPackage().getTarget(testClassName);
                ClassTarget ct = null;
                if(target instanceof ClassTarget) {
                    ct = (ClassTarget)target;
                    if(ct != null && ct.isUnitTest())
                        setAssociation(getPackage().getTarget(getIdentifierName() + "Test"));
                }
                endMove();
                getPackage().getEditor().revalidate();
                getPackage().getEditor().repaint();

			}
        }
    }

    private class EditAction extends AbstractAction
    {
        public EditAction()
        {
            putValue(NAME, editStr);
        }

        public void actionPerformed(ActionEvent e)
        {
            open();
        }
    }

    private class CompileAction extends AbstractAction
    {
        public CompileAction()
        {
            putValue(NAME, compileStr);
        }

        public void actionPerformed(ActionEvent e)
        {
            getPackage().compile(ClassTarget.this);
        }
    }

    private class RemoveAction extends AbstractAction
    {
        public RemoveAction()
        {
            putValue(NAME, removeStr);
        }

        public void actionPerformed(ActionEvent e)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(getPackage());
            if ( pmf.askRemoveClass() ){
                getPackage().getEditor().raiseRemoveTargetEvent(ClassTarget.this);
            }
        }
    }

    private class InspectAction extends AbstractAction
    {
        public InspectAction()
        {
            putValue(NAME, inspectStr);
        }

        public void actionPerformed(ActionEvent e)
        {
            inspect();
        }
    }

   
    public void mousePressed(MouseEvent evt, GraphEditor editor)
    {
        super.mousePressed(evt, editor);
    }

    public void singleClick(MouseEvent evt, GraphEditor editor)
    {
    }

    public void doubleClick(MouseEvent evt, GraphEditor editor)
    {
        open();
    }

    public void endMove()
    {
        super.endMove();
    	Target t = getAssociation();
    	
        if (t != null) {
            //TODO magic numbers. Should also take grid size in to account.
            t.setPos(getX() + 30, getY() - 30);
        }
    }

    /**
     * Prepares this ClassTarget for removal from a Package.
     * It removes dependency arrows and calls prepareFilesForRemoval()
     * to remove applicable files.
     *
     */
    private void prepareForRemoval()
    {
        if(editor != null)
            editor.close();

		// if this target is the assocation for another Target, remove
		// the association
		Iterator it = getPackage().getVertices();
		while(it.hasNext()) {
			Object o = it.next();
			if (o instanceof DependentTarget) {
				DependentTarget d = (DependentTarget) o;
				if (this.equals(d.getAssociation()))
					d.setAssociation(null);
			}
		}
		
		// flag dependent Targets as invalid
		invalidate();

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

        getRole().prepareFilesForRemoval(this,
                                    getSourceFile().getPath(),
                                    getClassFile().getPath(),
                                    getContextFile().getPath());
    }

	/* (non-Javadoc)
	 * @see bluej.editor.EditorWatcher#generateDoc()
	 */
	public void generateDoc() {
		getPackage().generateDocumentation(this);
	}
    
    public void remove(){
        prepareForRemoval();
        getPackage().removeClass(this);   
    }
    
    public void setSize(int width, int height)
    {
        super.setSize(Math.max(width, MIN_WIDTH), Math.max(height, MIN_HEIGHT));
    }
    
}
