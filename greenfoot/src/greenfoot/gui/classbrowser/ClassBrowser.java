package greenfoot.gui.classbrowser;

import greenfoot.actions.CompileClassAction;
import greenfoot.actions.EditClassAction;
import greenfoot.core.GClass;
import greenfoot.core.Greenfoot;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;
import greenfoot.gui.classbrowser.role.NormalClassRole;
import greenfoot.gui.classbrowser.role.WorldClassRole;
import java.awt.BorderLayout;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 
 * This is the component which has all the classes (from the project + the
 * system classes) that is visible in UI. It is responsible for drawing and
 * laying out the classes.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassBrowser.java 3855 2006-03-21 22:11:15Z mik $
 */
public class ClassBrowser extends JPanel
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private static String simObj = "greenfoot.GreenfootObject";
    private static String worldObj = "greenfoot.GreenfootWorld";

    private EditClassAction editClassAction;
    private CompileClassAction compileClassAction;
    private static final int SPACE = 2;

    private ButtonGroup buttonGroup = new ButtonGroup();

    private SelectionManager selectionManager = new SelectionManager();

    private ClassForest worldClasses = new ClassForest();
    private ClassForest greenfootClasses = new ClassForest();
    private ClassForest otherClasses = new ClassForest();

    public ClassBrowser()
    {
        setLayout(new BorderLayout());
        worldClasses = new ClassForest();
        greenfootClasses = new ClassForest();
        otherClasses = new ClassForest();
    }

    /**
     * Add a new class to the class browser data structure, without updating
     * the view on screen. The view can be explicitly updated later, using 
     * update().
     */
    public ClassView quickAddClass(GClass gClass)
    {
        ClassView classLabel = null;

        if (gClass.getQualifiedName().equals(simObj)) {
            // the class GreenfootObject
            classLabel = new ClassView(new GreenfootClassRole(), gClass);
            greenfootClasses.add(classLabel);
        }
        else if (gClass.getQualifiedName().equals(worldObj)) {
            // the class GreenfootWorld
            classLabel = new ClassView(new WorldClassRole(), gClass);
            worldClasses.add(classLabel);
        }
        else if (gClass.isSubclassOf(simObj)) {
            // a subclass of GreenfootObject
            classLabel = new ClassView(new GreenfootClassRole(), gClass);
            greenfootClasses.add(classLabel);
        }
        else if (gClass.isSubclassOf(worldObj)) {
            // a subclass of World
            classLabel = new ClassView(new WorldClassRole(), gClass);
            worldClasses.add(classLabel);
        }
        else {
            // everything else
            classLabel = new ClassView(new NormalClassRole(), gClass);
            otherClasses.add(classLabel);
        }
        buttonGroup.add(classLabel);
        classLabel.setClassBrowser(this);
        classLabel.addSelectionChangeListener(selectionManager);
        
        //TODO: the following two lines look dodgy... (mik)
        selectionManager.addSelectionChangeListener(compileClassAction);
        selectionManager.addSelectionChangeListener(editClassAction);
        
        return classLabel;
    }
    
    
    /**
     * Add a new class to the class browser.
     */
    public ClassView addClass(GClass gClass)
    {
        ClassView classView = quickAddClass(gClass);
        updateLayout();
        Greenfoot.getInstance().addCompileListener(classView);
        return classView;
    }
    
    /**
     * Remove a class from the browser and update the view on screen.
     */
    public void removeClass(ClassView classView) 
    {
        boolean found = greenfootClasses.remove(classView);        
        if(!found)
            found = worldClasses.remove(classView);
        if(!found)
            found = otherClasses.remove(classView);
                
        buttonGroup.remove(classView);
        Greenfoot.getInstance().removeCompileListener(classView);

        classView.removeSelectionChangeListener(selectionManager);
        rebuild();
    }

    /**
     * Arrange and show the class views on screen.
     */
    public void updateLayout()
    {
        greenfootClasses.rebuild();
        worldClasses.rebuild();
        otherClasses.rebuild();
        
        this.removeAll();  // remove current components

        // world Classes

        JComponent worldClassPanel = createClassHierarchyComponent(worldClasses.getRoots(), false);

        JPanel worldFrame = new JPanel();
        ((FlowLayout)worldFrame.getLayout()).setAlignment(FlowLayout.LEFT);
        worldFrame.setBackground(Color.WHITE);
        worldFrame.add(worldClassPanel);
        worldFrame.setBorder(BorderFactory.createTitledBorder(null, "World classes"));

        this.add(worldFrame, BorderLayout.NORTH);

        // simulation classes

        JComponent greenfootClassPanel = createClassHierarchyComponent(greenfootClasses.getRoots(), false);

        if (greenfootClassPanel != null) {
            JPanel objectFrame = new JPanel();
            ((FlowLayout)objectFrame.getLayout()).setAlignment(FlowLayout.LEFT);
            objectFrame.setBackground(Color.WHITE);
            objectFrame.add(greenfootClassPanel);
            objectFrame.setBorder(BorderFactory.createTitledBorder(null, "Object classes"));

            this.add(objectFrame, BorderLayout.CENTER);
        }
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
            JComponent classView = element.getData();
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
     * Gets all the classes that is in the GreenfootWorls classes section.
     * 
     * 
     * @return A list of ClassViews
     */
    public Iterator getWorldClasses()
    {
        return worldClasses.iterator();
    }

    /**
     * Rebuilds the class browser: updating the hierachy.
     *
     */
    public void rebuild()
    {
        Thread t = new Thread() {
            public void run() {
                updateLayout();
                revalidate();
                repaint();
            }
        };
        SwingUtilities.invokeLater(t);        
    }

}