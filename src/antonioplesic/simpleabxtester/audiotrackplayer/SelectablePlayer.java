package antonioplesic.simpleabxtester.audiotrackplayer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Works as follows:<br>
 * Two tracks can be played, T1 and T2. There is an integer that tells which one should be playing.
 * A loop is running in it's own thread, that basically looks like this:<br><br>
 * 
 * Loop:												<br>&nbsp;&nbsp;&nbsp;
 * 		//read equal amounts of both tracks				<br>&nbsp;&nbsp;&nbsp;
 * 		read part of T1;								<br>&nbsp;&nbsp;&nbsp;
 * 		read part of T2;								<br>&nbsp;&nbsp;&nbsp;
 * 		if(T1 should play)								<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 * 			feed read data T1 to AudioTrack;			<br>&nbsp;&nbsp;&nbsp;
 * 		else											<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 * 			feed read data T2 to AudioTrack;			<br>&nbsp;&nbsp;&nbsp;
 * 		Repeat loop										<br><br>
 * 
 * Since tracks are always equally read, there is no break in sync, and switching between tracks
 * is seamles. There is no silence, and no noise when switching, and that is guaranteed, so one
 * can not "cheat" during test by using cues as noise or silence duration, and is not distracted
 * by that same silence/noise. Of course, tracks must be in sync prior to start of playback, or else
 * there would be another audible tell of which is which: one would always be a few ms late due to
 * various encoder paddings. (yes, decoders/encoders should deal with padding by properly removing 
 * it, but using this app you can test even those encoder/decoder pairs that don't do it properly,
 * for instance, if you're trying to develop your own.)
 */
public class SelectablePlayer extends AudioTrkPlyr{

	SelectablePlayer thisPlayer = this; //so inner thread can easily return this (player) object without passing this object to runnable
	
	AudioTrack audioTrack;
	
	AtomicBoolean stop = new AtomicBoolean(false);
	
	private static int WTF = 2;

	Thread thread = null;

	AtomicInteger bytesPosition1 = new AtomicInteger();

	public SelectablePlayer() {
	}
	
	private Runnable myRunnable = new Runnable() {

		@Override
		public void run() {

			synchronized(lock){
				
				while(!stop.get()){
					
//					byte[] chunk1 = new byte[bufferSizeInBytes/10];
//					byte[] chunk2 = new byte[bufferSizeInBytes/10];
					
					//TODO: name WTF constant something meaningful, forgot what it stands for
					byte[] chunk1 = new byte[bufferSizeInBytes/WTF];
					byte[] chunk2 = new byte[bufferSizeInBytes/WTF];

					int bytesRead1 = 0;
					int bytesRead2 = 0;
					try {
						bytesRead1 = bufferedInputStream1.read(chunk1);
						bytesRead2 = bufferedInputStream2.read(chunk2);					
					} catch (IOException e) {
						e.printStackTrace();
					}

					if(pausedFlag.get()){
						try {
							//removedLog.w(this.getClass().getName(),"Pausing thread");
							lock.wait();
							//removedLog.w(this.getClass().getName(),"Resuming thread");
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					bytesPosition1.addAndGet( (bytesRead1<bytesRead2) ? bytesRead1 : bytesRead2 );
										
					//---------------------------------------------------------------------
					//if one of possible ends reached (marked section end, or actual end).
					//---------------------------------------------------------------------
					
					if( seekOnSectionEnd.get() && bytesPosition1.get() >= loopEndByte ){
						
						seekTo(loopStartMilis);
						
						if(!isLooping()){
							pause();
							completionListener.onCompletion(thisPlayer);
						}
						
					}else if(bytesPosition1.get() >= fileLength){
						
						seekTo(loopStartMilis);
						
						if(!isLooping()){
							pause();
							completionListener.onCompletion(thisPlayer);
						}
					}
					//----------------------------------------------------------------------
					

					if(bytesRead1 != bytesRead2){
						//removedLog.w(this.getClass().getName(),"procitano nejednako XXX:" + Integer.toString(bytesRead1) + " " + Integer.toString(bytesRead1));
//						throw new RuntimeException("procitano nejednako");
					}
					
					if(selectedTrack.get() == 1){
						audioTrack.write(chunk1, 0, bytesRead1);
					}
					else{
						audioTrack.write(chunk2, 0, bytesRead2);
					}
					
					if(!pausedFlag.get()){
						audioTrack.play();
					}
					
				}

				//thread is stopping, release resources
				//removedLog.w(this.getClass().getName(),"stopping player fragment thread, releasing resources");
				audioTrack.release();
			}
		}
	};

	public void play(){

		if(this.thread==null){
			synchronized(lock){
				audioTrack.play();
				thread=new Thread(myRunnable);
				thread.start();
			}
		}

		if(pausedFlag.get() == true){
			synchronized(lock){
				pausedFlag.set(false);
				lock.notify();
			}
		}

	}

	public void toggle(){

		if(selectedTrack.get()==1){
			selectedTrack.set(2);
		}
		else{
			selectedTrack.set(1);
		}
	}


	@Override
	public void prepare() {

		File file1 = new File(filePath1);
		File file2 = new File(filePath2);

		file1Length = file1.length();
		file2Length = file2.length();
		fileLength = (file1Length < file2Length) ? file1Length : file2Length;
		
		loopStartByte=0;
		loopStartMilis = bytePositionToMilis(loopStartByte);
		loopEndByte=fileLength;
		loopEndMilis = bytePositionToMilis(loopEndByte);
		
		selectedTrack.set(1);

		bufferSizeInBytes = WTF * AudioTrack.getMinBufferSize(44100,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);

		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
				bufferSizeInBytes, AudioTrack.MODE_STREAM);
		
		try {
			bufferedInputStream1 = new BufferedInputStream(new FileInputStream(filePath1));
			bufferedInputStream2 = new BufferedInputStream(new FileInputStream(filePath2));
			bufferedInputStream1.mark(0);
			bufferedInputStream2.mark(0);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//removedLog.w("bufferSize",Integer.toString(bufferSizeInBytes));		
	}

	@Override
	public synchronized void seekTo(int milis) {
		int bytesToSkip =  (int) ((milis/1000)*bytesPerSecond);
		
			/*if user seeks past the marked section, she is doing it for a reason.
			 * So seek to desired spot, but first disable seekOnSectionEndFlag.
			 * Else, user seeked before the end mark, so she expects the progress indicator to enter
			 * and stay inside section.
			 * 
			 * There is no need to set the seekOnSectionEnd to true when the real end is reached,
			 * because next seek is guaranteed to be before sectionEnd mark, which is captured by
			 * else clause.
			 */
			if(bytesToSkip>loopEndByte){
				seekOnSectionEnd.set(false);
			}else{
				seekOnSectionEnd.set(true);
			}

//			seeking = true;
						
			try {
//				synchronized (audioTrackLock) {
//				//pokusaj "flushanja" zaostalog outputa (obicni flush je "no op" ako je pauzirano)
//				AudioTrack oldAT = audioTrack;
//				oldAT.release();
//				audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
//						AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
//						bufferSizeInBytes, AudioTrack.MODE_STREAM);
//				if(isPlaying()){
//					audioTrack.play();
//				}}
				
				BufferedInputStream oldStream1 = bufferedInputStream1;
				BufferedInputStream oldStream2 = bufferedInputStream2;
				bufferedInputStream1 = new BufferedInputStream(new FileInputStream(filePath1));
				bufferedInputStream2 = new BufferedInputStream(new FileInputStream(filePath2));
				bufferedInputStream1.skip(bytesToSkip);
				bufferedInputStream2.skip(bytesToSkip);
				oldStream1.close();
				oldStream2.close();
				bytesPosition1.set(bytesToSkip);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

//			seeking = false;
	}

	@Override
	public int getCurrentPosition() {
		return (int) ((bytesPosition1.get()/bytesPerSecond)*1000);
	}

	@Override
	public int getDuration() {
		return (int)((fileLength/bytesPerSecond)*1000);
	}

	@Override
	public void pause() {
		pausedFlag.set(true);
	}

	@Override
	public void stop() {
		stop.set(true);	
	}
	
	//TODO: implement private seekToByte() instead
	private int bytePositionToMilis(long bytePosition){
		return (int)((bytePosition/bytesPerSecond)*1000);
	}

}
