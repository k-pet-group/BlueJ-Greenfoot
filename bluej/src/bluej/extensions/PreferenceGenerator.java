package bluej.extensions;

import javax.swing.JPanel;

/**
 * Extensions which wish to add preference items to BlueJ's Tools/Preferences/Extensions 
 * panel should register an instance of PreferenceGenerator with the BlueJ proxy object.
 *
 * The PreferenceGenerator allows the creation of a Panel to contain
 * preference data, and the loading and saving of that data.
 *
 * Below is a simple example to create a preference panel with a single
 * text item to record a user's favourite colour.
 *
 * To activate the preference panel you instantiate an object of the Preferences class
 * and then register it with the BlueJ proxy object, e.g.:
 * <pre>
 *        Preferences myPreferences = new Preferences(bluej);
 *        bluej.setPreferenceGenerator(myPreferences);
 * </pre>
 * The code for the Preferences class is:
 * <PRE>
 * public class Preferences implements PreferenceGenerator
 * {
 * private JPanel myPanel;
 * private JTextField color;
 * private BlueJ bluej;
 * public static final String PROFILE_LABEL="Favourite-Colour";
 *
 * public Preferences(BlueJ bluej) {
 *   this.bluej = bluej;
 *   myPanel = new JPanel();
 *   myPanel.add (new JLabel ("Favourite Colour"));
 *   color = new JTextField (40);
 *   myPanel.add (color);
 *   // Load the default value
 *   loadValues();
 *   }
 *
 * public JPanel getPanel ()  { return myPanel; }
 *
 * public void saveValues () {
 *   // Save the preference value in the BlueJ properties file
 *   bluej.setExtensionPropertyString(PROFILE_LABEL, color.getText());
 *   }
 *
 * public void loadValues () {
 *   // Load the property value from the BlueJ proerties file, default to an empty string
 *   color.setText(bluej.getExtensionPropertyString(PROFILE_LABEL,""));
 *   }
 * }
 * </pre>
 *
 * @version $Id: PreferenceGenerator.java 1852 2003-04-15 14:56:38Z iau $
 */

/*
 * AUthor Damiano Bolla, University of Kent at Canterbuty, January 2003
 */
 
public interface PreferenceGenerator
{
    /**
     * Bluej will call this method to get the panel where preferences for this
     * extension are. Preferences can be laid out as desired.
     *
     * @return    The JPanel to contain preference data.
     */
    public JPanel getPanel();


    /**
     * When this method is called the Extension should load its current values into
     * its preference panel.
     * This is called from a swing thread, so be quick.
     */
    public void loadValues();


    /**
     * When this method is called the Extension should save values from the preference panel into 
     * its internal state. Value checking can be performed at this point.
     * This is called from a swing thread, so be quick.
     */
    public void saveValues();
}

