package bluej.extmgr;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.DialogManager;
import bluej.utility.Utility;

import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.ListSelectionModel;
import javax.swing.table.*;
import javax.swing.ImageIcon;
import java.awt.*;

/**
 * This class can display info on a particular extension. It is not really
 * bound to the HelpDialog and may be useful in the future.
 */
class HelpDetailDialog extends JDialog implements ActionListener
{
    private final String extensionsTag = Config.getString ("extmgr.extensions");
    private final String detailsTag = Config.getString ("extmgr.details");
    private final String installedString = Config.getString ("extmgr.installed");
    private final String projectString = Config.getString ("extmgr.project");
    private final String statusTag = Config.getString ("extmgr.details.status");
    private final String nameTag = Config.getString ("extmgr.details.name");
    private final String locationTag = Config.getString ("extmgr.details.location");
    private final String typeTag = Config.getString ("extmgr.details.type");
    private final String versionTag = Config.getString ("extmgr.details.version");
    private final String dateTag = Config.getString ("extmgr.details.date");
    private final String urlTag = Config.getString ("extmgr.details.url");
    private final String menusTag = Config.getString ("extmgr.details.menus");
    private final String preferencesTag = Config.getString ("extmgr.details.preferences");
    private final String descriptionTag = Config.getString ("extmgr.details.description");

  
    private JLabel statusField, nameField, locationField, typeField, versionField, 
                   dateField, urlField, menusField; //, preferencesField;
    private JTextArea descriptionField;
    private URL url;
    private JButton closeButton;
        
    /**
     * Constructor
     */
    public HelpDetailDialog (Dialog owner )
    {
        super (owner, "Details");

        JPanel panel = new JPanel();
        panel.setLayout (new GridBagLayout());

        GridBagConstraints tag = new GridBagConstraints();
        tag.gridx = 0;
        tag.anchor = GridBagConstraints.NORTHEAST;

        GridBagConstraints value = new GridBagConstraints();
        value.gridx = 1;
        value.anchor = GridBagConstraints.NORTHWEST;
        value.gridwidth = 3;
                    
        panel.add (new JLabel (statusTag+":  "), tag);
        panel.add (new JLabel (nameTag+":  "), tag);
        panel.add (new JLabel (locationTag+":  "), tag);
        panel.add (new JLabel (typeTag+":  "), tag);
        panel.add (new JLabel (versionTag+":  "), tag);
        panel.add (new JLabel (dateTag+":  "), tag);
        panel.add (new JLabel (urlTag+":  "), tag);
        panel.add (new JLabel (menusTag+":  "), tag);
//        panel.add (new JLabel (preferencesTag+":  "), tag);
        panel.add (new JLabel (descriptionTag+":  "), tag);

        statusField = new JLabel();     
        nameField = new JLabel();
        locationField = new JLabel();
        typeField = new JLabel();
        versionField = new JLabel();
        dateField = new JLabel();
        dateField.setToolTipText ("yyyy/mm/dd hh:mm:ss");

        urlField = new JLabel();
        urlField.setCursor (new Cursor (Cursor.HAND_CURSOR));
        urlField.addMouseListener (new MouseAdapter() {
            public void mouseClicked (MouseEvent e) {
                openURL();
            }
        });

        menusField = new JLabel();
//        preferencesField = new JLabel();

        descriptionField = new JTextArea (4,40);
        descriptionField.setLineWrap (true);
        descriptionField.setWrapStyleWord (true);
        descriptionField.setEnabled (false);
        descriptionField.setDisabledTextColor (Color.black);
        descriptionField.setBackground(dateField.getBackground());
        // I need to put into a scrollbar in case the text is too much    
        JScrollPane descriptionScroller = new JScrollPane (descriptionField);
        descriptionScroller.setBorder (null);

        panel.add (statusField, value);
        panel.add (nameField, value);
        panel.add (locationField, value);
        panel.add (typeField, value);
        panel.add (versionField, value);
        panel.add (dateField, value);
        panel.add (urlField, value);
        panel.add (menusField, value);
//        panel.add (preferencesField, value);
        panel.add (descriptionScroller, value);

        // I need to put this beast into a scroll pane
        JScrollPane detailScroll = new JScrollPane(panel);


        // The close button goes into a panel, to make it nice...
        JPanel buttonPanel = new JPanel();
        closeButton = new JButton (Config.getString ("close"));
        closeButton.addActionListener (this);
        buttonPanel.add (closeButton);

        // TIme to put the two main panels into the root pane
        JPanel rootPane = (JPanel)getContentPane();
        rootPane.setLayout (new BorderLayout());
        rootPane.setBorder (Config.dialogBorder);

        rootPane.add (detailScroll, BorderLayout.CENTER);
        rootPane.add (buttonPanel, BorderLayout.SOUTH);

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent event)
            {
                Config.putLocation("bluej.extmgr.helpdialog.details", getLocation());
            }
        });
    
        setLocation(Config.getLocation("bluej.extmgr.helpdialog.details"));
        pack();
    }

        
    /**
     * Called when the button is pressed
     */
    public void actionPerformed (ActionEvent evt)
    {
        Object src = evt.getSource();
        if ( src == null ) 
            return;
        if (src == closeButton) 
            setVisible(false);
    }

    /**
     * Utility, to make code clean. Concatenate a series of strings separated by,
     * We are just tryng to be quick, not that it really matters but still better nd clearer
     */
    private String commaList (String[] list)
    {
        if (list == null) 
            return "";
        if (list.length < 1 ) 
            return "";
    
        StringBuffer commaList = new StringBuffer(200);

        int lastIndex = list.length-1;     // We know that there is at least one element
        for (int index=0; ; index++)
          {
          commaList.append (list[index]);
          if ( index >= lastIndex ) break;
          // There is another element in the list, add a comma
          commaList.append(",");
          }

        return commaList.toString();
    }


    /**
    * Called with the index on what extension you want to update
    * It would be better if I gave the extension.... Next one ?
    */
    public void updateInfo(int extensionIndex)
    {
        if (extensionIndex < 0 )
            return;

        List exts = ExtensionsManager.getExtMgr().getExtensions();
        ExtensionWrapper wrapper = (ExtensionWrapper) exts.get (extensionIndex);

        // You never know when you get strange errors...
        if ( wrapper == null ) 
            return;
    
        statusField.setText (wrapper.getStatus());
        statusField.setForeground (wrapper.isValid()?Color.black:Color.red);

        nameField.setText (wrapper.getName());
        locationField.setText (wrapper.getLocation());
        typeField.setText ((wrapper.getProject()!=null)?projectString:installedString);
        versionField.setText (wrapper.getVersion());
        dateField.setText (wrapper.getDate());
            
        url = wrapper.getURL();
        if (url == null) {
            urlField.setText (null);
        } else {
            urlField.setText (url.toExternalForm());
            urlField.setForeground (Color.blue);
        }
            
        descriptionField.setText (wrapper.getDescription());
        descriptionField.setCaretPosition (0);
            
        menusField.setText (commaList (wrapper.getMenuNames()));
//        preferencesField.setText (commaList (ExtPrefPanel.INSTANCE.getPreferenceNames (wrapper)));
            
        validate();
        pack();
    }
        
    private void openURL() 
    {
        if (url == null) 
            return;
        Utility.openWebBrowser (url.toExternalForm());
    }
}


