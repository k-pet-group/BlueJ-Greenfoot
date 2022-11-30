/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2;

import javafx.scene.layout.Pane;

/**
 * Extensions which wish to add preference items to BlueJ's Tools/Preferences/Extensions 
 * panel should register an instance of PreferenceGenerator with the BlueJ proxy object.
 *
 * The PreferenceGenerator allows the creation of a {@link Pane} to contain
 * preference data, and the loading and saving of that data.
 */

/*
 * Author Damiano Bolla, University of Kent at Canterbury, January 2003
 */
 
public interface PreferenceGenerator
{
    /**
     * BlueJ will call this method to get the {@link Pane} where preferences for this
     * extension are. Preferences can be laid out as desired.
     *
     * @return    A {@link Pane} object the extension can add preference data into.
     */
    public Pane getWindow();


    /**
     * When this method is called the Extension should load its current values into
     * its preference panel.
     * This is called from the JavaFX (GUI) thread, so be quick.
     */
    public void loadValues();


    /**
     * When this method is called the Extension should save values from the preference panel into 
     * its internal state. Value checking can be performed at this point.
     * This is called from the JavaFX (GUI) thread, so be quick.
     */
    public void saveValues();
}

