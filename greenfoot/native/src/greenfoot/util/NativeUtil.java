package greenfoot.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.win32.W32APIOptions;

/**
 * Created by neil on 23/06/2016.
 */
public class NativeUtil
{
    public interface User32Ext extends User32
    {
        User32Ext INSTANCE = (User32Ext) Native.loadLibrary("user32",
            User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        public BOOL AllowSetForegroundWindow(DWORD dwProcessId);
    }

    // Called from Greenfoot to allow BlueJ windows (the editor window) to take focus.
    public static void allowForegroundWindowsOS(long processId)
    {
        // We must reference the Kernel32 class in order to load it and perform
        // the native initialisation:
        @SuppressWarnings("unused") Kernel32 kernel32 = Kernel32.INSTANCE;
        User32Ext user32 = User32Ext.INSTANCE;
        user32.AllowSetForegroundWindow(new DWORD(processId));
    }
}
