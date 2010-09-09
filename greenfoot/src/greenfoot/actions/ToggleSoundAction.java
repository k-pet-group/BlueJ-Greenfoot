package greenfoot.actions;

import java.awt.event.ActionEvent;

import greenfoot.core.GProject;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.soundrecorder.SoundRecorderDialog;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;

public class ToggleSoundAction extends ToggleAction 
{
	private GreenfootFrame frame;
	private boolean state;

	public ToggleSoundAction(String title, GProject project, GreenfootFrame frame) {
		super(title, project);
		this.frame = frame;
	}

	public void actionPerformed(ActionEvent e)
	{
		new SoundRecorderDialog(frame, project, this);   
	}
	
	@Override
	public ButtonModel getToggleModel() 
	{
		return new JToggleButton.ToggleButtonModel() {
			
		    /**
		     * Returns whether or not the sound recorder window is currently visible.
		     */
			@Override
		    public boolean isSelected()
		    {
		    	return state;
		    }

		    /**
		     * @param b	Set the state to this value
		     */
			@Override
		    public void setSelected(boolean b)
		    {
				state = b;
		    }
		};
	}
	
	public void setOpen() {
		state = true;
	}

	public void setClosed() {
		state = false;
	}


}
