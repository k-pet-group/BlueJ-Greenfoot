package javablue.GUIBuilder;

import java.awt.Component;
import java.awt.Container;


/**
 * An interface describing the methods to be implemented for a generic
 * GUIComponent.
 */
public interface GUIComponent
{
    /**
     * Sets the parent of the component.
     *
     * @param parent	The component's parent in the tree structure.
     */
    public abstract void setParent (GUIComponentNode parent);


    /**
     * Returns the parent node in the tree structure of the component.
          *
     * @return	The parent of the component.
     */
    public abstract GUIComponentNode getTreeParent();


    /**
     * Sets a reference to the StructureContainer that contains the tree containing
     * this component.
     *
     * @param	The container containing this component.
     */
    public abstract void setStructureContainer (StructureContainer structCont);


    /**
     * Returns the StructureContainer that contains the tree containing the component.
     *
     * @return	The container containing the component.
     */
    public abstract StructureContainer getStructureContainer ();


    /**
     * Sets a reference to the main application.
     *
     * @param	The main GUIBuilderApp application.
     */
    public abstract void setMainApp(GUIBuilderApp app);


    /**
     * Removes the component from the tree structure.
     */
    public abstract void delete();


    /**
     * Generates the Java code used to make the component.
     *
     * @return	The Java code used to generate the component.
     */
    public abstract ComponentCode generateCode();


    /**
     * Makes a copy of the component. This is used for the preview function, since a
     * component can only be shown in one container.
     *
     * @return	A copy of the component.
     *
     * @see StructureContainer#preview()
     */
    public abstract Component display();


    /**
     * Shows the property dialog of this component. This method will not return until the
     * dialog is closed.
     *
     * @see javablue.GUIButtonPropertyDialog
     */
    public abstract void showPropertiesDialog();


    /**
     * Sets the name of the component.
     * This method is generally only implemented in the layouts as it is
     * already implemented in all subclasses of Component.
     *
     * @param name	The name of the layout.
     * @see java.awt.Component#setName(String name)
     */
    public abstract void setName(String name);


    /**
     * Returns the name of the layout.
     * This method is generally only implemented in the layouts as it is
     * already implemented in all subclasses of Component.
     * 
     * @return	The name of the layout.
     * @see java.awt.Component#getName()
     */
    public abstract String getName();
}
