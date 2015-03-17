package antonioplesic.simpleabxtester.encoder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import antonioplesic.simpleabxtester.DataManager;
import antonioplesic.simpleabxtester.R;
import antonioplesic.simpleabxtester.SelfPreparedSourcesActivity;
import antonioplesic.simpleabxtester.StartScreenActivity;
import antonioplesic.simpleabxtester.DataManager.StorageUnaccessibleException;
import antonioplesic.simpleabxtester.settings.SettingsHelper;
import antonioplesic.simpleabxtester.wavextractor.WavParser;
import antonioplesic.simpleabxtester.wavextractor.WavParserBetter;



import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.renderscript.Sampler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class EncoderService extends IntentService {
	
	final static String TAG = "EncoderService";
	
	NotificationCompat.Builder builder;
	NotificationManager notificationManager;
	
	WakeLock wakeLock;
	volatile boolean runningFlag = false;
	
	DataManager dataManager;
	
	int jobType;
	
	int lastProg = -1;
	
	/*Encoder methods should periodically check whether they should stop executing,
	 * for example when user cancels the job (simply calling stopService will not
	 * stop the service's worker thread)
	 */
	AtomicBoolean stop;

	public EncoderService(){
		super("EncoderService");
	}
	
	//mislim da se nigde ne koristi
//	interface ProgressCallback{
//		void updateProgress(float progress);
//	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		dataManager = new DataManager(this);
	}


	//From tutorial:
	/** The IntentService calls this method from the default worker thread with
	 * the encoderServiceIntent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent incomingIntent) {
		
		if(incomingIntent.getExtras()==null){
			//no actual job is being started, intent came in order to check whether service is running
			//removedLog.i(this.getClass().getName(),"captured intent with null extras, no job started");
			return;
		}
		else{
			//some real job should start, i.e. worker thread will do actual job for some time, so set running flag
			runningFlag = true;
		}
		
		//get requested job type from intent
		this.jobType = incomingIntent.getExtras().getInt(Constants.EXTRA_TYPE);
		
		long serviceStartTime = System.currentTimeMillis();
		
		/*Encoding an related jobs can take quite some time. It is probable that the user
		 * will either want to do something else while encoding is taking place, or that 
		 * she will lock the device, i.e. turn off the screen. It is expected that encoding
		 * jobs continues even when the screen is turned off due to lock. To prevent device 
		 * from sleeping, a partial wake lock is used.
		 */
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "encoderwakelock");
		wakeLock.acquire();
				
		/*Encoder methods should periodically check whether they should stop executing,
		 * for example when user cancels the job (simply calling stopService will not
		 * stop the service's worker thread)
		 */
		stop = new AtomicBoolean(false);
		
		/*Save the fact that the job started, which an activity can poll upon resume
		 * to see whether the job is still going on, or finished etc.
		 * TODO: Bound services might be the more elegant solution, so activity could poll
		 * the service itself about the status.
		 */
		setJobStatus(Constants.STATUS_STARTED);
		//removedLog.w(TAG,Integer.toString(getJobStatus(this)));
		
		/*Notify activity that intended to start the service that the service actually started,
		 * so it could show progress and stuff. Activity only gets this message if it is resumed
		 * while the notification is sent. Otherwise, activity checks getJobStauts() in its onResume()
		 * (see previous comment) 
		 */
		notifyOfStarted();
		//removedLog.w(TAG, "poslao obavijet o pocetku");
		
		/*When the "app as a whole" is running "in the background" (activity stopped/paused, 
		 * but service going on!) user is notified of background execution via notifications.
		 */
		builder = new NotificationCompat.Builder(this)
//					.setSmallIcon(R.drawable.ic_dialog_dialer)
//					.setSmallIcon(R.drawable.stat_sys_download)
//					.setSmallIcon(android.R.drawable.stat_sys_download)
					.setSmallIcon(R.drawable.ic_notification_animation_alt)
//					.setTicker()
					.setContentTitle("Simple ABX Tester")
					/*TODO: ovo trenutno ne radi na <4.1, rijesi to
					 * zelim da app radi na 4.0+ */
					.setContentIntent(intentForGettingBackToActivity())
					.setContentText("pocetak")
					
//					//experimentiranje, izbrisi
//					.addAction(android.R.drawable.stat_notify_sync, "Cancel job", null)

					
					.setOngoing(true);
		
		//TODO: fix hardcoded notification id
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(1,builder.build());
//		notificationManager.notify(1,notification);
		
		builder.setContentText("converting original to pcm");
		notificationManager.notify(1, builder.build());
//		notificationManager.notify(1, notification);
		
		
		/*service should be in foreground, since user expects to do something more interesting while
		 * waiting for encoding to finish, and expects service to complete successfully while away from
		 * application (being in foreground greatly decreases the chances of service getting killed
		 * if user, while waiting for encoding to finish, starts some resource heavy task, such as a game).
		 */
		startForeground(1, builder.build());
		
		
		//start job based on requested type
		switch (jobType) {
		case Constants.JOB_NORMAL: 
			originalStuff(incomingIntent, serviceStartTime); break;
		case Constants.JOB_SELF_PREPARED:
			selfPrepared(incomingIntent, serviceStartTime); break;
		default:
			break;
		}
		
		
//		originalStuff(incomingIntent, serviceStartTime);
		
		
	}
	
	//=====================================================================
	// Encoding/decoding procedures
	//=====================================================================

	private void originalStuff(Intent incomingIntent, long serviceStartTime) {
		/*Start converting input file to raw pcm, catch the progress info via corresponding listener,
		 * and then notify the user: 
		 * 1) in activity if it is active (by sending local broadcast using broadcastToRawProgress())
		 * 2) in notification area
		 */
		
		//Obtain filepaths required for the encoding/decoding procedure
		
		//Get input file from starting intent
		File fajl = (File) incomingIntent.getExtras().getSerializable(Constants.EXTRA_FILE);
		//removedLog.w(TAG, "primio fajl " + fajl.getName());
		
		String sourcePCMPath = null; 	//Points to one of the two files being compared
		String mp3Path = null; 			//points to intermediary file
		String mp3DecodedPath = null; 	//Points to other one of the two files being compared
		
		try {
			sourcePCMPath = dataManager.getSourcePCMPath(fajl);
			mp3Path = dataManager.getMp3Path(fajl);
			mp3DecodedPath = dataManager.getMp3DecodedPath(fajl);
		} catch (StorageUnaccessibleException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//removedLog.e(this.getClass().getName(),"Stripped raw lossless file copied to: " + sourcePCMPath);
		//removedLog.e(this.getClass().getName(),"Mp3 saved to 						 : " + mp3Path);
		//removedLog.e(this.getClass().getName(),"Decoded from mp raw data saved to   : " + mp3DecodedPath);
				
		//---------------------------------------------------------------------
		// 1) Convert input file to raw PCM file
		//---------------------------------------------------------------------
		
		if(fajl.getName().endsWith(".raw")){
			rawToRaw(fajl);
		}
		else if(fajl.getName().endsWith(".wav")){
			wavToRaw(fajl, sourcePCMPath);
		}
		else if(fajl.getName().endsWith(".flac")){
			flacToRaw(fajl, sourcePCMPath);
		}
		else if(fajl.getName().endsWith(".mp3")){
			//TODO: transcoding not supported, only lossless sources allowed
		}
		else{
			//TODO: wrong file type ( only 16 bit/sample, dual channel, 44100 samples/s PCM, .wav, and .flac files supported)
		}
		
		if(stop.get()){
			//removedLog.w(TAG, "worker stopped while decoding flac");
			onJobCompletion(serviceStartTime);
			return;
		}
		
		//---------------------------------------------------------------------
		// 2) Encode raw PCM obtained in 1) to mp3
		//---------------------------------------------------------------------
		
		//removedLog.w(TAG,"pcm file:" + sourcePCMPath);
		//removedLog.w(TAG,"mp3 file:" + mp3Path);
		
		rawToMp3(sourcePCMPath, mp3Path);
		
		if(stop.get()){
			//removedLog.w(TAG,"worker stopped while encoding");
			onJobCompletion(serviceStartTime);
			return;
		}
		
		//---------------------------------------------------------------------
		// 3) Decode the mp3 obtained in 2) to raw PCM file
		//---------------------------------------------------------------------
		
		//removedLog.w(TAG,"mp3 file: " + mp3Path);
		//removedLog.w(TAG,"decoded file:" +mp3DecodedPath);
		
		mp3ToRaw(mp3Path, mp3DecodedPath);

		if (stop.get()) {
			//removedLog.w(TAG, "worker stopped while decoding");
			onJobCompletion(serviceStartTime);
			return;
		}
		
		//---------------------------------------------------------------------
		// 4) Delete mp3 so users could not misuse the app as an mp3 encoder
		//---------------------------------------------------------------------
		dataManager.deleteMp3IfRequired(mp3Path);
		
		
		//---------------------------------------------------------------------
		// 5) Synchronize files obtained in 1) and 3)
		//---------------------------------------------------------------------
		
		/*This step enables seamless switching between tracks, as different 
		 * encoders/decoders add different amount of padding as normal part of 
		 * their operation
		 */
		
		long startTime = syncTracks(sourcePCMPath, mp3DecodedPath);
		
		if(stop.get()){
			//removedLog.w(TAG,"worker stopped while encoding");
			onJobCompletion(serviceStartTime);
			return;
		}
		
		long endTime = System.currentTimeMillis();
		//removedLog.w(TAG,"trajanje sinkronizacije: " + Long.toString(endTime-startTime) + " ms");
		
		
		//---------------------------------------------------------------------
		// 6) Work needing to be done on encoding/decoding completion
		//---------------------------------------------------------------------
		
		endStuff();
		onJobCompletion(serviceStartTime);
	}
	
	
	
	
	private void selfPrepared(Intent incomingIntent, long serviceStartTime) {
		
		//---------------------------------------------------------------------
		// 1) Decoding file1
		//---------------------------------------------------------------------
		File fajl1 = (File) incomingIntent.getExtras().getSerializable(Constants.EXTRA_FILE1);
		//removedLog.w("kurac, kurac, kurac", (fajl1==null)?"null":fajl1.toString());
		
		String path1 = null;
		try {
			path1 = dataManager.getSelfPreparedPath1(fajl1);
		} catch (StorageUnaccessibleException e) {
			e.printStackTrace();
		}
		
		decodeSelfPrepared(fajl1,path1);
		
		if(stop.get()){
			//removedLog.w(TAG, "worker stopped while decoding file 1");
			onJobCompletion(serviceStartTime);
			return;
		}
		
		//---------------------------------------------------------------------
		// 2) Decoding file2
		//---------------------------------------------------------------------
		File fajl2 = (File) incomingIntent.getExtras().getSerializable(Constants.EXTRA_FILE2);
		
		String path2 = null;
		try {
			path2 = dataManager.getSelfPreparedPath2(fajl2);
		} catch (StorageUnaccessibleException e) {
			e.printStackTrace();
		}
		
		decodeSelfPrepared(fajl2,path2);
		
		if(stop.get()){
			//removedLog.w(TAG, "worker stopped while decoding file 2");
			onJobCompletion(serviceStartTime);
			return;
		}
		
		//---------------------------------------------------------------------
		// 3) Synchronize (if requested)
		//---------------------------------------------------------------------
		boolean synchronize = incomingIntent.getExtras().getBoolean(Constants.EXTRA_SYNCHRONIZE);
		if(synchronize){
			syncTracks(path1, path2);
		}
		
		//---------------------------------------------------------------------
		// 5) Work needing to be done on encoding/decoding completion
		//---------------------------------------------------------------------
		endStuff();
		onJobCompletion(serviceStartTime);
	}

	private void decodeSelfPrepared(File fajl,String destPath) {
		//get path where decoded file1 will be saved		
		if(fajl.getName().endsWith(".raw")){
			rawToRaw(fajl, destPath);
		}
		else if(fajl.getName().endsWith(".wav")){
			wavToRaw(fajl, destPath);
		}
		else if(fajl.getName().endsWith(".flac")){
			flacToRaw(fajl, destPath);
		}
		else if(fajl.getName().endsWith(".mp3")){
			mp3ToRaw(fajl.getAbsolutePath(), destPath); //TODO: all other take File except this one takes String
		}
		else{
			//TODO: wrong file type ( only 16 bit/sample, dual channel, 44100 samples/s PCM, .wav, and .flac files supported)
		}
	}

	private long syncTracks(String sourcePCMPath, String mp3DecodedPath) {
		/*TODO: ovaj dio koda uzrokuje puno garbage collectiona, ukupnog trajanja cca 2 s
		 * To i nije puno kad se u obzir uzme da enkodiranja traje po pol minute, no ruzno izgleda u loggeru,
		 * treba vidjeti jel se da kako poboljsati trimmer, da reusa memoriju i sl.
		 * Inace, za ocekivati je da ovo traja, ipak tu ima nekoliko tisuca racunanja snage signala po sinkronizaciji.
		 */
		/*TODO: hardkodirani 2000,6000, to nije dobro, a nece ni radit za krace fajlove
		 * Dodatni problem su i tihe skladbe, usporedbu treba obaviti na dovoljno glasnim dijelovima
		 */
		long startTime = System.currentTimeMillis();
		
		SettingsHelper settingsHelper = new SettingsHelper(this);
		
		TracksSynchronizer t = new TracksSynchronizer(2, 16);
		try {
			t.loadSamples(sourcePCMPath,
							mp3DecodedPath,
							500000,
//							10000); *** 
							settingsHelper.getSyncWindowLength() * 2 ); /*** why times two? 
							TODO: analyze code, find out why this must be multiplied by two, and comment it,
							so you don't end up in this situation again -.- */ 
//			int samplesToTrim =  t.getSynchronizationOffsetOLD(5000);
//			int samplesToTrim =  t.getSynchronizationOffset(5000, ***
			int samplesToTrim = t.getSynchronizationOffset(settingsHelper.getSyncWindowLength(),
			
			new TracksSynchronizer.onSyncronizationProgressUpdateListener() {
				
				@Override
				public void progressUpdate(float progress) {
					
					broadcastSynchronizationProgress((float) (100*progress));
					
					builder.setProgress(100, (int) (100*progress), false).setContentText("Synchronizing tracks");
					notificationManager.notify(1, builder.build());
					
				}
			},
				new TracksSynchronizer.StopExectutionCallback() {
					
					@Override
					public boolean stopExecution() {
						return stop.get();
					}
				}
					
					);
			//removedLog.i(this.getClass().getName(),"samples to trimm: " + samplesToTrim);
			if(samplesToTrim!=0){
				if(samplesToTrim>0){
					t.trimmStart(mp3DecodedPath, samplesToTrim);
				}
				else{
					t.trimmStart(sourcePCMPath, -1*samplesToTrim);
				}
				//removedLog.w(TAG,"trimming complete");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return startTime;
	}

	private void mp3ToRaw(String mp3Path, String mp3DecodedPath) {
		new mp3EncoderDecoder().selfContainedDecodeFile(mp3Path, mp3DecodedPath,
				new mp3EncoderDecoder.OnDecodeProgressUpdateListener() {

					@Override
					public void updateProgress(float progress) {

						int prog = (int) (100 * progress);
						if (prog != lastProg) {

							broadcastFromEncodedMp3Progress((int) (100 * progress));

							builder.setProgress(100, (int) (100 * progress),
									false).setContentText("Decoding mp3");
							notificationManager.notify(1, builder.build());

							lastProg = prog;
						}
					}
				}, new mp3EncoderDecoder.StopExectutionCallback() {

					@Override
					public boolean stopExecution() {
						return stop.get();
					}
				});
	}

	private void rawToMp3(String sourcePCMPath, String mp3Path) {
		new mp3EncoderDecoder().selfContainedEncodeFile(this, sourcePCMPath, mp3Path, new mp3EncoderDecoder.OnEncoderProgressUpdateListener() {
			
			@Override
			public void updateProgress(float progress) {
				
				broadcastToMp3Progress((int) (100*progress));
				
				builder.setProgress(100, (int) (100*progress), false).setContentText("Encoding mp3");
				notificationManager.notify(1, builder.build());
			}
		}, 
		new mp3EncoderDecoder.StopExectutionCallback() {
			
			@Override
			public boolean stopExecution() {
				return stop.get();
			}
		});
	}

	private void flacToRaw(File fajl, String sourcePCMPath) {
		new flacDecoder().decodeFLAC(fajl.getAbsolutePath(), sourcePCMPath, 
				
				new flacDecoder.OnDecoderProgressUpdateListener() {
			
					@Override
					public void updateProgress(float progress) {

						int prog = (int) (100 * progress);
						if (prog != lastProg) {

							broadcastFlacDecodingProgress((int) (100 * progress));

							builder.setProgress(100,
									(int) (100 * progress), false)
									.setContentText("Decoding flac");
							notificationManager.notify(1, builder.build());

							lastProg = prog;
						}

					}
				}, new flacDecoder.StopExectutionCallback() {

					@Override
					public boolean stopExecution() {
						return stop.get();
					}
				}

		);
	}

//	private void wavToRaw(File fajl, String sourcePCMPath) {
//		broadcastToRawProgress(0);
//		try {
//			WavParser.extractRawDataFromWav(fajl.getAbsolutePath(), sourcePCMPath, new WavParser.OnWavToRawExtractionProgressUpdateListener() {
//
//				@Override
//				public void updateProgress(float progress) {
//					broadcastToRawProgress((int) (100*progress));
//				}
//			},
//			
//			new WavParser.StopExectutionCallback() {
//				
//				@Override
//				public boolean stopExecution() {
//					return stop.get();
//				}
//			});
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	private void wavToRaw(File fajl, String sourcePCMPath){
		broadcastToRawProgress(0);
		
		try {
			WavParserBetter.extractRawDataFromWav(fajl.getAbsolutePath(), sourcePCMPath, new WavParserBetter.OnWavToRawExtractionProgressUpdateListener() {
				
				@Override
				public void updateProgress(float progress) {
					broadcastToRawProgress((int) (100*progress));		
				}
			}, 
			
			new WavParserBetter.StopExectutionCallback() {
				
				@Override
				public boolean stopExecution() {
					return stop.get();
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
//TODO: next two methods are nearly almost the same, make first one call the second one
	
	private void rawToRaw(File fajl) {
		broadcastToRawProgress(0);
		try {
			dataManager.toRaw(fajl,new DataManager.OnToRawProgressUpdateListener() {

				@Override
				public void updateProgress(float progress) {
					broadcastToRawProgress((int) (100*progress));
				}
			});
		} catch (StorageUnaccessibleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void rawToRaw(File fajl, String destPath) {
		broadcastToRawProgress(0);
		try {
			dataManager.toRaw(fajl, destPath, new DataManager.OnToRawProgressUpdateListener() {

				@Override
				public void updateProgress(float progress) {
					broadcastToRawProgress((int) (100*progress));
				}
			});
		} catch (StorageUnaccessibleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void onJobCompletion(long serviceStartTime) {
		long serviceEndTime = System.currentTimeMillis();
		
		stopForeground(false);
		if(wakeLock.isHeld())
			wakeLock.release();
		
		if(stop.get() == true){
			notifyOfCancel();
			notificationManager.cancelAll();
			//removedLog.w(TAG, "poslao obavijest o cancelu");
		}
		else{
			notifyOfCompletion();
			//removedLog.w(TAG, "poslao obavijest o zavrsetku");
			builder.setProgress(0, 0, false);
			builder.setContentText("Test preparation completed (" + (serviceEndTime -serviceStartTime)/1000 + " s)");
//			builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
			builder.setSmallIcon(R.drawable.ic_notification_progress_0_alt);
			builder.setOngoing(false);
			notificationManager.notify(2, builder.build());
		}
		
		runningFlag = false;
	}
	
	@Override
	public void onDestroy() {
		
		if(getJobStatus(this) == Constants.STATUS_STARTED){
			//service zavrsio prije nego sto je sve napravljeno, vjerojatno canceliran od strane korisnika
			//removedLog.w(TAG,"service prekinut, no worker nastavlja raditi");
			
			if(stop!=null)
				stop.set(true);
			
			setJobStatus(Constants.STATUS_CANCELED);
		}
		else{
			//removedLog.w(TAG,"service zavrsen normalno");
		}
		
		/*Why is null check required here? Because of unbind. Unbind calls onDestroy, 
		* but wakelock is not instantiated until the work thread starts.
		* In this application, this service can be started for 2 reasons:
		* 1) to do actual encoding job
		* 2) to check wheter there is ongoing job
		* Case 2 is special because it is only a short instantation of service...
		* TODO: finish this explanation
		
		*/
		if(wakeLock!=null && wakeLock.isHeld()){
			wakeLock.release();
		}
		
		super.onDestroy();
	}

	private void endStuff() {
		
		if(stop.get() == true){
			setJobStatus(Constants.STATUS_IDLE);
			//removedLog.w(TAG,"Dugi posao prekinut");
		}
		else{
			//removedLog.w(TAG,"Dugi posao zavrsen normalno");
			setJobStatus(Constants.STATUS_DONE);
		}
		
		//removedLog.w(TAG,Integer.toString(getJobStatus(this)));
	}
	
	public final class Constants{
		
		//Defines a custom Intent action
		public static final String ACTION_BROADCAST = 
				"antonioplesic.simpleabxtester.encoder.BROADCAST";
		
		//Defines the key for the status "extra" in an Intent
		public static final String EXTRA_STATUS = 
				"antonioplesic.simpleabxtester.encoder.STATUS";
		
		public static final int STATUS_IDLE = 1;
		public static final int STATUS_DONE = 2;
		public static final int STATUS_STARTED = 3;
		public static final int STATUS_CANCELED = 4;
		
		public static final int JOB_NORMAL = 10;
		public static final int JOB_SELF_PREPARED = 11;
		
		public static final String EXTRA_LONG_JOB_PROGRESS = 
				"antonioplesic.simpleabxtester.encoder.PROGRESS";
		
		public static final String EXTRA_TYPE = 
				"antonioplesic.simpleabxtester.encoder.JOB_TYPE";
		
		public static final String EXTRA_FILE = 
				"antonioplesic.simpleabxtester.encoder.FILE";
		
		public static final String EXTRA_FILE1 =
				"antonioplesic.simpleabxtester.encoder.FILE1";
		
		public static final String EXTRA_FILE2 =
				"antonioplesic.simpleabxtester.encoder.FILE2";
		
		public static final String EXTRA_SYNCHRONIZE =
				"antonioplesic.simpleabxtester.encoder.SYNCHRONIZE";
		
		public static final String EXTRA_TO_RAW_PROGRESS = 
				"antonioplesic.simpleabxtester.encoder.TO_RAW_PROGRESS";
		
		public static final String EXTRA_FLAC_TO_PCM_PROGRESS = 
				"antonioplesic.simpleabxtester.encoder.FLAC_TO_PCM_PROGRESS";
				
		
		public static final String EXTRA_TO_MP3_PROGRESS =
				"antonioplesic.simpleabxtester.encoder.TO_MP3_PROGRESS";
		
		public static final String EXTRA_FROM_APP_ENCODED_MP3_PROGRESS = 
				"antonioplesic.simpleabxtester.encoder.FROM_APP_ENCODED_MP3_PROGRESS";
		
		public static final String EXTRA_SYNCHRONIZATION_PROGRESS = 
				"antonioplesic.simpleabxtester.encoder.SYNCHRONIZATION_PROGRESS";
		
//		public static final String EXTRA_REQUEST = 
//				"antonioplesic.simpleabxtester.encoder.REQUEST_STATUS";
//		
//		public static final int REQUEST_STATUS = 1;
	}
	
	private void notifyOfCompletion(){
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_STATUS, Constants.STATUS_DONE);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	private void notifyOfCancel(){
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_STATUS, Constants.STATUS_CANCELED);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	private void notifyOfStarted(){
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_STATUS, Constants.STATUS_STARTED);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	private void broadcastProgress(float progress){
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_LONG_JOB_PROGRESS, progress);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	private void broadcastToRawProgress(float progress){
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_TO_RAW_PROGRESS, (int) progress);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	protected void broadcastFlacDecodingProgress(float progress) {
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_FLAC_TO_PCM_PROGRESS, (int) progress);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	private void broadcastToMp3Progress(float progress){
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_TO_MP3_PROGRESS, (int) progress);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	private void broadcastFromEncodedMp3Progress(float progress){
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_FROM_APP_ENCODED_MP3_PROGRESS, (int) progress);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	private void broadcastSynchronizationProgress(float progress){
		Intent intent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_SYNCHRONIZATION_PROGRESS, (int) progress);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
//	public static void requestStatus(Context context){
//		Intent encoderServiceIntent = new Intent(Constants.ACTION_BROADCAST).putExtra(Constants.EXTRA_LONG_JOB_PROGRESS,0);
//		LocalBroadcastManager.getInstance(context).sendBroadcast(encoderServiceIntent);
//	}
	
	private void setJobStatus(int value){
		//removedLog.w(TAG,"spremam status posla");
		SharedPreferences pref = this.getSharedPreferences("service_status", 0);
		Editor editor = pref.edit();
		editor.putInt(Constants.EXTRA_STATUS, value);
		editor.commit();
	}
	
	public static void resetServiceFlags(Context context){
		//removedLog.w(TAG,"resetiram status zastavicu");
		SharedPreferences pref = context.getSharedPreferences("service_status", 0);
		Editor editor = pref.edit();
		editor.putInt(Constants.EXTRA_STATUS, Constants.STATUS_IDLE );
		editor.commit();
	}
	
	public static int getJobStatus(Context context){
		//removedLog.w(TAG,"loadam status posla");
		SharedPreferences pref = context.getSharedPreferences("service_status", 0);
		return pref.getInt(Constants.EXTRA_STATUS,-1);
	}
	
	private PendingIntent intentForGettingBackToActivity(){

		Intent toLaunch;
		
		switch (this.jobType) {
		case Constants.JOB_NORMAL:
			toLaunch = new Intent(this,StartScreenActivity.class);
			break;
		case Constants.JOB_SELF_PREPARED:
			toLaunch = new Intent(this,SelfPreparedSourcesActivity.class);
			break;
		default:
			toLaunch = new Intent(this,StartScreenActivity.class);
			break;
		}
		
		toLaunch.setAction(Intent.ACTION_MAIN);
		toLaunch.addCategory(Intent.CATEGORY_LAUNCHER);
		
		PendingIntent intent = PendingIntent.getActivity(this, 0 , toLaunch, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return intent;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//=========================================================================
	// Bound service functionality, used only to see if service (more precisely, 
	// its worker thread) is running
	//=========================================================================
	
	private final IBinder mBinder = new LocalBinder();
	
	public class LocalBinder extends Binder{
		
		EncoderService getService(){
			return EncoderService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	/** Returns whether the worker thread nested in service is running/doing meaningful job. 
	 * Does not return whether the service itself is running. 
	 * 
	 * @return Whether the worker thread nested in EncoderService is running
	 */
	boolean isRunning(){
		return runningFlag;
	}	
	
}



























