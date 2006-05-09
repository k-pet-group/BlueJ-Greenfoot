package greenfoot.gui.classbrowser;

import greenfoot.actions.CompileClassAction;
import greenfoot.actions.EditClassAction;
import greenfoot.core.GClass;
import greenfoot.core.GreenfootMain;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;
import greenfoot.gui.classbrowser.role.NormalClassRole;
import greenfoot.gui.classbrowser.role.WorldClassRole;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

/**
 * 
 * This is the component which has all the classes (from the project + the
 * system classes) that is visible in UI. It is responsible for drawing and
 * laying out the classes.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassBrowser.java 4144 2006-05-09 10:07:04Z polle $
 */
public class ClassBrowser extends JPanel
{
    private static String simObj = "greenfoot.Actor";
    private static String worldObj = "greenfoot.World";

    private EditClassAction editClassAction;
    private CompileClassAction compileClassAction;

    private ButtonGroup buttonGroup = new ButtonGroup();

    private SelectionManager selectionManager = new SelectionManager();

    private ClassForest worldClasses = new ClassForest();
    private ClassForest greenfootClasses = new ClassForest();
    private ClassForest otherClasses = new ClassForest();

    public ClassBrowser()
    {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(Color.WHITE);

        worldClasses = new ClassForest();
        greenfootClasses = new ClassForest();
        otherClasses = new ClassForest();
    }

    /**
     * Add a new class to the class browser data structure, without updating
     * the view on screen. The view can be explicitly updated later, using 
     * updateLayout().
     */
    public ClassView quickAddClass(GClass gClass)
    {
        ClassView classLabel = null;

        if (gClass.getQualifiedName().equals(simObj)) {
            // the class Actor
            classLabel = new ClassView(new GreenfootClassRole(), gClass);
            greenfootClasses.add(classLabel);
        }
        else if (gClass.getQualifiedName().equals(worldObj)) {
            // the class World
            classLabel = new ClassView(new WorldClassRole(), gClass);
            worldClasses.add(classLabel);
        }
        else if (gClass.isSubclassOf(simObj)) {
            // a subclass of Actor
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
            classLabel = new ClassView(NormalClassRole.getInstance(), gClass);
            otherClasses.add(classLabel);
        }
        buttonGroup.add(classLabel);
        classLabel.setClassBrowser(this);
        classLabel.addSelectionChangeListener(selectionManager);
        
        // make sure the compile and edit actions are informed of the selected class
        selectionManager.addSelectionChangeListener(compileClassAction);
        selectionManager.addSelectionChangeListener(editClassAction);

        GreenfootMain.getInstance().addCompileListener(classLabel);
        
        return classLabel;
    }
    
    
    /**
     * Add a new class to the class browser.
     */
    public ClassView addClass(GClass gClass)
    {
        ClassView classView = quickAddClass(gClass);
        updateLayout();
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
        GreenfootMain.getInstance().removeCompileListener(classView);

        classView.removeSelectionChangeListener(selectionManager);
        rebuild();
    }

    /**
     * Arrange and show the class views on screen.
     * 
     * TODO some of it should be put on the EventQueue
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
        TitledBorder border = BorderFactory.createTitledBorder(null, "World classes");
        border.setTitleColor(Color.GRAY);
        worldFrame.setBorder(border);

        this.add(worldFrame, BorderLayout.NORTH);

        // simulation classes

        JComponent greenfootClassPanel = createClassHierarchyComponent(greenfootClasses.getRoots(), false);

        if (greenfootClassPanel != null) {
            JPanel objectFrame = new JPanel();
            ((FlowLayout)objectFrame.getLayout()).setAlignment(FlowLayout.LEFT);
            objectFrame.setBackground(Color.WHITE);
            objectFrame.add(greenfootClassPanel);
            border = BorderFactory.createTitledBorder(null, "Actor classes");
            border.setTitleColor(Color.GRAY);
            objectFrame.setBorder(border);

            this.add(objectFrame, BorderLayout.CENTER);
        }
        
        if (! otherClasses.getRoots().isEmpty()) {
            JComponent otherClassPanel = createClassHierarchyComponent(otherClasses.getRoots(), false);
            
            if (otherClassPanel != null) {
                JPanel objectFrame = new JPanel();
                ((FlowLayout)objectFrame.getLayout()).setAlignment(FlowLayout.LEFT);
                objectFrame.setBackground(Color.WHITE);
                objectFrame.add(otherClassPanel);
                border = BorderFactory.createTitledBorder(null, "Other classes");
                border.setTitleColor(Color.GRAY);
                objectFrame.setBorder(border);
                
                this.add(objectFrame, BorderLayout.SOUTH);
            }
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

        JComponent hierarchyPanel = new JPanel();
        hierarchyPanel.setOpaque(false);
        hierarchyPanel.setLayout(new BoxLayout(hierarchyPanel, BoxLayout.Y_AXIS));
        
        boolean isFirstSubclass = true; //whether it is the first subclass

        for (Iterator iter = roots.iterator(); iter.hasNext();) {
            JComponent classPanel = new JPanel();
            classPanel.setOpaque(false);
            classPanel.setLayout(new BoxLayout(classPanel, BoxLayout.X_AXIS));

            ClassForest.TreeEntry element = (ClassForest.TreeEntry) iter.next();

            if (!isRecursiveCall) {
                // not recursive: we are at the greenfoot root - no arrows up
            }
            else if (isFirstSubclass && !iter.hasNext()) {
                // first and last subclass: arrow head, no line to bottom
                classPanel.add(new ArrowHeadEnd());
            }
            else if (isFirstSubclass) {
                // first, but not last, subclass: arrow head with line down
                classPanel.add(new ArrowHead());
            }
            else if (!iter.hasNext()) {
                // last subclass: line end
                classPanel.add(new ArrowConnectEnd());
            }
            else {
                // subclass in middle: line to top and bottom
                classPanel.add(new ArrowConnect());
            }

            JComponent classView = element.getData();
            classPanel.add(classView);
            JComponent flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            flowPanel.add(classPanel);
            flowPanel.setOpaque(false);
            hierarchyPanel.add(flowPanel);
            
            List children = element.getChildren();
            if(!children.isEmpty()) {
                JComponent childPanel = new JPanel();
                childPanel.setOpaque(false);
                childPanel.setLayout(new BoxLayout(childPanel, BoxLayout.X_AXIS));

                JComponent child = createClassHierarchyComponent(children, true);
                if (iter.hasNext()) {
                    childPanel.add(new ArrowLine());
                }
                else if (isRecursiveCall) {
                    childPanel.add(new EmptySpace());
                }
                childPanel.add(child);
                flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                flowPanel.setOpaque(false);
                flowPanel.add(childPanel);
                hierarchyPanel.add(flowPanel);
            }
            
            isFirstSubclass = false;
        }

        return hierarchyPanel;

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
     * <p>
     * Runs on the event thread
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