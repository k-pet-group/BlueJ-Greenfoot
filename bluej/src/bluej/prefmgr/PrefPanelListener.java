package bluej.prefmgr;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.Vector;

import bluej.Config;
import bluej.utility.Debug;

/**
 * 
 * @author Andrew Patterson
 * @version $Id: PrefPanelListener.java 262 1999-09-28 10:45:36Z ajp $
 */
public interface PrefPanelListener
{
    void beginEditing();
    void revertEditing();
    void commitEditing();
}
