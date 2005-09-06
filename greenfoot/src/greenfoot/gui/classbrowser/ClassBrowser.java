package greenfoot.gui.classbrowser;

import greenfoot.GreenfootObject;
import greenfoot.GreenfootWorld;
import greenfoot.actions.CompileClassAction;
import greenfoot.actions.EditClassAction;
import greenfoot.core.Greenfoot;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;
import greenfoot.gui.classbrowser.role.NormalClassRole;
import greenfoot.gui.classbrowser.role.WorldClassRole;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;

import rmiextension.wrappers.RClass;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * 
 * This is the component which has all the classes (from the project + the
 * system classes) that is visible in UI. It is responsible for drawing and
 * laying out the classes.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassBrowser.java 3551 2005-09-06 09:31:41Z polle $
 */
public class ClassBrowser extends JPanel
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private static String simObj = "greenfoot.GreenfootObject"; //.class.getName();
    private static String worldObj = "greenfoot.GreenfootWorld"; //.class.getName();

    private EditClassAction editClassAction;

    private CompileClassAction compileClassAction;

    private ButtonGroup buttonGroup = new ButtonGroup();

    private List classes = new ArrayList();

    private static final int SPACE = 2;

    private SelectionManager selectionManager = new SelectionManager();

    private ClassForest worldClasses = new ClassForest();
    private ClassForest greenfootClasses = new ClassForest();
    private ClassForest otherClasses = new ClassForest();

    private JComponent worldClassPanel;
    private JComponent greenfootClassPanel;

    public ClassBrowser()
    {
        setLayout(new GridBagLayout());
    }

    private void addClass(ClassView classView)
    {
        classes.add(classView);
        buttonGroup.add(classView);

        classView.addSelectionChangeListener(selectionManager);
        selectionManager.addSelectionChangeListener(compileClassAction);
        selectionManager.addSelectionChangeListener(editClassAction);

        layoutClasses();
    }

    public ClassView addClass(RClass rClass)
    {
        ClassView classLabel = null;

        try {
            if (rClass.getQualifiedName().equals(simObj)) {
                //the class GreenfootObject
                classLabel = new ClassView(new GreenfootClassRole(), rClass);
            }
            else if (rClass.getQualifiedName().equals(worldObj)) {
                //The class GreenfootWorld
                classLabel = new ClassView(new WorldClassRole(), rClass);
            }
            else if (rClass.isSubclassOf(simObj)) {
                //A subclass of GreenfootObject
                classLabel = new ClassView(new GreenfootClassRole(), rClass);
            }
            else if (rClass.isSubclassOf(worldObj)) {
                //A subclass of World
                classLabel = new ClassView(new WorldClassRole(), rClass);
            }
            else {
                //everything else
                classLabel = new ClassView(new NormalClassRole(), rClass);
            }
            if (classLabel != null) {
                classLabel.setClassBrowser(this);
                addClass(classLabel);
            }
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Greenfoot.getInstance().addCompileListener(classLabel);
        return classLabel;
    }

    /**
     * Orders the classes in a nice way
     */
    private void layoutClasses()
    {

        // First, we should sort the classes into SystemCLasses,
        // SimlationObjects and Others
        worldClasses = new ClassForest();
        greenfootClasses = new ClassForest();
        otherClasses = new ClassForest();
        categorizeClasses(classes, worldClasses, greenfootClasses, otherClasses);

        //  logger.info("WORLD + " + worldClasses);
        //  logger.info("SIMUL + " + greenfootClasses);
        //  logger.info("OTHER + " + otherClasses);

        //Build the gui
        this.removeAll();

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.weightx = 0;

        //World Classes

        worldClassPanel = createClassHierarchyComponent(worldClasses.getRoots(), false);
        worldClassPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        worldClassPanel.setBackground(Color.WHITE);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        JPanel filler = new JPanel();
        filler.setLayout(new FlowLayout(0, 0, FlowLayout.LEFT));
        filler.setBackground(worldClassPanel.getBackground());
        filler.add(worldClassPanel);
        filler.setBorder(BorderFactory.createTitledBorder(null, "GreenfootWorld classes"));

        this.add(filler, c);

        //Simulation classes

        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;

        greenfootClassPanel = createClassHierarchyComponent(greenfootClasses.getRoots(), false);
        greenfootClassPanel.setOpaque(true);
        greenfootClassPanel.setBackground(Color.WHITE);

        if (greenfootClassPanel != null) {
            greenfootClassPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;

            filler = new JPanel();
            filler.setLayout(new FlowLayout(0, 0, FlowLayout.LEFT));
            filler.setBackground(greenfootClassPanel.getBackground());
            filler.add(greenfootClassPanel);
            filler.setBorder(BorderFactory.createTitledBorder(null, "GreenfootObject classes"));

            this.add(filler, c);
        }

    }

    /**
     * Categorizes classes into worlds, greenfootsobjects and other classes.
     * 
     * @param classes
     *            All the classes
     * @param worldClasses
     *            The world classes put into a forest of subclasses
     * @param greenfootClasses
     *            The GreenfootObject classes put into a forest of subclasses
     * @param otherClasses
     *            The other classes put into a forest of subclasses
     */
    private void categorizeClasses(List classes, ClassForest worldClasses, ClassForest greenfootClasses,
            ClassForest otherClasses)
    {
        List worldClassesList = getInstancesOf(classes, WorldClassRole.class);
        List greenfootClassesList = getInstancesOf(classes, GreenfootClassRole.class);
        List otherClassesList = getInstancesOf(classes, NormalClassRole.class);

        worldClasses.buildForest(worldClassesList);
        greenfootClasses.buildForest(greenfootClassesList);
        otherClasses.buildForest(otherClassesList);
    }

    /**
     * Gets all instances of the given class.
     * 
     * @param classes
     * @param superClass
     * @return
     */
    private List getInstancesOf(List classes, Class cls)
    {
        List subclasses = new ArrayList();
        for (Iterator iter = classes.iterator(); iter.hasNext();) {
            ClassView classLabel = (ClassView) iter.next();
            String name = classLabel.getQualifiedClassName();
            if (cls.isInstance(classLabel.getRole())) {
                subclasses.add(classLabel);
            }
        }
        return subclasses;
    }

    /**
     * Creates a component with the class hierarchy. This method calls itself
     * recursively.
     * 
     * @param roots
     *            All the superclasses (classe hat are NOT subclasses)
     * @param header
     *            A header for specifying the types of classes in this
     *            hierarchy.
     * @return
     */
    private JComponent createClassHierarchyComponent(Collection roots, boolean isRecursiveCall)
    {

        JComponent classPanel = new JPanel();
        classPanel.setOpaque(false);
        classPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        boolean isFirstSubclass = true; //whether it is the first subclass
        //boolean isSubclass = lse; //whether it is a subclass

        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.BOTH;

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.insets.bottom = 0;
        constraints.insets.top = 4;
        constraints.insets.left = 10;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets.top = 0;
        constraints.insets.right = 0;
        constraints.insets.left = 0;
        constraints.gridy = 1;

        constraints.anchor = GridBagConstraints.WEST;

        for (Iterator iter = roots.iterator(); iter.hasNext();) {
            ClassForest.TreeEntry element = (ClassForest.TreeEntry) iter.next();
            JComponent childPanel = createClassHierarchyComponent(element.getChildren(), true);
            constraints.gridwidth = 1;
            constraints.gridx = 0;

            constraints.fill = GridBagConstraints.BOTH;
            if (!isRecursiveCall) {}
            else if (isFirstSubclass && !iter.hasNext()) {
                classPanel.add(new ArrowHeadEnd(10, 7), constraints);
            }
            else if (isFirstSubclass) {
                classPanel.add(new ArrowHead(10, 7), constraints);
            }
            else if (!iter.hasNext()) {
                classPanel.add(new ArrowConnectEnd(), constraints);
            }
            else {
                classPanel.add(new ArrowConnect(), constraints);
            }
            constraints.fill = GridBagConstraints.NONE;

            constraints.gridx = 1;
            constraints.insets.bottom = 0; //SPACE
            constraints.insets.top = SPACE;
            constraints.insets.right = 3;
            JComponent classView = (JComponent) element.getData();
            classPanel.add(classView, constraints);
            constraints.insets.bottom = 0;
            constraints.insets.top = 0;
            constraints.insets.right = 0;

            constraints.gridwidth = 1;
            constraints.gridy += 1;

            if (childPanel != null) {
                constraints.gridx = 0;

                if (iter.hasNext()) {
                    constraints.fill = GridBagConstraints.BOTH;
                    classPanel.add(new ArrowLine(), constraints);
                    constraints.fill = GridBagConstraints.NONE;
                }

                constraints.gridx++;
                classPanel.add(childPanel, constraints);
            }

            constraints.gridy += 1;
            isFirstSubclass = false;
        }

        return classPanel;

    }

    /**
     * @param compileClassAction
     */
    public void addCompileClassAction(CompileClassAction compileClassAction)
    {
        this.compileClassAction = compileClassAction;
    }

    /**
     * @param editClassAction
     */
    public void addEditClassAction(EditClassAction editClassAction)
    {
        this.editClassAction = editClassAction;
    }

    /**
     * @return
     */
    public SelectionManager getSelectionManager()
    {
        return selectionManager;
    }

    /**
     * Gets a world that have a default constructor.
     * 
     * TODO make it return an iterator
     * 
     * @return A random world. Or null if no worlds are available
     */
    public Iterator getWorldClasses()
    {
        return worldClasses.iterator();
    }

}