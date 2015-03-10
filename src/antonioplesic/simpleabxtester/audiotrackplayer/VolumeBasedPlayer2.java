package antonioplesic.simpleabxtester.audiotrackplayer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Basic idea is to have 2 AudioTrack players running simultaneously, one of which
 * is always muted, to enable switching between tracks by selecting which one is not
 * muted. Results were not satisfactory, but that could be due to the bad 
 * implementation; it seems like it could work, even though it also seems a bit
 * naive.
 * @deprecated Abandoned due to SelectablePlayer working better.
 */
@Deprecated
public class VolumeBasedPlayer2 extends AudioTrkPlyr{


	AudioTrack audioTrack1;
	AudioTrack audioTrack2;

	Thread thread = null;

	
	AtomicInteger bytesPosition1 = new AtomicInteger();

	public VolumeBasedPlayer2() {

	}

	private Runnable myRunnable = new Runnable() {

		@Override
		public void run() {

			synchronized(lock){

				while (true) {

					byte[] chunk1 = new byte[bufferSizeInBytes / 10];
					int bytesRead1 = 0;
					try {
						bytesRead1 = bufferedInputStream1.read(chunk1);
					} catch (IOException e) {
						e.printStackTrace();
					}

					byte[] chunk2 = new byte[bufferSizeInBytes / 10];
					int bytesRead2 = 0;
					try {
						bytesRead2 = bufferedInputStream2.read(chunk2);
					} catch (IOException e) {
						e.printStackTrace();
					}

					if(pausedFlag.get()){
						try {
							//removedLog.w("pauziram dretvu", "pauziram dretvu");
							lock.wait();
							//removedLog.w("nastavljam dretvu", "nastavljam dretvu");

						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					//removedLog.w("chunkovi jednaki?:" +  Boolean.toString( (Arrays.toString(chunk1)).equals(Arrays.toString(chunk2)) ), " " + Boolean.toString( (Arrays.toString(chunk1)).equals(Arrays.toString(chunk2)) ));
					//removedLog.w("","bytes read: " + Integer.toString(bytesRead1) + " " + Integer.toString(bytesRead2));
					//					//removedLog.w("",Arrays.toString(chunk1));
					//					//removedLog.w("",Arrays.toString(chunk2));


					bytesPosition1.addAndGet(bytesRead1);

					////removedLog.w("pisem","pisem");
					audioTrack1.write(chunk1, 0, bytesRead1);
					audioTrack2.write(chunk2, 0, bytesRead2);

					//doznaj koji track je aktivan
					int akt = selectedTrack.get();
					
					//ugasi neaktivne
					if(akt==1){
						audioTrack2.setStereoVolume(0f, 0f);
					}
					if(akt==2){
						audioTrack1.setStereoVolume(0f, 0f);
					}

					//upali aktivne
					if( akt==1){
						audioTrack1.setStereoVolume(1f, 1f);
					}
					if( akt==2){
						audioTrack2.setStereoVolume(1f, 1f);
					}


					//					if (selectedTrack.get() == 1) {
					//						audioTrack1.setStereoVolume(1f, 1f);
					//					} else {
					//						audioTrack1.setStereoVolume(0f, 0f);
					//					}
					//					
					//					if (selectedTrack.get() == 2) {
					//						audioTrack2.setStereoVolume(1f, 1f);
					//					} else {
					//						audioTrack2.setStereoVolume(0f, 0f);
					//					}

					if(!pausedFlag.get()){
						audioTrack1.play();
						audioTrack2.play();
					}

				}
			}
		}
	};

	public void play() {

		if (this.thread == null) {
			synchronized (lock) {
				//removedLog.w("thread==null",Boolean.toString(thread==null));
				audioTrack1.play();
				audioTrack2.play();
				thread = new Thread(myRunnable);
				//removedLog.w("thread==null, nakon stvaranja",Boolean.toString(thread==null));
				thread.start();
			}
		}

		if (pausedFlag.get() == true){
			synchronized(lock){
				pausedFlag.set(false);
				lock.notify();
				//				audioTrack1.play();
				//				audioTrack2.play();
			}

		}


	}

	public void toggle() {

		if (selectedTrack.get() == 1) {
			selectedTrack.set(2);
		} else {
			selectedTrack.set(1);
		}
	}


	@Override
	public void prepare() {

		File file1 = new File(filePath1);
		File file2 = new File(filePath2);

		file1Length = file1.length();
		file2Length = file2.length();

		selectedTrack.set(1);

		bufferSizeInBytes = 4 * AudioTrack.getMinBufferSize(44100,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);

		audioTrack1 = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
				bufferSizeInBytes, AudioTrack.MODE_STREAM);

		audioTrack2 = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
				bufferSizeInBytes, AudioTrack.MODE_STREAM);

		try {
			bufferedInputStream1 = new BufferedInputStream(new FileInputStream(filePath1));
			bufferedInputStream2 = new BufferedInputStream(new FileInputStream(filePath2));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//removedLog.w("bufferSize", Integer.toString(bufferSizeInBytes));

	}

	@Override
	public synchronized void seekTo(int milis) {		
		int bytesToSkip = (int) ((milis/1000)*bytesPerSecond);

		try {
			//retardirano, a i sporo, nije instant seek
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

	}

	@Override
	public int getCurrentPosition() {
		return (int) ((bytesPosition1.get()/bytesPerSecond)*1000);
	}

	@Override
	public int getDuration() {
		return (int)((file1Length/bytesPerSecond)*1000);
	}

	@Override
	public void pause() {
		//		audioTrack1.pause();
		//		audioTrack2.pause();
		pausedFlag.set(true);

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}
