/*
 This file is part of the BlueJ program.
 Copyright (C) 2016  Michael Kolling and John Rosenberg

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

import bluej.utility.Debug;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import sun.swing.JLightweightFrame;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

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
 * on a particular event.
 *
 * So this class is a near-copy of SwingNode.SwingKeyEventHandler,
 * with that suggested fix.  Since some of the fields and classes we are
 * using are private or package-private, we have to use reflection
 * to access them.  Not nice, but I can't see any other way to do it.
 */
@OnThread(Tag.FXPlatform)
public class SwingKeyEventHandlerFixed implements EventHandler<KeyEvent>
{
    private final SwingNode swingNode;
    private Field lwFrameField;

    @OnThread(Tag.Any)
    public SwingKeyEventHandlerFixed(SwingNode swingNode)
    {
        this.swingNode = swingNode;
        try
        {
            lwFrameField = SwingNode.class.getDeclaredField("lwFrame");
            lwFrameField.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(javafx.scene.input.KeyEvent event)
    {
        try
        {
            handleSub(event);
        }
        catch (Exception e)
        {
            Debug.reportError(e);
        }
    }

    public void handleSub(javafx.scene.input.KeyEvent event) throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException
    {
        JLightweightFrame frame = (JLightweightFrame) lwFrameField.get(swingNode);
        if (frame == null) {
            return;
        }
        if (event.getCharacter().isEmpty()) {
            // TODO: should we post an "empty" character?
            return;
        }
        // Don't let Arrows, Tab, Shift+Tab traverse focus out.
        if (event.getCode() == KeyCode.LEFT  ||
            event.getCode() == KeyCode.RIGHT ||
            event.getCode() == KeyCode.UP ||
            event.getCode() == KeyCode.DOWN ||
            event.getCode() == KeyCode.TAB)
        {
            event.consume();
        }

        Method fxKeyEventTypeToKeyID = Class.forName("javafx.embed.swing.SwingEvents").getDeclaredMethod("fxKeyEventTypeToKeyID", javafx.scene.input.KeyEvent.class);
        fxKeyEventTypeToKeyID.setAccessible(true);

        int swingID = (Integer)fxKeyEventTypeToKeyID.invoke(null, event);
        if (swingID < 0) {
            return;
        }

        Method fxKeyModsToKeyMods = Class.forName("javafx.embed.swing.SwingEvents").getDeclaredMethod("fxKeyModsToKeyMods", javafx.scene.input.KeyEvent.class);
        fxKeyModsToKeyMods.setAccessible(true);


        int swingModifiers = (Integer)fxKeyModsToKeyMods.invoke(null, event);
        int swingKeyCode = event.getCode().impl_getCode();
        char swingChar = event.getCharacter().charAt(0);

        // A workaround. Some swing L&F's process mnemonics on KEY_PRESSED,
        // for which swing provides a keychar. Extracting it from the text.
        if (event.getEventType() == javafx.scene.input.KeyEvent.KEY_PRESSED) {
            String text = event.getText();
            if (text.length() == 1) {
                swingChar = text.charAt(0);
            }
        }
        // NCCB: this is the fix added.  AltGr appears as Control+Alt, so
        // we just suppress those modifiers on a key-typed event
        if (event.getEventType() == KeyEvent.KEY_TYPED && event.isAltDown() && event.isControlDown())
        {
            swingModifiers = 0;
        }

        long swingWhen = System.currentTimeMillis();
        java.awt.event.KeyEvent keyEvent = new java.awt.event.KeyEvent(
            frame, swingID, swingWhen, swingModifiers,
            swingKeyCode, swingChar);
        AccessController.doPrivileged(new PostEventAction(keyEvent));
    }

    private static class PostEventAction implements PrivilegedAction<Void>
    {
        private AWTEvent event;
        public PostEventAction(AWTEvent event) {
            this.event = event;
        }
        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public Void run() {
            EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
            eq.postEvent(event);
            return null;
        }
    }
}
