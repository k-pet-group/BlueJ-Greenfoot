package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import bluej.guibuilder.graphics.*;

/**
 *
 * @author  Andrew Patterson
 * @version $cvs $
 */
public abstract class NewComponentAction extends AbstractAction
{
    public NewComponentAction(String name)
    {
        super(name);
    }

    public NewComponentAction(String name, Icon icon)
    {
        super(name, icon);
    }

    abstract public GUIComponent createNewComponent(GUIComponentNode parent,
                                                    StructureContainer structCont,
                                                    GUIBuilderApp app);

    public void actionPerformed(ActionEvent e)
    {
    }

}
