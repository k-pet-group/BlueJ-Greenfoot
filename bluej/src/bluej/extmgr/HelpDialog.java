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
 *  The Extensions Manager help panel allows the user to view current
 *  extensions.
 *
 * @version    $Id: HelpDialog.java 1497 2002-11-11 10:32:50Z damiano $
 */
public class HelpDialog extends JDialog implements ActionListener
{
    private final String installedString = Config.getString("extmgr.installed");
    private final String projectString = Config.getString("extmgr.project");
    private final ImageIcon infoIcon = Config.getImageAsIcon("image.extmgr.info");

    private HelpDetailDialog detailDialog;
    private JButton closeButton;
    private ExtensionsTableModel extensionsTableModel;
    private JTable extensionsTable;


    /**
     *  Setup the UI for the dialog and event handlers for the dialog's buttons.
     *
     * @param  parent  Description of the Parameter
     */
    public HelpDialog(PkgMgrFrame parent)
    {
        super(parent, Config.getString("extmgr.title"), true);

        extensionsTable = getExtensionTable();
        JScrollPane extensionsPane = new JScrollPane(extensionsTable);

        JPanel buttonPanel = new JPanel();
        closeButton = new JButton(Config.getString("close"));
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);

        JPanel rootPane = (JPanel) getContentPane();
        rootPane.setLayout(new BorderLayout());
        rootPane.setBorder(Config.dialogBorder);

        rootPane.add(extensionsPane, BorderLayout.CENTER);
        rootPane.add(buttonPanel, BorderLayout.SOUTH);
        DialogManager.centreDialog(this);

        // save position when window is moved
        addComponentListener(
            new ComponentAdapter()
            {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.extmgr.helpdialog", getLocation());
                }
            });

        setLocation(Config.getLocation("bluej.extmgr.helpdialog"));
        pack();
        setVisible(true);
    }


    /**
     *  Just to manage the close button
     *
     * @param  evt  Description of the Parameter
     */
    public void actionPerformed(ActionEvent evt)
    {
        Object src = evt.getSource();
        if (src == null)
            return;

        if (src == closeButton)
            hide();
    }


    /**
     *  Utility, to make code cleaner
     */
    private void showDetails()
    {
        if (detailDialog == null)
            detailDialog = new HelpDetailDialog(this);

        int selectedColumn = extensionsTable.getSelectedColumn();

        // We want the user to click on the ICON !!!
        if (selectedColumn != 0)
            return;

        detailDialog.updateInfo(extensionsTable.getSelectedRow());
        detailDialog.setVisible(true);
    }


    /**
     *  Utility, to make the code clean. Returns the table that describes the
     *  installed extensions I really would like not to set so many preferred
     *  values...
     *
     * @return    The extensionTable value
     */
    private JTable getExtensionTable()
    {
        extensionsTableModel = new ExtensionsTableModel();

        JTable aTable = new JTable(extensionsTableModel);
        aTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        aTable.setPreferredScrollableViewportSize(new Dimension(400, 100));

        TableColumnModel tcm = aTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(20);
        tcm.getColumn(2).setPreferredWidth(250);

        aTable.setRowHeight(18);
        aTable.setRowSelectionAllowed(false);

        aTable.addMouseListener(new myMouseAdapter());
        return aTable;
    }


    /**
     *  When a mouse is clicked I come here Maybe I can do it with the selected,
     *  it may be easier... next release ?
     */
    private class myMouseAdapter extends MouseAdapter
    {
        /**
         *  Description of the Method
         *
         * @param  e  Description of the Parameter
         */
        public void mouseClicked(MouseEvent e)
        {
            if (e.getClickCount() == 1)
                showDetails();
        }
    }


    /**
     *  This models the data of the table. Basically the JTable ask this class
     *  about the data to be displayed on the screen
     */
    private class ExtensionsTableModel extends AbstractTableModel
    {
        // It does not matter very much if it is not static, it is created once only
        private String columnNames[] = {
                "",
                Config.getString("extmgr.statuscolumn"),
                Config.getString("extmgr.namecolumn"),
                Config.getString("extmgr.typecolumn"),
                Config.getString("extmgr.versioncolumn")};


        /**
         *  Gets the rowCount attribute of the ExtensionsTableModel object
         *
         * @return    The rowCount value
         */
        public int getRowCount()
        {
            return getExtensions().size();
        }


        /**
         *  Gets the columnCount attribute of the ExtensionsTableModel object
         *
         * @return    The columnCount value
         */
        public int getColumnCount()
        {
            return columnNames.length;
        }


        /**
         *  Gets the columnName attribute of the ExtensionsTableModel object
         *
         * @param  col  Description of the Parameter
         * @return      The columnName value
         */
        public String getColumnName(int col)
        {
            return columnNames[col];
        }


        /**
         *  Gets the cellEditable attribute of the ExtensionsTableModel object
         *
         * @param  row  Description of the Parameter
         * @param  col  Description of the Parameter
         * @return      The cellEditable value
         */
        public boolean isCellEditable(int row, int col)
        {
            return false;
        }


        /**
         *  Gets the valueAt attribute of the ExtensionsTableModel object
         *
         * @param  row  Description of the Parameter
         * @param  col  Description of the Parameter
         * @return      The valueAt value
         */
        public Object getValueAt(int row, int col)
        {
            if (col == 0)
                return infoIcon;

            // I really need to sort this out... Do I need to call it every time ?
            ExtensionWrapper wrapper = getWrapper(row);
            if (wrapper == null)
                return "getValueAt: ERROR: no wrapper at row=" + row + " col=" + col;

            if (col == 1)
                return wrapper.getStatus();

            if (col == 2)
                return wrapper.getName();

            if (col == 3)
                return (wrapper.getProject() != null) ? projectString : installedString;

            if (col == 4)
                return wrapper.getVersion();

            // If I trow an exception all will stop. This instead keeps going
            return "getValueAt: ERROR at row=" + row + " col=" + col;
        }


        /**
         *  Gets the columnClass attribute of the ExtensionsTableModel object
         *
         * @param  col  Description of the Parameter
         * @return      The columnClass value
         */
        public Class getColumnClass(int col)
        {
            if (col == 0)
                return new ImageIcon().getClass();

            return new String().getClass();
        }


        /**
         *  Gets the wrapper attribute of the ExtensionsTableModel object
         *
         * @param  index  Description of the Parameter
         * @return        The wrapper value
         */
        private ExtensionWrapper getWrapper(int index)
        {
            // Every time I am called I reload this ?
            List exts = getExtensions();
            // of ExtensionWrapper
            if (index > exts.size())
                return null;

            return (ExtensionWrapper) exts.get(index);
        }


        /**
         *  Gets the extensions attribute of the ExtensionsTableModel object
         *
         * @return    The extensions value
         */
        private List getExtensions()
        {
            // Just wondering if there is a thread sync issue here
            return ExtensionsManager.getExtMgr().getExtensions();
        }
    }
}
