/*
 This file is part of the BlueJ program.
 Copyright (C) 2016,2017  Michael Kolling and John Rosenberg

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

import java.util.function.BiConsumer;

import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.KeyEvent;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * In SwingNode, there is a SwingKeyEventHandler class which translates
 * FX keypresses into Swing keypresses.  Unfortunately it has a bug,
 * wherein AltGr shortcuts on Windows (which are used frequently by
 * some non-English users) do not work correctly.  This is bug:
 *
 * https://bugs.openjdk.java.net/browse/JDK-8088471
 *
 * And currently has no fix date.  Thankfully, a commenter on the bug
 * pointed to a solution: overriding the handler to suppress modifiers
 * on a particular event.  This is implemented by setting a new key handler
 * in SwingNode; this class is a simple wrapper which sets the new handler;
 * the real fix is in SwingKeyEventHandlerFixed
 */
public class SwingNodeFixed extends SwingNode
{
    @OnThread(Tag.Any)
    public SwingNodeFixed()
    {
        // Defeat thread checker:
        ((BiConsumer<EventType<KeyEvent>, EventHandler<KeyEvent>>)(this::setEventHandler)).accept(javafx.scene.input.KeyEvent.ANY, new SwingKeyEventHandlerFixed(this));
        // Above is effectively:
        //
    	//     setEventHandler(javafx.scene.input.KeyEvent.ANY, new SwingKeyEventHandlerFixed(this));
        //
        // We are allowed to call FX methods if the component is not in a scene, which it can't be at this point.
        // However, the thread checker doesn't allow for this and flags an error; so for now we use the above
        // instead.
    }
}
