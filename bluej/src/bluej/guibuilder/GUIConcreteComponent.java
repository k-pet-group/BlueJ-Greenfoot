package javablue.GUIBuilder;

/**
 * This interface is used byte the classes that are concrete, which is generally
 * all subclasses of Component. No layout should implement this interface.
 * @see java.awt.Component
 */
public interface GUIConcreteComponent extends GUIComponent
{
    /**
     * Sets a reference to the GUIContainer that contains the component.
     *
     * @param container	The GUIContainer that contains the component.
     */
    public abstract void setContainer(GUIContainer container);


    /**
     * Returns the GUIContainer that contains the component.
     *
     * @return	The GUIContainer that contains the component.
     */
    public abstract GUIContainer getContainer();


    /**
     * Returns the ComponentDescriptor of the component.
     * This descriptor contains the elements common to all GUIComponents.
     *
     * @return	The ComponentDescriptor for the component.
     */
    public abstract ComponentDescriptor getComponentDescriptor();
}
