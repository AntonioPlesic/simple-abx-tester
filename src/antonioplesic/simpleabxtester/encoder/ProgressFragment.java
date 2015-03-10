package antonioplesic.simpleabxtester.encoder;




import android.app.Activity;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import antonioplesic.simpleabxtester.encoder.EncoderService.LocalBinder;

/**
 * Used for getting progress information from EncoderService. If activity uses EncoderService, 
 * it should also use this fragment. It is responsible for "tracking" the state of service, and showing
 * a progressDialog if necessary. No other action on part of the activity holding the fragment is necessary,
 * other than instantiating the fragment, TODO dovrsi JAVADOC
 * 
 * DOVRSI
 * DOVIRSI
 * DOVRSI
 * DOVRSI
 */
public class ProgressFragment extends Fragment {
	
	/**
	 * Activities that implement this interface get notified of job completion
	 */
	public interface OnJobCompletionListener{
		
		public void onJobCompletion();
		
	}
	
	//For tracking encoding/decoding service progress while activity is on s
	ProgressDialog progressDialog; //WARNING: be careful of possible leaks as constructor takes Context, which is supplied by getAtivity()
	
	//Receiver for receiving progress from encoding/decoding service (registered/unregistered in onStart()/onStop() )
	BroadcastReceiver receiver;
	
	//For notifiying activity on job completion
	OnJobCompletionListener jobCompletionListener;
		
	int progresDialogProgress = 0;
	String progresDialogMessage = "";
	
	static final String KEY_PROGRESS = "progressDialogProgress";
	static final String KEY_PROGRESS_MESSAGE =  "progressDialogMessage";
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		//removedLog.i(this.getClass().getName(),"onAttach");
		
		try {
			jobCompletionListener = (OnJobCompletionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnJobCompletionListener");
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		//removedLog.i(this.getClass().getName(),"onSaveInstanceState");
		
		outState.putInt(KEY_PROGRESS,progresDialogProgress);
		outState.putString(KEY_PROGRESS_MESSAGE, progresDialogMessage);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		//removedLog.i(this.getClass().getName(),"onActivityCreated");

		if(savedInstanceState!=null){
			
			//removedLog.i(this.getClass().getName(),"onActivityCreated,savedInstance != null");
			
			progresDialogProgress = savedInstanceState.getInt(KEY_PROGRESS);
			progresDialogMessage = savedInstanceState.getString(KEY_PROGRESS_MESSAGE);

			//There used to be a major leak here, caused by calling showProgressDialog.
			//So, don't call showProgressDialog here.

		}
	}
	

	private void showProgressDialog() {
		progressDialog = new ProgressDialog(getActivity()); //WARNING: This will leak activity unless dealt with in proper lifecycle methods
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setProgressNumberFormat(null);
		progressDialog.setIndeterminate(false);
//		progressDialog.setCancelable(false);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setMax(100);
		progressDialog.setMessage(progresDialogMessage);
		progressDialog.setProgress(0);
		progressDialog.setProgress(progresDialogProgress);
//		progressDialog.setTitle("");
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				
				//normally, dialogFragments are tied to activities
				//But since i'm managing dialog from this fragment completley
				//care must be taken not to leak activity
				if(progressDialog!=null)
					progressDialog.dismiss();
				progressDialog = null;
				
				stopEncodingService();
				
			}
		});
		progressDialog.show();
		progressDialog.setProgress(progresDialogProgress);
	}
	
	public void stopEncodingService(){
		Intent intent = new Intent(getActivity(), EncoderService.class);
		getActivity().stopService(intent);
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		
		//removedLog.i(this.getClass().getName(),"onStart");
		
		//register the receiver responsible for receiving and processing of progress reports shown in activity
		IntentFilter filter = new IntentFilter(EncoderService.Constants.ACTION_BROADCAST);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		//removedLog.i(this.getClass().getName(),"onResume");
		
		/*check if encoding service finished (and was started, i.e. running (do nothing if it was idle))
		 *  while the app was paused
		 */
		if(EncoderService.getJobStatus(getActivity()) == EncoderService.Constants.STATUS_DONE){
			
			/*TODO: job is done, and activity is notified, reset done flag of service 
			 * (currently included in  chooseAndLauncThester, not sure if that is the best idea)
			 */
//			EncoderService.resetServiceFlags(this);
			
			//removedLog.w(this.getClass().getName(), "Encoding finished while activity was inactive");
			
			//dismiss progress dialog if shown
			if(progressDialog != null){
				progressDialog.dismiss();
			}
			
			//also dismiss notification
			dismissNotifications();
			
			// notify activity of completion, upon which activity will likely launch testing activity			
			jobCompletionListener.onJobCompletion();
		}
		
		/* Check if service is running (its worker thread, to be more precise)
		 * This is achieved simply by binding to it. When bound,
		 * onServiceConnected gets called. There, Service is interrogated about
		 * its current status, and progress dialog shown if necessary.
		 */
		//Bind to service in order to check if its worker thread is running
		Intent intent = new Intent(this.getActivity(), EncoderService.class);
		getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		
		//removedLog.i(this.getClass().getName(), "Service binding initiated");
				
	}

	private void dismissNotifications() {
		NotificationManager notificationManager =  (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}
		
	@Override
	public void onStop() {
		super.onStop();
		
		//removedLog.i(this.getClass().getName(),"onStop");
		
		//normally, dialogFragments are tied to activities
		//But since i'm managing dialog from this fragment completley
		//care must be taken not to leak activity
		if(progressDialog!=null)
			progressDialog.dismiss();
		progressDialog = null;
		
		//unregister receiver while activity is away
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//removedLog.i(this.getClass().getName(),"onCreate");
		
		//instantiate a receiver that receives progress reports that should be shown in activity
		receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				//LONG JOB PROGRESS
				float progress = intent.getExtras().getFloat(EncoderService.Constants.EXTRA_LONG_JOB_PROGRESS);
				if (progress != 0.0f) {
					
					//removedLog.w(this.getClass().getName(), "Progress received: " + Float.toString(progress));
					
					if (progressDialog != null) {
						progresDialogMessage = "Long job";
						progresDialogProgress = (int) progress;
						updateProgressDialog();
					}
				}

				//ENCODING JOBS COMPLETED SUCESSFULLY
				if (intent.getExtras().getInt(EncoderService.Constants.EXTRA_STATUS) == EncoderService.Constants.STATUS_DONE) {
					
					//removedLog.w(this.getClass().getName(), "Received job completed message");
					
					progressDialog.dismiss();
					dismissNotifications();
					
					// notify activity of completion, upon which activity will likely launch testing activity	
					jobCompletionListener.onJobCompletion();
				}
				
				//TO RAW PROGRESS
				int rawProgress = intent.getExtras().getInt(EncoderService.Constants.EXTRA_TO_RAW_PROGRESS, -1);
				if (rawProgress != -1){
					//removedLog.w(this.getClass().getName() ,"Received \"to raw\" progress message");
					progresDialogProgress = rawProgress;
					progresDialogMessage = "Converting original audio file to PCM";
					if (progressDialog != null) {
						updateProgressDialog();	
					}	
				}
				
				//FLAC TO PCM PROGRESS
				int flacToPCMProgress = intent.getExtras().getInt(EncoderService.Constants.EXTRA_FLAC_TO_PCM_PROGRESS,-1);
				if(flacToPCMProgress != -1){
					//removedLog.w(this.getClass().getName(),"Received \"flac to pcm\" progress message");
					progresDialogProgress = flacToPCMProgress;
					progresDialogMessage = "Decoding FLAC to PCM";
					if(progressDialog != null){
						updateProgressDialog();
					}
				}
				
				//TO MP3 PROGRESS
				//TODO: pazi, usporedivanje floata s == !!!!
				int progressMp3 = intent.getExtras().getInt(EncoderService.Constants.EXTRA_TO_MP3_PROGRESS, -1);
				if (progressMp3 != -1){
					//removedLog.w(this.getClass().getName(),"Received mp3 encoding progress message");
					progresDialogProgress = progressMp3;
					progresDialogMessage = "Encoding to mp3";
					if(progressDialog != null){
						updateProgressDialog();
					}
				}
				
				//TO DECODED PROGRESS
				int decodedProgress = intent.getExtras().getInt(EncoderService.Constants.EXTRA_FROM_APP_ENCODED_MP3_PROGRESS, -1);
				if( decodedProgress != -1){
					//removedLog.w(this.getClass().getName(),"Received mp3 decoding progress message");
					progresDialogProgress = decodedProgress;
					progresDialogMessage = "Decoding mp3";
					if(progressDialog != null){
						updateProgressDialog();
					}
				}
				
				//SYNC PROGRESS
				int syncProgress = intent.getExtras().getInt(EncoderService.Constants.EXTRA_SYNCHRONIZATION_PROGRESS, -1);
				if(syncProgress != -1){
					//removedLog.w(this.getClass().getName(),"Primljen sync progress");
					progresDialogProgress = syncProgress;
					progresDialogMessage = "Synchronizing tracks";
					if(progressDialog != null){
						updateProgressDialog();
					}
				}
			}

			private void updateProgressDialog() {
				progressDialog.setMessage(progresDialogMessage);
				progressDialog.setProgress(progresDialogProgress);

			}

		};
	}
	
	/**Causes the ProgressFragment to show progress dialog. To be called just
	 * before the encoding intent is dispatched, else dialog will not show
	 * propertly.
	 * <p>
	 * Additionally, it might take some time before service broadcasts first
	 * progress intent, but the progress dialog should be shown immidiatley
	 * after the user selects an anction, in order to disable selecting other
	 * actions without firstly canceling current one, and to streamline the user
	 * experience.
	 */
	public void forceDialogShowing(){ //TODO: rename this method, maybe just to showProgressDialog()
		showProgressDialog();	
	}
		
	//=========================================================================
	// For checking if service (its worker thread) is started
	//=========================================================================
	
	EncoderService mService = null;

	ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {	}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			//removedLog.i(this.getClass().getName(),"Service connected: " + mService.toString());
			//Toast.makeText(getActivity(), "worker thread running: " + mService.isRunning(), Toast.LENGTH_SHORT).show();
			if(mService.isRunning()){
				showProgressDialog();
			}
			
			getActivity().unbindService(mConnection);
		}
	};
	
	

}
