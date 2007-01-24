package greenfoot.sound;

import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays sounds from a file or URL. Several sounds can be played at the same
 * time.
 * 
 * 
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundPlayer implements SimulationListener {
	/**
	 * Holds a list of all sounds currently playing. We use this list to stop
	 * the sounds.
	 */
	private List sounds = new ArrayList();

	// private Map sounds = new Hashtable();

	private static SoundPlayer instance;

	/**
	 * Only use clips when the size of the clip is below this value.
	 */
	private static int maxClipSize = 500 * 1000;

	private SoundPlayer() {
	}

	public synchronized static SoundPlayer getInstance() {
		if (instance == null) {
			instance = new SoundPlayer();
		}
		return instance;
	}

	/**
	 * Stops all sounds currently played by the soundplayer. Includes paused
	 * sounds. Stopped sounds can NOT be resumed.
	 * 
	 */
	public synchronized void stop() {
		for (Iterator iter = sounds.iterator(); iter.hasNext();) {
			Sound element = (Sound) iter.next();
			element.stop();
		}
		sounds.clear();
	}

	/**
	 * Pauses all sounds. Can be resumed.
	 * 
	 */
	public synchronized void pause() {
		for (Iterator iter = sounds.iterator(); iter.hasNext();) {
			Sound element = (Sound) iter.next();
			element.pause();
		}
	}

	/**
	 * Resumes paused sounds.
	 * 
	 */
	public synchronized void resume() {
		for (Iterator iter = sounds.iterator(); iter.hasNext();) {
			Sound element = (Sound) iter.next();
			element.resume();
		}
	}

	/**
	 * Plays the sound from file.
	 * 
	 * @param file
	 *            Name of a file or an url
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 */
	public void play(final String file) throws IOException,
			UnsupportedAudioFileException, LineUnavailableException {

		// First, determine the size of the sound, if possible
		URL url = GreenfootUtil.getURL(file, "sounds");
		int size = url.openConnection().getContentLength();
		if (size == -1 || size > maxClipSize) {
			// If we can not get the size, or if it is a big file we stream it
			// in a thread.
			final Sound sound = new SoundStream(url, SoundPlayer.this);
			sounds.add(sound);
			Thread t = new Thread() {
				public void run() {
					try {
						sound.play();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnsupportedAudioFileException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (LineUnavailableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			t.start();
		} else {
			// The sound is small enough to be loaded into memory as a clip.
			Sound sound = new SoundClip(url, SoundPlayer.this);
			sounds.add(sound);
			sound.play();

		}
	}

	/**
	 * Stop sounds when simulation is disabled (a new world is created). Pause
	 * sounds when simulation is paused. Resume when simulation is started.
	 */
	public void simulationChanged(SimulationEvent e) {
		//TODO Only stop when stop button is pressed - not at the end of an act.
		if (e.getType() == SimulationEvent.DISABLED) {
			stop();
		} else if (e.getType() == SimulationEvent.STOPPED) {
			pause();
		} else if (e.getType() == SimulationEvent.STARTED) {
			resume();
		}
	}

	/**
	 * Method that should be called by a soundsStream when it is finished
	 * playing.
	 */
	synchronized void soundFinished(Sound s) {
		sounds.remove(s);
	}
}
