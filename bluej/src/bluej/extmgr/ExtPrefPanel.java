package bluej.extmgr;

import bluej.extensions.BPrefPanel.PPSetting;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefPanelListener;
import bluej.prefmgr.PrefMgrDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

/**
 * The Extensions Preferences Panel allows the user to interactively
 * edit a number of settings, as requested by extensions.
 *
 * @author  Clive Miller
 * @version $Id: ExtPrefPanel.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class ExtPrefPanel extends JPanel implements PrefPanelListener
{
    public static final ExtPrefPanel INSTANCE = new ExtPrefPanel();

    public static void register()
    {
        PrefMgrDialog.add(INSTANCE, Config.getString("extmgr.extensions"), INSTANCE);
    }
    
    private JPanel contents;
    private Collection items; // of PPSetting
    private Map extensionPanels; // of ExtensionWrapper => JPanel
    private Map extensionItems; // of PPSetting => ExtensionWrapper
    
    /**
     * Sets up the UI.
     * Registers the extensions preference panel with the preferences.
     * dialog
     */
    private ExtPrefPanel()
    {
        items = new ArrayList();
        setBorder(Config.generalBorder);
        contents = new JPanel();
        contents.setPreferredSize (new Dimension (0, 0));
        JScrollPane scrollPane = new JScrollPane (contents, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setLayout (new BorderLayout());
        add (scrollPane);
        extensionPanels = new HashMap();
        extensionItems = new HashMap();
    }
    
    public void beginEditing()
    {
    }

    public void revertEditing()
    {
        for (Iterator it=items.iterator(); it.hasNext();) {
            PPSetting item = (PPSetting)it.next();
            item.loadValue();
        }
    }

    public void commitEditing()
    {
        for (Iterator it=items.iterator(); it.hasNext();) {
            PPSetting item = (PPSetting)it.next();
            item.saveValue();
        }
    }

    public void removePreferences (ExtensionWrapper ew)
    {
        for (Iterator it=items.iterator(); it.hasNext();) {
            PPSetting item = (PPSetting)it.next();
            if (extensionItems.get (item) == ew) {
                it.remove();
                JPanel panel = (JPanel)extensionPanels.get (ew);
                panel.remove (item.getDrawable()); // Not strictly necessary
                if (panel.getComponentCount() == 0) contents.remove (panel);
                repaint();
            }
        }
    }
    
    String[] getPreferenceNames (ExtensionWrapper ew)
    {
        Collection names = new ArrayList();
        for (Iterator it=items.iterator(); it.hasNext();) {
            PPSetting item = (PPSetting)it.next();
            if (extensionItems.get (item) == ew) names.add (item.getTitle());
        }
        return (String[])names.toArray (new String[0]);
    }
        
    public void addSetting (ExtensionWrapper ew, PPSetting item)
    {
        items.add (item);
        extensionItems.put (item, ew);
        Component dr = item.getDrawable();
        JPanel panel = (JPanel)extensionPanels.get (ew);
        if (panel == null) {
            panel = new JPanel();
            panel.setLayout (new BoxLayout (panel, BoxLayout.Y_AXIS));
            String extName = ew.getName();
            extName = extName.substring (extName.lastIndexOf ('.')+1);
            panel.setBorder (new TitledBorder (extName));
            contents.add (panel);
            extensionPanels.put (ew, panel);
        }
        panel.add (dr);
        Container parent = getParent();
        if (parent != null) parent.repaint();
    }
}
