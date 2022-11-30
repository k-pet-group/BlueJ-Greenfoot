/*
 This file is part of the BlueJ program.
 Copyright (C) 2017 Michael Kölling and John Rosenberg

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
package bluej.utility.javafx;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * A version of ScrollPane which overrides the default behaviour of capturing
 * focus when clicked.  This scroll pane cannot be focused.
 */
public class UnfocusableScrollPane extends ScrollPane
{
    /**
     * Creates the scroll pane with the given content.
     */
    public UnfocusableScrollPane(Node content)
    {
        super(content);
    }

    @Override
    public void requestFocus()
    {
        // Override behaviour to do nothing: this pane is not focusable
    }
}
