/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.sound;

import java.io.IOException;
import java.net.URL;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

/**
 * Plays sound from a MIDI file.
 * 
 * TODO: maybe keep it open for a while, instead of closing as soon as playback
 * has stopped.
 * 
 * @author Poul Henriksen
 * 
 */
public class MidiFileSound implements Sound
{
	private void printDebug(String s)
	{
		System.out.println(s);
	}

	private URL url;
	private SoundPlaybackListener playbackListener;
	private Sequencer sequencer;
	private Synthesizer synthesizer;
	private Sequence sequence;
	private boolean pause = false;

	public MidiFileSound(final URL url, SoundPlaybackListener listener)
	{
		this.url = url;
		playbackListener = listener;
		try {
			/*
			 * We read in the MIDI file to a Sequence object. This object is set
			 * at the Sequencer later.
			 */
			sequence = MidiSystem.getSequence(url);

			/*
			 * Now, we need a Sequencer to play the sequence. Here, we simply
			 * request the default sequencer without an implicitly connected
			 * synthesizer
			 */
			sequencer = MidiSystem.getSequencer(false);

			synthesizer = MidiSystem.getSynthesizer();

			/*
			 * To free system resources, it is recommended to close the
			 * synthesizer and sequencer properly.
			 * 
			 * To accomplish this, we register a Listener to the Sequencer. It
			 * is called when there are "meta" events. Meta event 47 is end of
			 * track.
			 * 
			 * Thanks to Espen Riskedal for finding this trick.
			 */
			sequencer.addMetaEventListener(new MetaEventListener() {
				public void meta(MetaMessage event)
				{
					if (event.getType() == 47) {
						close();
					}
				}
			});

		} catch (InvalidMidiDataException e) {
			SoundExceptionHandler.handleInvalidMidiDataException(e, url
					.toString());
		} catch (IOException e) {
			SoundExceptionHandler.handleIOException(e, url.toString());
		} catch (MidiUnavailableException e) {
			SoundExceptionHandler.handleLineUnavailableException(e);
		}
	}

	public synchronized void play()
	{
		sequencer.setLoopCount(0);

		if (!isPlaying()) {
			startPlayback();
		}
	}

	/**
	 * Start playback if it hasn't been started yet.
	 */
	private synchronized void startPlayback()
	{
		try {
			pause = false;
			
			if (!sequencer.isOpen()) {
				sequencer.setSequence(sequence);
				Receiver synthReceiver = synthesizer.getReceiver();
				Transmitter seqTransmitter = sequencer.getTransmitter();
				seqTransmitter.setReceiver(synthReceiver);
				/*
				 * The Sequencer is still a dead object. We have to open() it to
				 * become live. This is necessary to allocate some resources in
				 * the native part.
				 */
				sequencer.open();
				synthesizer.open();
			}

			sequencer.start();
			playbackListener.playbackStarted(this);
		} catch (SecurityException e) {
			SoundExceptionHandler.handleSecurityException(e, url.toString());
		} catch (MidiUnavailableException e) {
			SoundExceptionHandler.handleLineUnavailableException(e);
		} catch (InvalidMidiDataException e) {
			SoundExceptionHandler.handleInvalidMidiDataException(e, url
					.toString());
		}
	}

	public synchronized void loop()
	{
		sequencer.setLoopStartPoint(0);
		sequencer.setLoopEndPoint(-1);
		sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
		startPlayback();
	}

	public synchronized void resume()
	{
		pause = false;
		sequencer.start();
		playbackListener.playbackStarted(this);
	}

	public synchronized void pause()
	{
		pause = true;
		sequencer.stop();
		playbackListener.playbackPaused(this);
	}

	public synchronized void stop()
	{
		pause = false;
		close();
	}

	public synchronized void close()
	{
        playbackListener.playbackStopped(this);
		pause = false;
		printDebug(" playback ended: " + url);
		if (sequencer != null) {
			sequencer.close();
		}
		if (synthesizer != null) {
			synthesizer.close();
		}
        playbackListener.soundClosed(this);
	}

	public synchronized boolean isPaused()
	{
		return pause;
	}

	public synchronized boolean isPlaying()
	{
		return sequencer.isRunning();
	}

	public synchronized boolean isStopped()
	{
		return !isPaused() && !isPlaying();
	}
}
