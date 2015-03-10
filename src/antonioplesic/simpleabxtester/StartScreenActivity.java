package antonioplesic.simpleabxtester;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import antonioplesic.simpleabxtester.R;
import antonioplesic.simpleabxtester.AlreadyEncodedDialogFragment.AlreadyEncodedDialogListener;
import antonioplesic.simpleabxtester.DataManager.StorageUnaccessibleException;
import antonioplesic.simpleabxtester.EncodingDialogFragment.EncodingDialogListener;
import antonioplesic.simpleabxtester.dummytester.DummyABXYTester;
import antonioplesic.simpleabxtester.encoder.EncoderService;
import antonioplesic.simpleabxtester.encoder.ProgressFragment;
import antonioplesic.simpleabxtester.filepicker.FilePickerActivity;
import antonioplesic.simpleabxtester.lifted.StorageUtils;
import antonioplesic.simpleabxtester.lifted.StorageUtils.StorageInfo;
import antonioplesic.simpleabxtester.player.PlayerActivity;
import antonioplesic.simpleabxtester.settings.SettingsActivity;
import antonioplesic.simpleabxtester.settings.SettingsHelper;
import antonioplesic.simpleabxtester.wavextractor.WavInfoBetter;
import antonioplesic.simpleabxtester.wavextractor.WavParserBetter;



import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class StartScreenActivity extends BaseActivity implements
		EncodingDialogListener, AlreadyEncodedDialogListener,  ProgressFragment.OnJobCompletionListener {
	
	//=========================================================================
	// View fields
	//=========================================================================
	
	//Buttons
	Button newABXYTestButton;
	Button newABXTestButton;
	Button encoderSettingsButton;
	Button newSelfPreparedTestButton;

	ClickGuard clickGuard = new ClickGuard();	//for disabling double/multiple clicks at same time
	
	//Convenience buttons for various debugging/experimentation purposes
	Button debugButton; 
	Button debugButtonAnother;
	Button yetAnotherDebugButton;
	

	//=========================================================================
	// Input and progress tracking fields
	//=========================================================================
	
	//These fields are saved/restored on orientation change	
	File fajl = null; //File pointing to original lossless file that serves as basis for testing
	int selectedMultiStageAction = 0; //Selected test variant (ABXY, ABX, "self-prepared ABXY", "self-prepared ABX" ...)
	
	
	//=========================================================================
	// DataManager field
	//=========================================================================
	DataManager dataManager = null;
	

	//=========================================================================
	// Constants
	//=========================================================================
	
	//for saving/restoring fields on configuration changes etc.
	static final String SELECTED_FILE = "fajl";
	static final String SELECTED_TEST_TYPE = "selectedMultiStageAction";

	private static final int PICKFILE_RESULT_CODE = 1;

	//for determining selected test variant
	private static final int NEW_ABXY_TEST_REQUEST = 2;
	private static final int NEW_ABX_TEST_REQUEST = 3;
	private static final int NO_REQUEST = 4;

	private static final int SETTINGS_CHANGED_IN_DIALOG = 5;
	
	
	//=========================================================================
	// Methods
	//=========================================================================
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {

		outState.putSerializable(SELECTED_FILE, fajl);
		outState.putInt(SELECTED_TEST_TYPE, selectedMultiStageAction);

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

		super.onRestoreInstanceState(savedInstanceState);
		
		fajl = (File) savedInstanceState.getSerializable(SELECTED_FILE);
		selectedMultiStageAction = savedInstanceState.getInt(SELECTED_TEST_TYPE);

	}


	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		//removedLog.i(this.getClass().getName(),"OnResume");

		
		//removedLog.e(this.getClass().getName(),"Reseting click guard");
		clickGuard.reset();
			
	}

	



	@Override
	protected void onStop() {
		super.onStop();
		
		//removedLog.i(this.getClass().getName(),"ONSTOP");
	}
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.start_screen_activity);
		
		//instantiate a DataManager
		dataManager = new DataManager(this);
		
		//attach ProgressFragment
		FragmentManager fragmentManager = getFragmentManager();
		if(fragmentManager.findFragmentByTag("progressFragment")==null){
			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
			fragmentTransaction.add(new ProgressFragment(), "progressFragment");
			fragmentTransaction.commit();
		}
		


		//=====================================================================
		//  Preferences related stuff
		//=====================================================================
		
		// load settings as this activity is the main entry point into the
		// application
		PreferenceManager.setDefaultValues(this, R.xml.enkoder_settings, false);
		PreferenceManager.setDefaultValues(this, R.xml.data_settings, false);

		
		//=====================================================================
		// Views setup
		//=====================================================================
				
		debugButtonAnother = (Button) findViewById(R.id.debugAnother);
		debugButtonAnother.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
//				launchMyPicker();
				launchLayoutTesterActivity();
				
			}
		});
		
		newABXYTestButton = (Button) findViewById(R.id.btnABXYBlindTest);
		newABXYTestButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(clickGuard.passed()){
					selectedMultiStageAction = NEW_ABXY_TEST_REQUEST;
	//				launchExternalFilePicker();
					launchMyPicker();
				}
				
			}
		});

		newABXTestButton = (Button) findViewById(R.id.btnABXBlindTest);
		newABXTestButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(clickGuard.passed()){
					selectedMultiStageAction = NEW_ABX_TEST_REQUEST;
	//				launchExternalFilePicker();
					launchMyPicker();
				}
			}
		});
		
		newSelfPreparedTestButton = (Button) findViewById(R.id.btnSelfPreparedTest);
		newSelfPreparedTestButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(clickGuard.passed()){
					launchSelfPreparedFilesTestActivity();
				}
				
			}
		});

		encoderSettingsButton = (Button) findViewById(R.id.btnEncoderSettings);
		encoderSettingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(clickGuard.passed()){
					launchSettingsActivity();
				}
			}
		});
		
		debugButton = (Button) findViewById(R.id.buttonC_ABX);
		debugButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				debug2();
			}
		});
		
		yetAnotherDebugButton = (Button) findViewById(R.id.yetAnotherDebug);
		yetAnotherDebugButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				internalFiles();
				
			}
		});
		
	}
	
	//=========================================================================
	// Preventing quick/multiple button presses for launching activities
	//=========================================================================
	/*XXX: there must be some platform provided way of doing this, or some more
	 * elegant way (handling all clicks via the same interface). Something about
	 * declaring launch mode in the manifest for activities is suggested on the 
	 * Internet.
	 * 
	 * General idea: before doing any meaningful action, button should check
	 * if another uncompatible action is already being exectuded. Since I prefer
	 * annonymus button click listeners, it would look something like this:
	 * 
	 * someButton.setOnClickListener(new OnClickListener() {
	 *		@Override
	 *		public void onClick(View v) {
	 *			if(passesClickGuard()){
	 *				someAction();
	 *			}
	 *		}
	 *	});
	 * 
	 * ClickGuard should be reset in onResume();
	 *  
	 */
	
	/**
	 * For handling clicks in quick succession, or multiple clicks: most activities
	 * shouldn't be opened twice, or more different ones launched at the same time.
	 * If a button passes click guard, all other clicks (that check the guard) are
	 * disabled until resetClickGuard() is called.
	 * See XXX in code;
	 * @return True if button should proceed with action, false otherwise;
	 */
//	boolean passesClickGuard(){
//		if(clickGuard==false){
//			clickGuard=true;
//			return true;
//		}
//		else{
//			return false;
//		}
//	}
	
	/**
	 * Resets the click guard to enable buttons that check the click guard to be 
	 * clicked again.
	 */
//	private void resetClickGuard() {
//		clickGuard = false;
//	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.start_screen, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			launchSettingsActivity();
//			return true;
//		}
//		if (id == R.id.start_menu_About) {
//			launchAboutActivity();
//			return true;
//		}
//		if (id == R.id.start_menu_Help) {
//			launchHelpActivity();
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}

	@Override
	protected void onDestroy() {

		//if user is exiting application...
		if(isFinishing()){
			//...notify service that it should gracefully stop itself if running
			stopEncodingService();
		}
		
		super.onDestroy();
	}
	
	public void launchExternalFilePicker() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//		intent.setType("file/*");
		intent.setType("*/*"); 
//		intent.setType("audio/flac");
		
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, PICKFILE_RESULT_CODE);
	}
	
	protected void launchMyPicker() {
		Intent intent = new Intent(this, FilePickerActivity.class);
//		startActivity(intent);
		startActivityForResult(intent, PICKFILE_RESULT_CODE);	
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		String resultStringForDebugging;
		switch (resultCode) {
		case Activity.RESULT_OK:
			resultStringForDebugging="Activity.RESULT_OK";
			break;
		case Activity.RESULT_CANCELED:
			resultStringForDebugging="Activity.RESULT_CANCELED";
			break;
		default:
			resultStringForDebugging="Some other code: " + resultCode;
			break;
		}
		//removedLog.w(this.getClass().getName(), "onActivityResult invoked, result: " + resultStringForDebugging);
		
		//=====================================================================
		// On file chooser result
		//=====================================================================
		
		if ((resultCode == Activity.RESULT_OK && requestCode == PICKFILE_RESULT_CODE)) {
			
			//removedLog.w("vratio sam se iz file pickera", "vratio sam se iz fajl pickera");
			//removedLog.w("file",data.getData().toString());
			//removedLog.w("file", data.getData().getPath());

			// File fajl = new File(data.getData().getPath());
			fajl = new File(data.getData().getPath());
			//removedLog.w("apsolutni", fajl.getAbsolutePath());
			
			boolean isLocalFile = fajl.exists();
			
			//removedLog.w(this.getClass().getName(),"Je li rezultat odabira vec fajl?: " + isLocalFile );
			//removedLog.w("filename", fajl.getName().toString());
			
			
			/* "If" is the only case that currently gets executed, "else" is unreachable on purpose
			 * for now. Idea behind the code under the else block was to enable use of external file pickers.
			 * Initially, that was supposed to be the only way of picking files, i.e. there were no plans 
			 * to provide built in file picker, but there were several problems with that approach from the
			 * user interaction perspective:
			 * 
			 * 1) Using device's built in file explorer.
			 *	   a) 	Some devices don't have in built file explorer (encountered on some old Sony-Erricsson).
			 *	   b) 	Some devices (Samsungs) have great file explorer, but are launched by nonstandard and undocumented intents (AFAIK).
			 *			I only encountered this on Samsungs, yet, I didn't really try many different manufacturers. However possibility exists, 
			 *			as proved with Samsungs, that other manufacturers also use some *exotic ways of launching file explorers. Should I 
			 *			customize and test for each possible manufacturer/device. Obviously no. 
			 *			*By exotic launching methods I mean those different from one implemented in launchExternalFilePicker().
			 *
			 * 2) Using one of popular explorers from play store
			 * 	 a.I)	Scenario: user installs this ABX tester app, tries loading a file, sees that she has to install additional software
			 * 		  	to make it work => interest lost.
			 * 	 a.II)  Even worse scenario: user installs this ABX tester app on a Samsung phone, sees that she has to install additional
			 * 			file explorer app despite having perfectly functioning one already built in by default => user is pissed, I for sure would be
			 *   b)		This is a really minor reason, but still: I wasn't able to find a really good way of forcing user to install a third party app,
			 *   		but I'm aware that I didn't look thoroughly. Also, I didn't want to force user to install a specific file explorer, but to
			 *   		enable choosing among all of them that statisfy my criteria for suitability.
			 *   
			 * Also worth mentioning is the ability to select directories, which not all file explorers have (at least not when launched like
			 * in launchExternalFilePicker()).
			 * 
			 * As seen by now unreachable code under "else" block, while developing the app I actually used an external file explorer, but gave up on
			 * that approach for the reasons above, although it is the way I would prefer it to be. One of the reasons for that preferencs is that way
			 * network available files could be reached (tried only with google drive though). 
			 * 
			 *  In conclusion:
			 *   1) 	Made my own file explorer
			 *   2) 	Had to keep it simple, no network sources etc.
			 */
			
			if(isLocalFile){
				if(testFileSupported(fajl)){
					launchOneOfTheTests(fajl);
				}
				
			}
			else{
				ContentResolver resolver =  getContentResolver();
				String type = resolver.getType(data.getData());
				
				if(type.matches("audio/flac|audio/x-wav")){
					//removedLog.w(this.getClass().getName(),"format " + type +  " podrzan");
										
					String name;
					long size;
					
					Cursor cursor = resolver.query(data.getData(),null,null,null,null);
					cursor.moveToFirst();
					name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
					size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
					//removedLog.w(this.getClass().getName(), "ime fajla: " + name);
					//removedLog.w(this.getClass().getName(), "file size: " + size);
					cursor.close();
					
					try {
						File copiedFajl = dataManager.copyFromStream(resolver.openInputStream(data.getData()), name);
						//removedLog.w(this.getClass().getName(), "vracen fajl: "+ copiedFajl.getAbsolutePath() );
						fajl = copiedFajl;
						launchOneOfTheTests(fajl);
						
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (StorageUnaccessibleException e) {
						showDataFolderInaccessibleMessage();
					}
				}
				else{
					//removedLog.w(this.getClass().getName(),"format " + type +  " NIJE podrzan");
				}
				
				//removedLog.w(this.getClass().getName(),"FORMAT: " + type);
			}
		}
		
		//=====================================================================
		// On return from settings screen that was launched from an 
		// encoding dialog
		//=====================================================================

		if (resultCode == Activity.RESULT_OK 
		&& requestCode == SETTINGS_CHANGED_IN_DIALOG) {

			//removedLog.w(this.getClass().getName(),"vratio se iz settingsa pokrenutih u dialogu");

			// relaunch appropriate dialog
			launcEncodingDialogs(fajl);

		}

	}

	private boolean testFileSupported(File fajl) {
		//TODO: make more thorough tests
		
		//make sure file selected is not from the working directory, that would cause weird errors
		try {
			if(fajl.getAbsolutePath().startsWith(dataManager.getStorageDirectoryPath())){
				Toast.makeText(this, "Can't select file from wokring directory", Toast.LENGTH_LONG).show();
				return false;
			}
		} catch (StorageUnaccessibleException e) {
			return false;
		}
		
		
		
		String filename = fajl.getPath();
		
		if(filename.endsWith(".mp3")){
			/* No need to check specifics, mp3's are outright unsupported here, as I do not want to support
			 * transcoding for now. If user knowingly wants to ABX test transcoded files, one can do so in the 
			 * self prepared mode.
			 * TODO: enable transcoding, but show splash screens to inform the user about the file being 
			 * transcoded and why it is a bad practice, generally speaking.
			 * Ultimately, user should be able to see for herself, through ABX testing, that transcoding is bad.
			 */
			Toast.makeText(this, "mp3 files not supported, source must be in lossless format",Toast.LENGTH_LONG).show();
			return false;
		}
		else if(filename.endsWith(".flac")){
			return checkFlac(fajl);
		}
		else if(filename.endsWith(".wav")){
			return checkWav(fajl);
		}
		else if(filename.endsWith(".pcm")){
			//TODO: shold it be supported at all, how to find out samplerate, bitrate? use wav instead
			//if no detected, make user provide info
		}
		else{
			Toast.makeText(this, "File not supported, see help for list of supported files", Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
		
	}



	/** Launches an appropriate test variant, determined by <b>this.selectedMultiStageAction<b>
	 * 
	 * @param fajl - file pointing to the original lossless file that is the basis for testing
	 */
	private void launchOneOfTheTests(File fajl) {

		switch (selectedMultiStageAction) {

		case NEW_ABXY_TEST_REQUEST:
			// selectedMultiStageAction = NO_REQUEST;
			//removedLog.w(this.getClass().getName(), "pocinjem novi ABXY test");
			launcEncodingDialogs(fajl);
			break;

		case NEW_ABX_TEST_REQUEST:
			// selectedMultiStageAction = NO_REQUEST;
			//removedLog.w(this.getClass().getName(), "pocinjem novi ABX test");
			launcEncodingDialogs(fajl);
			break;

		default:
			selectedMultiStageAction = NO_REQUEST;
			//removedLog.w(this.getClass().getName(), "nothing");
			break;
		}
	}

	/** Shows dialog for determining encoding options
	 * 
	 * @param fajl - file pointing to the original lossless file that is the basis for testing
	 */
	private void launcEncodingDialogs(File fajl) {
		try{
			if (dataManager.isProcessedForCurrentSettings(fajl)) {
				AlreadyEncodedDialogFragment alreadyEncodedDialogFragment = new AlreadyEncodedDialogFragment();
				alreadyEncodedDialogFragment.show(getFragmentManager(), "re_encoding_confirmation");

			} else {
				EncodingDialogFragment encodingDialogFragment = new EncodingDialogFragment();
				encodingDialogFragment.show(getFragmentManager(), "encoding_confirmation");
			}
		}
		catch (StorageUnaccessibleException e){
			showDataFolderInaccessibleMessage();
		}
	}

	public void launchSettingsActivityFromDialog() {
		Intent intent = new Intent(this, SettingsActivity.class);
		//XXX
		//XXX
		//XXX
		//FIXME really ugly way of coding, make 5 some sensibly named constant
		//XXX
		//XXX
		//XXX
		intent.putExtra("TYPE", 5); //XXX
		startActivityForResult(intent, SETTINGS_CHANGED_IN_DIALOG);
	}

	
	public void launchABXYTesterActivity(){
//		Intent intent = new Intent(this, DummyABXYTester.class);
//		Intent intent = getPackageManager().getLaunchIntentForPackage("player.v01");
		Intent intent = new Intent(this, PlayerActivity.class);
		
		try {
			intent.putExtra("fajl1", dataManager.getSourcePCMPath(fajl));
			intent.putExtra("fajl2", dataManager.getMp3DecodedPath(fajl));
			intent.putExtra("selectorType", PlayerActivity.SelectorType.DRAGGER_SELECTOR);
			startActivity(intent);
		} catch (StorageUnaccessibleException e) {
			showDataFolderInaccessibleMessage();
		}	
	}
	
	private void launchABXTesterActivity() {
		Intent intent = new Intent(this, PlayerActivity.class);
		try {
			intent.putExtra("fajl1", dataManager.getSourcePCMPath(fajl));
			intent.putExtra("fajl2", dataManager.getMp3DecodedPath(fajl));
			intent.putExtra("selectorType", PlayerActivity.SelectorType.ABX_SELECTOR);
			startActivity(intent);
		} catch (StorageUnaccessibleException e) {
			showDataFolderInaccessibleMessage();
		}	
	}
	
	private void showDataFolderInaccessibleMessage() {
		Toast.makeText(this, "Specified data folder not accessible (insert missing " +
				"SD card or change the data directory in settings)",Toast.LENGTH_LONG).show();
	}
	
	public void chooseAndLaunchTester() {
		
		/*FIXME: this should not really be here, better place to put it would be in the service itself, after
		 * successful completion of service's tasks 
		 */
		EncoderService.resetServiceFlags(this);
		
		switch (this.selectedMultiStageAction) {
		case NEW_ABXY_TEST_REQUEST:
			this.selectedMultiStageAction = 0;
			launchABXYTesterActivity();
			break;
		case NEW_ABX_TEST_REQUEST:
			this.selectedMultiStageAction = 0;
			launchABXTesterActivity();
		default:
			this.selectedMultiStageAction = 0;
			break;
		}

	}
	
	protected void launchSelfPreparedFilesTestActivity() {
		Intent intent = new Intent(this, SelfPreparedSourcesActivity.class);
		startActivity(intent);
	}


	@Override
	public void onEncodingDialogNeutralClick(DialogFragment dialog) {
		launchSettingsActivityFromDialog();
	}

	@Override
	public void onAlreadyEncodedDialogNeutralClick(DialogFragment dialog) {
		launchSettingsActivityFromDialog();
	}
	
	@Override
	public void onEncodingDialogPositiveClick(DialogFragment dialog) {

		//FIXME: call like this: progressFragment.showProgressDialog();
//		showProgressDialog();
		((ProgressFragment) getFragmentManager().findFragmentByTag("progressFragment")).forceDialogShowing();
			
		launchPreparationServiceForNormal();
	}

	@Override
	public void onAlreadyEncodedDialogPositiveClick(DialogFragment dialog) {
		
		//FIXME: call like this: progressFragment.showProgressDialog();		
//		showProgressDialog();
		((ProgressFragment) getFragmentManager().findFragmentByTag("progressFragment")).forceDialogShowing();
		
		launchPreparationServiceForNormal();
	}
	
	private void launchPreparationServiceForNormal() {
		// start test preparation service (comprised of various encoding/decoding jobs)
		Intent intent = new Intent(this, EncoderService.class);
		intent.putExtra(EncoderService.Constants.EXTRA_TYPE, EncoderService.Constants.JOB_NORMAL);
		intent.putExtra(EncoderService.Constants.EXTRA_FILE, fajl);
		startService(intent);
	}
	
	@Override
	public void onJobCompletion() {
		//launch test variant determined by field selectedMultiStageAction
		chooseAndLaunchTester();
	}

	@Override
	public void onAlreadyEncodedDialogNegativeClick(DialogFragment dialog) {
		chooseAndLaunchTester();
	}
	
	public void stopEncodingService(){
		Intent intent = new Intent(this, EncoderService.class);
		stopService(intent);
	}
	
	
	//=========================================================================
	// Stuff used to get the hang of JNI callback mechanisms
	//=========================================================================
	
	static{
		System.loadLibrary("gnustl_shared");
		System.loadLibrary("mp3lame");
	}
	
	private void debug(){
//		nativeMethod();
		nativeMethod2(new OnNativeMethod2ProgressUpdateListener() {
			
			@Override
			public void updateProgress(float broj) {
				//removedLog.w(this.getClass().getName(),"c mi je poslao broj " + broj + " i zbog toga sam sretan");
				
			}
		});
	}
	
	private void debug2(){
		
		Intent intent = new Intent(this, HelpActivity.class);
		startActivity(intent);
		
//		String s = "";
//		
//		for( StorageInfo storageInfo : StorageUtils.getStorageList()){
//			
//			//removedLog.w(this.getClass().getName(),storageInfo.path);
//			s = s + storageInfo.path + "\n";
//		}
//		
//		Toast.makeText(this, s, Toast.LENGTH_LONG).show();
		
		
//		//varijanta 2
//		String s1 = System.getenv("SECONDARY_STORAGE");
//		//removedLog.w(this.getClass().getName(),s1);
//		
//		File f = new File(s1);
//		String s2 = f.getParent();
//		//removedLog.w(this.getClass().getName(),s2);
//		
////		Toast.makeText(this, s1 + "\n" + s2, Toast.LENGTH_LONG).show();
//		File root = new File(s2);
//		//removedLog.w(this.getClass().getName(), "Djeca od " + s2 + ":");
//		for(String inRoot : root.list()){
//			//removedLog.w(this.getClass().getName(), inRoot);
//		}
//		//removedLog.w(this.getClass().getName(),"==========");
//		
//		
//		try {
//		     // Open the file
//		     FileInputStream fs = new FileInputStream("/proc/mounts");
//
//		     DataInputStream in = new DataInputStream(fs);
//		     BufferedReader br = new BufferedReader(new InputStreamReader(in));
//
//		     String strLine;
//		     StringBuilder sb = new StringBuilder();
//
//		     //Read File Line By Line
//		     while ((strLine = br.readLine()) != null)   {
//		         // Remember each line
//		         sb.append(strLine+"\n");
//		     }
//		     
//		     //removedLog.w(this.getClass().getName(),sb.toString());
//
//		     //Close the stream
//		     in.close();
//		 } catch (Exception e) {
//		     //Catch exception if any
//		     e.printStackTrace();
//		 }
	}
	
	private void debug3(){
		
	}
	
	protected void internalFiles() {
		StringBuilder builder = new StringBuilder();
		
		for(String string : this.fileList()){
			builder.append(string);
			builder.append("\n");
		}
		
		String[] files = new File(dataManager.getDefaultDirectoryPath()).list();
		if(files!=null){
			for(String string : files){
				builder.append("  ");
				builder.append(string);
				builder.append("\n");
			}
		}
		
		
		Toast.makeText(this, builder.toString(), Toast.LENGTH_LONG).show();
		
		
	}
	
	
	private native void nativeMethod();
	private native void nativeMethod2(OnNativeMethod2ProgressUpdateListener listener);
	
	private void callback(){
		//removedLog.w(this.getClass().getName(),"ovo je pozvano iz c-a");
	}
	
	private void callback(float broj){
		//removedLog.w(this.getClass().getName(),"c mi je poslao broj " + broj);
	}
	
	private void launchLayoutTesterActivity() {
		Intent intent = new Intent(this, LayoutTestingActivity.class);
		startActivity(intent);
	}

	public interface OnNativeMethod2ProgressUpdateListener{
		void updateProgress(float broj);
	}



}
