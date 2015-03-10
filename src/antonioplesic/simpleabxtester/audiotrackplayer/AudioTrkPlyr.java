package antonioplesic.simpleabxtester.audiotrackplayer;

import java.io.BufferedInputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for AudioTrack based players.
 * 
 * During development, I experimented with 2 different approaches to track switching. Those
 * shared the same interface that resulted in this abstract class. However, SelectablePlayer
 * subclass prevailed, and this abstract class is largely unneeded now, and it is unlikely 
 * that I will continue development of other class. There is a possibility that other
 * way of doing track switching, volume based one, would provide faster switching between
 * tracks, but I had trouble implementing it. There is also a possibility of going to lover
 * levels for playing/switching audio, via OpengGL ES. Current performance is satisfactory, 
 * but the fact that it can surely be improved is worth noting.
 * 
 * Subclasses VolumeBasedPlayer and VolumeBasedPlayer2 are effectively abandoned, I decided to
 * keep them in project to document the basic idea behind them, if I ever decide to work on 
 * them again.
 * 
 * Only properly functioning subclass is SelectablePlayer.
 * 
 */
public abstract class AudioTrkPlyr {
	
	Object lock = new Object();
	
	//bytesPerSecond = sample rate [Hz] * (bitsPerSample [bits] / 8 [bits/byte]) * noOfChannels
	//TODO: constant for now, not in the future, support for different sample rates and resolutions planned
	final double bytesPerSecond = 44100*2*2;
	
	AtomicBoolean pausedFlag = new AtomicBoolean(true);
	
	AtomicBoolean loopingFlag = new AtomicBoolean();
	AtomicBoolean loopingSectionFlag = new AtomicBoolean();
	AtomicBoolean seekOnSectionEnd = new AtomicBoolean(true); /*TODO: should this actually be volatile?
	* I think not, as playback thread can call seek too. Yet not in the same time as other threads, as seek is
	* synchronized. Confused... */
	int loopStartMilis;
	int loopEndMilis;
	long loopStartByte;
	long loopEndByte;
	
	int bufferSizeInBytes = 0;
	
	String filePath1;
	String filePath2;
	
	long file1Length;
	long file2Length;
	long fileLength;
	
	AtomicInteger bytesReadSoFar = new AtomicInteger();
	AtomicInteger selectedTrack = new AtomicInteger();
	
	public abstract int getCurrentPosition();
	
	public abstract int getDuration();
	
	public abstract void play();
	
	public abstract void pause();
	
	/***Stops playback and releases all resources, such as playback thread.
	 * 
	 */
	public abstract void stop();
	
	public abstract void toggle();
	
	public abstract void seekTo(int milis);
	
	public boolean isPlaying() {
		return !pausedFlag.get();
	}
	
	public synchronized void selectTrack(int trackNo){
		selectedTrack.set(trackNo);
	}
	
	public void setDataSources(String path1, String path2){
		this.filePath1 = path1;
		this.filePath2 = path2;
	}
	
	public abstract void prepare();
		
//	FileInputStream fileInputStream1 = null;
//	FileInputStream fileInputStream2 = null;
	
	BufferedInputStream bufferedInputStream1 = null;
	BufferedInputStream bufferedInputStream2 = null;
	
	public void setLooping(boolean looping){
		loopingFlag.set(looping);
	}
	
	public boolean isLooping(){
		return loopingFlag.get();
	}
	
	public void setLoopingSection(boolean loopingSection){
		loopingSectionFlag.set(loopingSection);
	}
	
	public boolean isLoopingSection(){		
		return loopingSectionFlag.get();
	}
	
	public void setLoopingSectionStart(int milis){
		loopStartMilis = milis;
		loopStartByte = (int) ((bytesPerSecond/1000)*milis);	
	}
	
	public void setLoopingSectionEnd(int milis){
		loopEndMilis = milis;
		loopEndByte = (int) ((bytesPerSecond/1000)*milis);		
	}
	
	public interface OnCompletionListener{
		public void onCompletion(AudioTrkPlyr player);
	}
	
	OnCompletionListener completionListener = null;
	
	public void setOnCompletionListener(OnCompletionListener listener){
		this.completionListener = listener;
	}
	

}
