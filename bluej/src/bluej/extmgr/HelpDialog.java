package bluej.extmgr;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.DialogManager;
import bluej.utility.Utility;

import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

/**
 * The Extensions Manager help panel allows the user to view current extensions.
 *
 * @author  Clive Miller
 * @version $Id: HelpDialog.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class HelpDialog extends JDialog implements ActionListener
{
    static final String extensionsTag = Config.getString ("extmgr.extensions");
    static final String detailsTag = Config.getString ("extmgr.details");
    static final String dialogTitle = Config.getString ("extmgr.title");
    static final String installedString = Config.getString ("extmgr.installed");
    static final String projectString = Config.getString ("extmgr.project");
    static final String statusTag = Config.getString ("extmgr.details.status");
    static final String nameTag = Config.getString ("extmgr.details.name");
    static final String locationTag = Config.getString ("extmgr.details.location");
    static final String typeTag = Config.getString ("extmgr.details.type");
    static final String versionTag = Config.getString ("extmgr.details.version");
    static final String dateTag = Config.getString ("extmgr.details.date");
    static final String urlTag = Config.getString ("extmgr.details.url");
    static final String menusTag = Config.getString ("extmgr.details.menus");
    static final String preferencesTag = Config.getString ("extmgr.details.preferences");
    static final String descriptionTag = Config.getString ("extmgr.details.description");
    static final Color unvisitedLink = Color.blue;
    static final Color visitedLink = new Color (0x800080);
        
    private static final List visitedLinks = new ArrayList();
    
    private static String columnNames[] = {
        Config.getString("extmgr.statuscolumn"),
        Config.getString("extmgr.namecolumn"),
        Config.getString("extmgr.typecolumn"),
        Config.getString("extmgr.versioncolumn")};

    private static String commaList (String[] list)
    {
        String commaList = "";
        if (list != null) {
            for (int i=0; i<list.length; i++) {
                commaList += list[i] + (i == list.length-1 ? "" : ", ");
            }
        }
        return commaList;
    }
    
    private DetailsDialog detailsDialog;
    private JButton closeButton, detailsButton;
    private final ExtensionsTableModel extensionsTableModel;
    private JTable extensionsTable;

    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     *
     * @param title the title of the dialog
     */
    public HelpDialog (PkgMgrFrame parent)
    {
        super (parent, dialogTitle, true);
        extensionsTableModel = new ExtensionsTableModel();
        addWindowListener (new WindowAdapter() {
            public void windowClosing (WindowEvent e) {
                if (detailsDialog != null) detailsDialog.hide();
            }
        });
        makeDialog();
        extensionsTable.addMouseListener (new MouseAdapter() {
            public void mouseClicked (MouseEvent e) {
                if (e.getClickCount() == 2) showDetails();
            }
        });

        // save position when window is moved
        addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent event)
            {
                Config.putLocation("bluej.extmgr.helpdialog", getLocation());
            }
        });

        setLocation(Config.getLocation("bluej.extmgr.helpdialog"));

        show();
    }

    /**
     * Close button was pressed.
     */
    public void actionPerformed (ActionEvent evt)
    {
        Object src = evt.getSource();
        if (src == closeButton) {
            hide();
            if (detailsDialog != null) detailsDialog.hide();
        } else if (src == detailsButton) {
            showDetails();
        }
    }

    private void showDetails()
    {
        if (detailsDialog == null) detailsDialog = new DetailsDialog (this);
        detailsDialog.show();
        detailsDialog.updateInfo();
    }
    
    private void makeDialog()
    {
        JPanel extensionsPane = new JPanel(new BorderLayout());
        {
            JLabel extensionsLabel = new JLabel (extensionsTag);
            {
                extensionsLabel.setAlignmentX (LEFT_ALIGNMENT);
            }

            JScrollPane scrollPane = new JScrollPane();
            {
                // table of extensions
                extensionsTable = new JTable (extensionsTableModel);
                {
                    extensionsTable.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
                    extensionsTable.setPreferredScrollableViewportSize (new Dimension(400, 100));
                }

                TableColumnModel tcm = extensionsTable.getColumnModel();
                tcm.getColumn(0).setPreferredWidth(50);
                tcm.getColumn(1).setPreferredWidth(250);
                tcm.getColumn(2).setPreferredWidth(50);
                tcm.getColumn(3).setPreferredWidth(50);
    
                scrollPane.setAlignmentY (TOP_ALIGNMENT);
                scrollPane.setViewportView (extensionsTable);
            }

            JPanel buttonPanel = new JPanel();
            {
                detailsButton = new JButton (detailsTag);
                detailsButton.addActionListener (this);
                buttonPanel.add (detailsButton);
            }
            extensionsPane.setAlignmentX (LEFT_ALIGNMENT);

            extensionsPane.add (extensionsLabel, BorderLayout.NORTH);
            extensionsPane.add (scrollPane, BorderLayout.CENTER);
            extensionsPane.add (buttonPanel, BorderLayout.EAST);
            extensionsPane.setBorder (Config.generalBorder);
        }

        JPanel tablePanel = new JPanel (new GridLayout (0,1));
        {
            tablePanel.add (extensionsPane);
            tablePanel.setBorder (new BevelBorder (BevelBorder.RAISED));
        }

        JPanel buttonPanel = new JPanel();
        {
            closeButton = new JButton (Config.getString ("close"));
            closeButton.addActionListener (this);
            buttonPanel.add (closeButton);
        }

        JPanel content = (JPanel)getContentPane();
        content.setLayout (new BorderLayout());
        content.setBorder (Config.dialogBorder);

        content.add (tablePanel, BorderLayout.CENTER);
        content.add (buttonPanel, BorderLayout.SOUTH);
        DialogManager.centreDialog(this);
    }

    private class ExtensionsTableModel extends AbstractTableModel
    {
        private List extensions;
    
        /**
         * Return the name of a particular column
         *
         * @param col   the column we are naming
         * @return      a string of the columns name
         */
        public String getColumnName(int col)
        {
            return columnNames [col];
        }
    
        /**
         * Return the number of rows in the table
         *
         * @return      the number of rows in the table
         */
        public int getRowCount()
        {
            return getExtensions().size();
        }
        
        /**
         * Return the number of columns in the table
         *
         * @return      the number of columns in the table
         */
        public int getColumnCount()
        {
            return columnNames.length;
        }
        
        /**
         * Find the table entry at a particular row and column
         *
         * @param   row     the table row
         * @param   col     the table column
         * @return          the Object at that location in the table
         */
        public Object getValueAt(int row, int col)
        {
            ExtensionWrapper wrapper = getWrapper (row);
            
            if (col == 0)
                return wrapper.getStatus();
            else if (col == 1)
                return wrapper.getName();
            else if (col == 2)
                return (wrapper.getProject()!=null)?projectString:installedString;
            else if (col == 3)
                return wrapper.getVersion(); 
            throw new IllegalArgumentException("bad column number in ExtensionsTableModel::getValueAt()");
        }
    
        private ExtensionWrapper getWrapper (int index)
        {
            List exts = getExtensions(); // of ExtensionWrapper
            if (index > exts.size()) return null;
            return (ExtensionWrapper) exts.get (index);
        }
        
        /**
         * Indicate that no column is edititable
         */
        public boolean isCellEditable(int row, int col)
        {
            return false;
        }
    
        private List getExtensions()
        {
            return ExtensionsManager.getExtMgr().getExtensions();
        }
    }

    private class DetailsDialog extends JDialog implements ListSelectionListener, ActionListener
    {
        private JLabel statusField, nameField, locationField, typeField, versionField, 
                       dateField, urlField, menusField, preferencesField;
        private JTextArea descriptionField;
        private URL url;
        private JButton closeButton;
        
        public DetailsDialog (Dialog owner)
        {
            super (owner, "Details");
            
            JPanel content = new JPanel();
            content.setLayout (new BorderLayout());
            content.setBorder (Config.generalBorder);
            {
                JPanel panel = new JPanel();
                panel.setLayout (new GridBagLayout());
                {
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
                    panel.add (new JLabel (preferencesTag+":  "), tag);
                    panel.add (new JLabel (descriptionTag+":  "), tag);

                    statusField = new JLabel();     
                    nameField = new JLabel();
                    nameField.setForeground (Color.black);
                    locationField = new JLabel();
                    locationField.setForeground (Color.black);
                    typeField = new JLabel();
                    typeField.setForeground (Color.black);
                    versionField = new JLabel();
                    versionField.setForeground (Color.black);
                    dateField = new JLabel();
                    dateField.setForeground (Color.black);
                    dateField.setToolTipText ("yyyy/mm/dd hh:mm:ss");
                    urlField = new JLabel() {
                        public void paint (Graphics g) {
                            super.paint (g);
                            int y = getFont().getSize()+2;
                            g.drawLine (0,y, getWidth(),y);
                        }
                    };
                    urlField.setCursor (new Cursor (Cursor.HAND_CURSOR));
                    urlField.addMouseListener (new MouseAdapter() {
                        public void mouseClicked (MouseEvent e) {
                            openURL();
                        }
                    });
                    menusField = new JLabel();
                    menusField.setForeground (Color.black);
                    preferencesField = new JLabel();
                    preferencesField.setForeground (Color.black);
                    descriptionField = new JTextArea (4,40);
                    descriptionField.setLineWrap (true);
                    descriptionField.setWrapStyleWord (true);
//                    descriptionField.setEditable (false);
                    descriptionField.setEnabled (false);
//                    descriptionField.setForeground (urlField.getForeground());
                    descriptionField.setDisabledTextColor (Color.black);
                    descriptionField.setBackground (urlField.getBackground());
                    descriptionField.setFont (urlField.getFont());
//                    descriptionField.setMargin (new Insets (2, 4, 2, 2));
                    JScrollPane descriptionScroller = new JScrollPane (descriptionField);
                    descriptionScroller.setHorizontalScrollBarPolicy (JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                    descriptionScroller.setBorder (null);
                    

                    panel.add (statusField, value);
                    panel.add (nameField, value);
                    panel.add (locationField, value);
                    panel.add (typeField, value);
                    panel.add (versionField, value);
                    panel.add (dateField, value);
                    panel.add (urlField, value);
                    panel.add (menusField, value);
                    panel.add (preferencesField, value);
                    panel.add (descriptionScroller, value);
                }
                content.add (panel, BorderLayout.CENTER);
            }
            {
                JPanel buttonPanel = new JPanel();
                {
                    closeButton = new JButton (Config.getString ("close"));
                    closeButton.addActionListener (this);
                    buttonPanel.add (closeButton);
                }
                content.add (buttonPanel, BorderLayout.SOUTH);
            }
            setContentPane (content);

            // save position when window is moved
            addComponentListener(new ComponentAdapter() {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.extmgr.helpdialog.details", getLocation());
                }
            });
    
            setLocation(Config.getLocation("bluej.extmgr.helpdialog.details"));

            extensionsTable.getSelectionModel().addListSelectionListener (this);
            pack();
        }
        
        public void actionPerformed (ActionEvent evt)
        {
            Object src = evt.getSource();
            if (src == closeButton) {
                hide();
            }
        }

        public void valueChanged (ListSelectionEvent e)
        {
            if (e.getValueIsAdjusting()) return;
            updateInfo();
        }
        
        private void updateInfo()
        {
            int index = extensionsTable.getSelectedRow();
            if (index == -1) return;
            List exts = extensionsTableModel.getExtensions(); // of ExtensionWrapper

            ExtensionWrapper wrapper = (ExtensionWrapper) exts.get (index);
    
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
                urlField.setForeground (visitedLinks.contains (url) ? visitedLink : unvisitedLink);
            }
            
            descriptionField.setText (wrapper.getDescription());
            descriptionField.setCaretPosition (0);
            
            menusField.setText (commaList (wrapper.getMenuNames()));
            preferencesField.setText (commaList (ExtPrefPanel.INSTANCE.getPreferenceNames (wrapper)));
            
            validate();
            pack();
        }
        
        public void openURL() {
            if (url != null) {
                boolean success = Utility.openWebBrowser (url.toExternalForm());
                if (success && !visitedLinks.contains (url)) visitedLinks.add (url);
            }
            updateInfo();
        }
    }
}