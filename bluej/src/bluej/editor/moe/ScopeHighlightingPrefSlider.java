package bluej.editor.moe;

import java.awt.Color;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bluej.prefmgr.PrefMgr;

/**
 * 
 * @author Marion Zalk
 *
 */
public class ScopeHighlightingPrefSlider extends JSlider implements ChangeListener{

    public static final int MIN=0;
    public static final int MAX=255;
    Color bg=new Color(245, 245, 253, PrefMgr.getTransparency());
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
        PrefMgr.setTransparency(slider.getValue());
    }
}
