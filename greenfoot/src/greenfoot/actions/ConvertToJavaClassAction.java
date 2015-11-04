/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2015  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.actions;

import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.BlueJTheme;
import bluej.Config;
import javax.swing.JOptionPane;


/**
 * An action to convert a Stride class to a Java class.
 * 
 * @author Amjad Altadmri
 */
public class ConvertToJavaClassAction extends AbstractAction
{
    private ClassView cls;
    private JFrame frame;

    private static String confirmConvertTitle = Config.getString("convert.to.java.confirm.title");
    private static String confirmConvertText1 = Config.getString("convert.to.java.confirm.text1");
    private static String confirmConvertText2 = Config.getString("convert.to.java.confirm.text2");
    private static String confirmConvertText3 = Config.getString("convert.to.java.confirm.text3");
    
    public ConvertToJavaClassAction(ClassView view, JFrame frame)
    {
        super(Config.getString("convert.to.java.class"));
        this.cls = view;
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (confirmConvertClass(cls, frame)) {
            cls.removeStrideFileOnly();
        }
    }
    
    public static boolean confirmConvertClass(ClassView cls, JFrame frame)
    {
        String[] options = new String[] { Config.getString("convert.to.java.class"), BlueJTheme.getCancelLabel() };
        int convertToJava = 0;
        int response = JOptionPane.showOptionDialog(frame,
                confirmConvertText1 + " " + cls.getClassName() + " " + confirmConvertText2 + "\n" + confirmConvertText3,
                confirmConvertTitle,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        return response == convertToJava;
    }
}
