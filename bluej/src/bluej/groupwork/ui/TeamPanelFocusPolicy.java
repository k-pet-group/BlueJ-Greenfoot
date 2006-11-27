package bluej.groupwork.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.Window;

/**
 * A focus policy to override the initially focused component.
 * All other operations are delegated to the original policy.
 * 
 * @author Davin McCall
 * @version $Id: TeamPanelFocusPolicy.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class TeamPanelFocusPolicy extends FocusTraversalPolicy
{
    private Component defaultComponent;
    private FocusTraversalPolicy delegate;
    
    public TeamPanelFocusPolicy(Component defaultComponent, FocusTraversalPolicy delegate)
    {
        super();
        this.defaultComponent = defaultComponent;
        this.delegate = delegate;
    }
    
    public Component getDefaultComponent(Container aContainer)
    {
        return delegate.getDefaultComponent(aContainer);
    }
    
    public Component getComponentAfter(Container aContainer, Component aComponent)
    {
        return delegate.getComponentAfter(aContainer, aComponent);
    }
    
    public Component getComponentBefore(Container aContainer, Component aComponent)
    {
        return delegate.getComponentBefore(aContainer, aComponent);
    }
        
    public Component getFirstComponent(Container aContainer)
    {
        return delegate.getFirstComponent(aContainer);
    }
    
    public Component getLastComponent(Container aContainer)
    {
        return delegate.getLastComponent(aContainer);
    }
    
    public Component getInitialComponent(Window window)
    {
        return defaultComponent;
    }
}
