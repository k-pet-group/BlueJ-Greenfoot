/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.extmgr;

import bluej.*;
import bluej.utility.EscapeDialog;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/**
 * The Extensions Manager help panel allows the user to view current extensions.
 *
 * @author Clive Millaer, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class HelpDialog implements ActionListener
{
    private final String systemString = Config.getString("extmgr.systemExtensionShort");
    private final String projectString = Config.getString("extmgr.projectExtensionShort");
    private final ImageIcon infoIcon = Config.getImageAsIcon("image.extmgr.info");

    private JDialog mainFrame;
    private HelpDetailDialog detailDialog;
    private JButton closeButton;
    private JTable extensionsTable;
    private ExtensionsTableModel extensionsTableModel;
    private List<ExtensionWrapper> extensionsList;

    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     * This new version is guarantee to have a valid extension manager.
     */
    HelpDialog(List<ExtensionWrapper> i_extensionsList, JFrame parent)
    {
        extensionsList = i_extensionsList;
    
        extensionsTable = getExtensionTable();
        JScrollPane extensionsPane = new JScrollPane(extensionsTable);

        JPanel buttonPanel = new JPanel();
        closeButton = new JButton(Config.getString("close"));
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);


        mainFrame = new EscapeDialog(parent, Config.getString("extmgr.title"), true);
        JPanel rootPane = (JPanel)mainFrame.getContentPane();
        rootPane.setLayout(new BorderLayout());
        rootPane.setBorder(BlueJTheme.dialogBorder);

        rootPane.add(extensionsPane, BorderLayout.CENTER);
        rootPane.add(buttonPanel, BorderLayout.SOUTH);

        // save position when window is moved
        mainFrame.addComponentListener(
            new ComponentAdapter()
            {
                public void componentMoved(ComponentEvent event)
                {
                    Config.putLocation("bluej.extmgr.helpdialog", mainFrame.getLocation());
                }
            });

        mainFrame.setLocation(Config.getLocation("bluej.extmgr.helpdialog"));
        mainFrame.pack();
        mainFrame.setVisible(true);
    }


    /**
     *  Just to manage the close button.
     */
    public void actionPerformed(ActionEvent evt)
    {
        // We really want all of this to go away, really, not just hiding it !
        mainFrame.dispose();
    }


    /**
     *  Utility, to make code cleaner
     */
    private void showDetails()
    {
        // If no detail dialog is created then make it...
        if (detailDialog == null) 
            detailDialog = new HelpDetailDialog(mainFrame);

        int selectedColumn = extensionsTable.getSelectedColumn();

        // We want the user to click on the ICON !!!
        if (selectedColumn != 0) 
            return;

        ExtensionWrapper aWrapper = getWrapper(extensionsTable.getSelectedRow());
        if (aWrapper == null) 
            return;
        
        detailDialog.updateInfo(aWrapper);
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
     *  Gets the wrapper attribute of the ExtensionsTableModel object
     *
     * @param  index  Description of the Parameter
     * @return        The wrapper value
     */
    private ExtensionWrapper getWrapper(int index)
    {
        // of ExtensionWrapper
        if (index > extensionsList.size()) 
            return null;

        return (ExtensionWrapper) extensionsList.get(index);
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
            if(e.getClickCount() == 1)  
                showDetails();
        }
    }

    private final static String columnNames[] = {
            " ",
            Config.getString("extmgr.statuscolumn"),
            Config.getString("extmgr.namecolumn"),
            Config.getString("extmgr.typecolumn")
    };

    /**
     *  This models the data of the table. Basically the JTable ask this class
     *  about the data to be displayed on the screen
     */
    class ExtensionsTableModel extends AbstractTableModel
    {
        // It does not matter very much if it is not static, it is created once only

        /**
         *  Returns the rowCount attribute of the ExtensionsTableModel object
         */
        public int getRowCount()
        {
            return extensionsList.size();
        }

        /**
         * Returns the columnCount attribute of the ExtensionsTableModel object
         */
        public int getColumnCount()
        {
            return columnNames.length;
        }

        /**
         * Gets the columnName attribute of the ExtensionsTableModel object
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
         * Gets the valueAt attribute of the ExtensionsTableModel object
         *
         * @param  row  Description of the Parameter
         * @param  col  Description of the Parameter
         * @return      The valueAt value
         */
        public Object getValueAt(int row, int col)
        {
            if (col == 0) {
                return infoIcon;
            }

            ExtensionWrapper wrapper = getWrapper(row);
            if (wrapper == null) {
                return "getValueAt: ERROR: no wrapper at row=" + row + " col=" + col;
            }

            if (col == 1) {
                return wrapper.getExtensionStatus();
            }

            if (col == 2) { 
                return wrapper.safeGetExtensionName();
            }

            if (col == 3) {
                return (wrapper.getProject() != null) ? projectString : systemString;
            }

            // If I throw an exception all will stop. This instead keeps going
            return "getValueAt: ERROR at row=" + row + " col=" + col;
        }

        /**
         * Gets the columnClass attribute of the ExtensionsTableModel object
         *
         * @param  col  Description of the Parameter
         * @return      The columnClass value
         */
        public Class<?> getColumnClass(int col)
        {
            if (col == 0) {
                return new ImageIcon().getClass();
            }

            return new String().getClass();
        }
    }
}
