package bluej.prefmgr;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;

import java.io.*;

import bluej.Config;
import bluej.utility.Debug;

/**
 * 
 * @author Andrew Patterson
 * @version $Id: PrefPanelListener.java 1418 2002-10-18 09:38:56Z mik $
 */
public interface PrefPanelListener
{
    void beginEditing();
    void revertEditing();
    void commitEditing();
}
