package bluej.guibuilder;

import java.awt.event.KeyEvent;

/**
 * Container for ShortcutPairs. These pairs contain the raw keycode and a string
 * representation of the keycode.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 * @see java.awt.event.KeyEvent
 */
public class GUIMenuShortcuts
{
    /**
     * An array of ShortcutPairs
     */
    public static final ShortcutPair[] shortcuts = {
	new ShortcutPair(KeyEvent.VK_0, "0"),
	new ShortcutPair(KeyEvent.VK_1, "1"),
	new ShortcutPair(KeyEvent.VK_2, "2"),
	new ShortcutPair(KeyEvent.VK_3, "3"),
	new ShortcutPair(KeyEvent.VK_4, "4"),
	new ShortcutPair(KeyEvent.VK_5, "5"),
	new ShortcutPair(KeyEvent.VK_6, "6"),
	new ShortcutPair(KeyEvent.VK_7, "7"),
	new ShortcutPair(KeyEvent.VK_8, "8"),
	new ShortcutPair(KeyEvent.VK_9, "9"),
	new ShortcutPair(KeyEvent.VK_A, "A"),
	new ShortcutPair(KeyEvent.VK_B, "B"),
	new ShortcutPair(KeyEvent.VK_C, "C"),
	new ShortcutPair(KeyEvent.VK_D, "D"),
	new ShortcutPair(KeyEvent.VK_E, "E"),
	new ShortcutPair(KeyEvent.VK_F, "F"),
	new ShortcutPair(KeyEvent.VK_G, "G"),
	new ShortcutPair(KeyEvent.VK_H, "H"),
	new ShortcutPair(KeyEvent.VK_I, "I"),
	new ShortcutPair(KeyEvent.VK_J, "J"),
	new ShortcutPair(KeyEvent.VK_K, "K"),
	new ShortcutPair(KeyEvent.VK_L, "L"),
	new ShortcutPair(KeyEvent.VK_M, "M"),
	new ShortcutPair(KeyEvent.VK_N, "N"),
	new ShortcutPair(KeyEvent.VK_O, "O"),
	new ShortcutPair(KeyEvent.VK_P, "P"),
	new ShortcutPair(KeyEvent.VK_Q, "Q"),
	new ShortcutPair(KeyEvent.VK_R, "R"),
	new ShortcutPair(KeyEvent.VK_S, "S"),
	new ShortcutPair(KeyEvent.VK_T, "T"),
	new ShortcutPair(KeyEvent.VK_U, "U"),
	new ShortcutPair(KeyEvent.VK_V, "V"),
	new ShortcutPair(KeyEvent.VK_W, "W"),
	new ShortcutPair(KeyEvent.VK_X, "X"),
	new ShortcutPair(KeyEvent.VK_Y, "Y"),
	new ShortcutPair(KeyEvent.VK_Z, "Z"),
	new ShortcutPair(KeyEvent.VK_F1, "F1"),
	new ShortcutPair(KeyEvent.VK_F2, "F2"),
	new ShortcutPair(KeyEvent.VK_F3, "F3"),
	new ShortcutPair(KeyEvent.VK_F4, "F4"),
	new ShortcutPair(KeyEvent.VK_F5, "F5"),
	new ShortcutPair(KeyEvent.VK_F6, "F6"),
	new ShortcutPair(KeyEvent.VK_F7, "F7"),
	new ShortcutPair(KeyEvent.VK_F8, "F8"),
	new ShortcutPair(KeyEvent.VK_F9, "F9"),
	new ShortcutPair(KeyEvent.VK_F10, "F10"),
	new ShortcutPair(KeyEvent.VK_F11, "F11"),
	new ShortcutPair(KeyEvent.VK_F12, "F12")};
}



/**
 * A structure for storing a keycode and a corresponding label together.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 */
class ShortcutPair
{
    /**
     * The raw keycode.
     */
    public int keycode;

    /**
     * A human readable string representation of the keycode.
     */
    public String label;


    /**
     * Constructs a new ShortcutPair.
     *
     * @param keycode	The keycode
     * @param label	A string representing the keycode
     */
    ShortcutPair (int keycode, String label)
    {
	this.keycode = keycode;
	this.label = label;
    }
}
