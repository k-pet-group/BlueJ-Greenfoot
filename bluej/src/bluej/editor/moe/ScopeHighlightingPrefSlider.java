/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

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
package bluej.editor.moe;

import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bluej.prefmgr.PrefMgr;

/**
 * ScopeHighlightingPrefSlider is the slider that controls the 
 * strength of the scope highlighting colors
 * 
 * @author Marion Zalk
 *
 */
public class ScopeHighlightingPrefSlider extends JSlider implements ChangeListener{

    public static final int MIN=0;
    public static final int MAX=20;
    JSlider slider;

   /**
    * Constructor that sets up the look and feel for the scope highlighting colour slider
    */
 public ScopeHighlightingPrefSlider(){
        super(MIN, MAX);
        //set the transparency value from the prefMgr
        setValue(PrefMgr.getTransparency());
        //labels
        Hashtable<Integer, JLabel>labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(new Integer(MIN), new JLabel("Transparent"));
        labelTable.put(new Integer(MAX), new JLabel("Highlighted"));
        setLabelTable( labelTable );
        setPaintLabels(true);
        addChangeListener(this);
    }

    public void stateChanged(ChangeEvent e) {
        slider = (JSlider) e.getSource();
        BlueJSyntaxView.setStrength(slider.getValue());
    }
}
