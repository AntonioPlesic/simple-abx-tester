package antonioplesic.simpleabxtester.audiotrackplayer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
public class VolumeBasedPlayer extends AudioTrkPlyr{
	
	//moguce i promjene
	final double bytesPerSecond = 44100*2*2;

	File file = null;
//	String filePath1 = "/sdcard/music/You Were Mine RAW PCM.raw";
//	String filePath2 = "/sdcard/music/Cowboy Take Me Away RAW PCM.raw";
	//String filePath2 = "/sdcard/music/You Were Mine RAW PCM.raw";
	AudioTrack audioTrack1;
	AudioTrack audioTrack2;
//	FileInputStream fileInputStream1 = null;
//	BufferedInputStream bufferedInputStream1 = null;
//	FileInputStream fileInputStream2 = null;
//	BufferedInputStream bufferedInputStream2 = null;

	int bufferSizeInBytes = 0;
	Thread thread1 = null;
	Thread thread2 = null;
	
	AtomicInteger bytesPosition1 = new AtomicInteger();

//	AtomicInteger bytesReadSoFar = new AtomicInteger();
//	AtomicInteger selectedTrack = new AtomicInteger();

	public VolumeBasedPlayer() {
		
//		selectedTrack.set(1);
//
//		bufferSizeInBytes = 4 * AudioTrack.getMinBufferSize(44100,
//				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
//
//		audioTrack1 = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
//				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
//				bufferSizeInBytes, AudioTrack.MODE_STREAM);
//
//		audioTrack2 = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
//				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
//				bufferSizeInBytes, AudioTrack.MODE_STREAM);
//
//		try {
//			bufferedInputStream1 = new BufferedInputStream(new FileInputStream(
//					filePath1));
//			bufferedInputStream2 = new BufferedInputStream(new FileInputStream(
//					filePath2));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		//removedLog.w("bufferSize", Integer.toString(bufferSizeInBytes));

	}

	private Runnable myRunnable1 = new Runnable() {

		@Override
		public void run() {

			while (true) {

				byte[] chunk = new byte[bufferSizeInBytes / 10];
				int bytesRead = 0;
				try {
					bytesRead = bufferedInputStream1.read(chunk);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				bytesPosition1.addAndGet(bytesRead);

				audioTrack1.write(chunk, 0, bytesRead);

				if (selectedTrack.get() == 1) {
					audioTrack1.setStereoVolume(1f, 1f);
				} else {
					audioTrack1.setStereoVolume(0f, 0f);
				}
			}
		}
	};

	private Runnable myRunnable2 = new Runnable() {

		@Override
		public void run() {

			while (true) {

				byte[] chunk = new byte[bufferSizeInBytes / 10];
				int bytesRead = 0;
				try {
					bytesRead = bufferedInputStream2.read(chunk);
				} catch (IOException e) {
					e.printStackTrace();
				}

				audioTrack2.write(chunk, 0, bytesRead);

				if (selectedTrack.get() == 2) {
					audioTrack2.setStereoVolume(1f, 1f);
				} else {
					audioTrack2.setStereoVolume(0f, 0f);
				}
			}
		}
	};

	public void play() {

		synchronized (this) {
			if (thread1 == null) {
				audioTrack1.play();
				audioTrack2.play();
				thread1 = new Thread(myRunnable1);
				thread2 = new Thread(myRunnable2);
				thread1.start();
				thread2.start();
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}
