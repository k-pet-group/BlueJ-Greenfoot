package bluej.extensions;

import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.ExtensionWrapper;
import bluej.extmgr.ExtPrefPanel;

import bluej.Config;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * The BlueJ proxy Preference Panel object. This provides a means by which to add preference
 * items to the Tools-Preferences Extensions panel.
 * The object can be
 * got from {@link bluej.extensions.BlueJ#getPrefPanel() getPrefPanel()}.
 *
 * @author Clive Miller
 * @version $Id: BPrefPanel.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class BPrefPanel
{
    private final ExtensionWrapper ew;
    private final ExtPrefPanel prefPanel;
    
    BPrefPanel (ExtensionWrapper ew)
    {
        this.ew = ew;
        this.prefPanel = ExtPrefPanel.INSTANCE;
    }
    
    /**
     * The superclass to all settings item objects
     */
    public abstract class PPSetting
    {
        protected final String name, title;
        
        /**
         * Creates a new PPSetting, with these mandatory parameters
         * @param name the machine name by which to identify this item (must be unique
         * within any extension)
         * @param title the user's visual name of this item
         */
        public PPSetting (String name, String title)
        {
            this.name = name;
            this.title = title;
        }
        
        /**
         * Gets the identifier that was given to this setting.
         * @return the name given in the constructor
         */
        public String getName()
        {
            return name;
        }
        
        /**
         * Gets the user's visual name of this item
         * @return the title given in the constructor
         */
        public String getTitle()
        {
            return title;
        }
        
        /**
         * Gets the component to be drawn in the preferences panel
         * @return the top-level component to be drawn
         */
        public abstract Component getDrawable();

        /**
         * Called by the host when it's time for the subclass to load or revert its value
         */
        public abstract void loadValue();
        
        /**
         * Called by the host when it's time for the subclass to save its value
         */
        public abstract void saveValue();
    }
    
    /**
     * Adds the requested setting to the preference panel.
     */         
    public void add (PPSetting item)
    {
        prefPanel.addSetting (ew, item);
    }
    
    /**
     * An implementation of a setting that contains a text field and a label, 
     * to put on the preferences panel.
     */
    public class PPTextField extends PPSetting
    {
        private final JTextField textField;
        private final JPanel item;
        
        /**
         * Creates a text field for the preferences panel that retains its value by storing it
         * in the bluej properties file. 
         * @param name the internal name given to the item. This should be a constant String, and will
         * be used for the persistance of the item.
         * @param title the name to display on the panel. This could be a language-independent derived String,
         * such as <CODE>bj.getLabel("preferences.username")</CODE>.
         * @param size the size of the text field to display
         */
        public PPTextField (String name, String title, int size)
        {
            super (name, title);
            item = new JPanel (new BorderLayout());
            item.add (new JLabel (title+": "), BorderLayout.WEST);
            textField = new JTextField("", size);
            item.add (textField);
            loadValue();
        }
        
        /**
         * Gets the current value for this text field preference item
         * @return the value currently held in the text field
         */
        public String getValue()
        {
            return textField.getText();
        }
        
        /**
         * Sets the required value for this text field preference item
         * @param value the value to place in this text field
         */
        public void setValue (String value)
        {
            textField.setText (value);
            saveValue();
        }
        
        public Component getDrawable()
        {
            return item;
        }
        
        public void loadValue()
        {
            String value = Config.getPropString (ExtensionsManager.getPreferencesString (ew, name), null);
            textField.setText (value);
        }
        
        public void saveValue()
        {
            String value = textField.getText();
            Config.putPropString (ExtensionsManager.getPreferencesString (ew, name), value);
        }
    }


    /**
     * A representation of a Preference Panel check box item.
     */
    public class PPCheckBox extends PPSetting
    {
        private JCheckBox checkBox;
        
        /**
         * Creates a check box for the preferences panel.
         * @param name the internal name given to the item. This should be a constant String, and will
         * be used for the persistance of the item.
         * @param title the name to display on the panel. This could be a language-independent derived String,
         * such as <CODE>bj.getLabel("preferences.username")</CODE>.
         * @return a reference to control the item
         */
        public PPCheckBox (String name, String title)
        {
            super (name, title);
            checkBox = new JCheckBox (title, false);
            loadValue();
        }
        
        /**
         * Gets the current value for this check box preference item
         * @return the value currently held in the check box
         */
        public boolean getValue()
        {
            return checkBox.isSelected();
        }
        
        /**
         * Sets the required value for this check box preference item
         * @param value the value to place in this check box
         */
        public void setValue (boolean value)
        {
            checkBox.setSelected (value);
        }
        
        public Component getDrawable()
        {
            return checkBox;
        }
        
        public void loadValue()
        {
            String value = Config.getPropString (ExtensionsManager.getPreferencesString (ew, name), "false");
            checkBox.setSelected (new Boolean (value).booleanValue());
        }
        
        public void saveValue()
        {
            String value = String.valueOf (checkBox.isSelected());
            Config.putPropString (ExtensionsManager.getPreferencesString (ew, name), value);
        }
    }
}