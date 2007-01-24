package greenfoot.sound;

import greenfoot.util.GreenfootUtil;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * A sound that is loaded into memory before it is played.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundClip implements Sound, LineListener {
	private Clip soundClip;
	private SoundPlayer player;

	public SoundClip(URL url, SoundPlayer player) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		this.player = player;
		loadSound(url);
	}

	/**
	 * Load the sound file supplied by the parameter into this sound engine.
	 * 
	 * @return True if successful, false if the file could not be decoded.
	 * @throws LineUnavailableException 
	 * @throws IOException 
	 * @throws UnsupportedAudioFileException 
	 */
	private boolean loadSound(URL url) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		// currentSoundDuration = 0;

		AudioInputStream stream = AudioSystem.getAudioInputStream(url);
		AudioFormat format = stream.getFormat();

		// we cannot play ALAW/ULAW, so we convert them to PCM
		//
		if ((format.getEncoding() == AudioFormat.Encoding.ULAW)
				|| (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
			AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
					format.getSampleRate(), format.getSampleSizeInBits() * 2,
					format.getChannels(), format.getFrameSize() * 2, format
							.getFrameRate(), true);
			stream = AudioSystem.getAudioInputStream(tmp, stream);
			format = tmp;
		}
		DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(),
				((int) stream.getFrameLength() * format.getFrameSize()));

		soundClip = (Clip) AudioSystem.getLine(info);
		soundClip.open(stream);
		/*
		 * currentSoundFrameLength = (int) stream.getFrameLength();
		 * currentSoundDuration = (int) (soundClip.getBufferSize() /
		 * (soundClip.getFormat().getFrameSize() *
		 * soundClip.getFormat().getFrameRate()));
		 */
		soundClip.addLineListener(this);
		return true;

	}

	/**
	 * Stop this sound.
	 * 
	 */
	public void stop() {
		soundClip.stop();
		soundClip.setMicrosecondPosition(0);
	}

	/**
	 * Pause the song. Paused sounds can be resumed.
	 * 
	 */
	public void pause() {
		soundClip.stop();
	}

	/**
	 * Resume a paused sound.
	 * 
	 */
	public void resume() {
		soundClip.start();
	}

	/**
	 * Play this sound.
	 */
	public void play() {
		soundClip.setMicrosecondPosition(0);
		soundClip.start();
	}

	public void update(LineEvent event) {
		if(event.getType() == LineEvent.Type.STOP) {
			player.soundFinished(this);
		}
	}

}
