/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017  Michael Kolling and John Rosenberg
 
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
package bluej.groupwork.ui;

import bluej.Config;
import bluej.groupwork.TeamStatusInfo;
import bluej.pkgmgr.Project;

import java.util.List;

import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;


/**
 * Given a list of StatusEntry(s) returns a table model which allows them to
 * be edited in a JTable.
 *
 * A TableCell which either shows a label or a graphic.  They are wrapped
 * in a container to allow a border with padding to be applied.
 * 
 * 
 * @author Amjad Altadmri
 */
public /*static*/ class StatusTableCell extends TableCell<TeamStatusInfo, Object> //StringOrInteger>
    {
        private HBox container = new HBox(); // HBox so that we can use baseline-alignment
        private Label label = new Label();
        private ImageView objRefPic;
        private SimpleBooleanProperty showingLabel = new SimpleBooleanProperty(true);
        private SimpleBooleanProperty occupied = new SimpleBooleanProperty(true);

        final static private Image objectrefIcon = Config.getImageAsFXImage("image.inspector.objectref");/////


        protected final String resourceLabel = Config.getString("team.status.resource");
        protected final String remoteStatusLabel = Config.getString("team.status.remote");
        protected final String versionLabel = Config.getString("team.status.version");

        private final boolean isDVCS;

        protected Project project;
        protected String statusLabel;
        protected List<String> labelsList;
        protected ObservableList<TeamStatusInfo> resources;

        private final int COLUMN_COUNT = 3;


        /**
         *
         */
        public StatusTableCell(Project project)
        {
            this.project = project;
            this.isDVCS = project.getTeamSettingsController().isDVCS();

            objRefPic = new ImageView(objectrefIcon);
            container.getChildren().addAll(label, objRefPic);
            JavaFXUtil.addStyleClass(container, "team-status-value-wrapper");
            JavaFXUtil.addStyleClass(label, "team-status-value-label");
            objRefPic.managedProperty().bind(showingLabel.not());
            objRefPic.visibleProperty().bind(showingLabel.not());
            label.managedProperty().bind(showingLabel);
            label.visibleProperty().bind(showingLabel);
            container.visibleProperty().bind(occupied);
            setText("");
            setGraphic(container);
        }

        @Override
        protected void updateItem(Object v, boolean empty)
        {
            super.updateItem(v, empty);
            occupied.set(!empty);
            if (v != null && String.valueOf(v) == null)
            {
                showingLabel.set(false);
            }
            else
            {
                label.setText(v == null || empty ? "" : String.valueOf(v));
                showingLabel.set(true);
            }
        }



    /**
     * Return the number of rows in the table
     *
     * @return      the number of rows in the table
     */
    public int getRowCount()
    {
        return resources.size();
    }
    
    /**
     * Return the number of columns in the table
     *
     * @return      the number of columns in the table
     */
    public int getColumnCount()
    {
        return COLUMN_COUNT;
    }

    /**
     * Return the name of a particular column
     *
     * @param col   the column we are naming
     * @return      a string of the columns name
     */
    public String getColumnName(int col)
    {
        try {
            return labelsList.get(col);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("bad column number in StatusTableModel::getColumnName()");
        }
    }


    /**
     * Indicate that nothing is editable
     */
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

    /**
     * Set the table entry at a particular row and column (only
     * valid for the location column)
     *
     * @param   value   the Object at that location in the table
     * @param   row     the table row
     * @param   col     the table column
     */
    public void setValueAt(Object value, int row, int col)
    {
       // do nothing here
    }
    
    public void clear()
    {
        resources.clear();
//        fireTableDataChanged();
    }
    
    public void setStatusData(ObservableList<TeamStatusInfo> statusResources)
    {
        resources = statusResources;
//        fireTableDataChanged();
    }

    public ObservableList<TeamStatusInfo> getResources()
    {
        return resources;
    }


    /**
     * Find the table entry at a particular column for a specific row.
     *
     * @param   info    the table row info
     * @param   col     the table column number
     * @return          the Object at that location in the table
     */
    public Object getValueAt(TeamStatusInfo info, int col)
    {
        switch (col) {
            case 0:
                return ResourceDescriptor.getResource(project, info, false);
            case 1:
                return isDVCS ? info.getStatus() : info.getLocalVersion();
            case 2:
                return isDVCS ? info.getRemoteStatus() : info.getStatus();
            default:
                break;
        }

        return null;
    }
}