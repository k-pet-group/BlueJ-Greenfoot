package bluej.extensions;

import javax.swing.JPanel;

/**
 *  Provides a means by which to add preference items to the Tools-Preferences
 *  Extensions panel. <P>
 *
 *  What you do is to implement getPanel(), loadValues() and saveValues() and
 *  put all your swing Components into a JPanel that will be taken for drawing.
 *  After having allocated this class you can simply call bluej.setPrefGen (
 *  myPrefGen ); </P> This is a simple example of how a preference panel can be
 *  implemented. <pre>
 * public class JsPreferences implements PrefGen
 * {
 * private JPanel myPanel;
 * private JTextField profileCmd;
 * private BlueJ bluej;
 * public static final String PROFILE_LABEL="profile.cmd";
 *
 * public JsPreferences(BlueJ bluej) {
 *   this.bluej = bluej;
 *   myPanel = new JPanel();
 *   myPanel.add (new JLabel ("Profile Command"));
 *   profileCmd = new JTextField (40);
 *   myPanel.add (profileCmd);
 *   // So the user sees the previous values
 *   loadValues();
 *   }
 *
 * public JPanel getPanel ()  { return myPanel; }
 *
 * public void saveValues () {
 *   bluej.setExtPropString(PROFILE_LABEL,profileCmd.getText());
 *   }
 *
 * public void loadValues () {
 *   profileCmd.setText(bluej.getExtPropString(PROFILE_LABEL,""));
 *   }
 * }
 * </pre>
 */

public interface PrefGen
{
    /**
     *  The system will call this method to get the panel where you have put all
     *  your preferences. You can layout your preferences items as you wish.
     *
     * @return    The panel value
     */
    public JPanel getPanel();


    /**
     *  Called by the host when it's time for the subclass to load or revert its
     *  value What it should do is loading values from somewhere and putting
     *  them into the panel objects
     */
    public void loadValues();


    /**
     *  Called by the host when it's time for the subclass to save its value
     *  What it should do is to get the values from the components and save them
     */
    public void saveValues();
}

