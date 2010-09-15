package greenfoot.actions;

import greenfoot.core.GProject;
import greenfoot.gui.soundrecorder.SoundRecorderControls;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;

public class ToggleSoundAction extends ToggleAction 
{
	private SoundRecorderControls recorder;
	
    public ToggleSoundAction(String title, GProject project) {
		super(title, project);
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
		    	return recorder != null && recorder.isVisible();
		    }

		    /**
		     * @param b	Set the state to this value
		     */
			@Override
		    public void setSelected(boolean b)
		    {
			    if (b) {
			        if (recorder == null) {
			            recorder = new SoundRecorderControls(project);
			        }
			        recorder.setVisible(true);
			    } else if (recorder != null) {
				    recorder.closeAndStopRecording();
			    }
		    }
		};
	}
}
