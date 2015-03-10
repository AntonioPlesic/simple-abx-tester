package antonioplesic.simpleabxtester;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;



import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import antonioplesic.simpleabxtester.R;
import antonioplesic.simpleabxtester.DataManager.StorageUnaccessibleException;
import antonioplesic.simpleabxtester.encoder.EncoderService;
import antonioplesic.simpleabxtester.encoder.ProgressFragment;
import antonioplesic.simpleabxtester.filepicker.FilePickerActivity;
import antonioplesic.simpleabxtester.player.PlayerActivity;

public class SelfPreparedSourcesActivity extends BaseActivity implements ProgressFragment.OnJobCompletionListener{
	
	private static final int PICKFILE_RESULT_CODE = 1;
	
	File fajl1;
	File fajl2;
	
	int selectedTrack;
	
	//TextView used instead of Button, see long comment little below.
//	Button track1Button;
	TextView track1TextView;
//	Button track2Button;
	TextView track2TextView;
	
	ClickGuard clickGuard = new ClickGuard();
	
	CheckBox syncCheckBox;
	
	Button okButton;
	
	RadioGroup radioGroup;
	
	DataManager dataManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.self_prepared_layout);
		
		dataManager = new DataManager(this);
		
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		//TODO: place in strings, don't hardcode
		getActionBar().setTitle("Self-prepared tracks");
		
		//attach ProgressFragment
		FragmentManager fragmentManager = getFragmentManager();
		if(fragmentManager.findFragmentByTag("progressFragment")==null){
			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
			fragmentTransaction.add(new ProgressFragment(), "progressFragment");
			fragmentTransaction.commit();
		}
		
		/*It would perhaps be more sensible to use buttons, like the commented code
		 * below and in the corresponding layout file suggests, but TextView that is 
		 * made to look like a button provides much better feel here, because of the way 
		 * it handles long text out of the box. It is much easier to
		 * make TextView that already has the desired long text wrapping act like a button, 
		 * than extend Button to provide same long text handling.
		 */
		
//		track1Button = (Button) findViewById(R.id.self_prepared_layout_track1btn);
//		track1Button.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				if(clickGuard.passed()){
//					selectedTrack = 1;
//					launchMyPicker();
//				}
//			}
//		});
//		
//		track2Button = (Button) findViewById(R.id.self_prepared_layout_track2btn);
//		track2Button.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				if(clickGuard.passed()){
//					selectedTrack = 2;
//					launchMyPicker();
//				}
//			}
//		});
		
		track1TextView = (TextView) findViewById(R.id.self_prepared_layout_track1textview);
		track1TextView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(clickGuard.passed()){
					selectedTrack = 1;
					launchMyPicker();
				}
			}
		});
		track2TextView = (TextView) findViewById(R.id.self_prepared_layout_track2textview);
		track2TextView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(clickGuard.passed()){
					selectedTrack = 2;
					launchMyPicker();
				}
			}
		});
		
		okButton = (Button) findViewById(R.id.self_prepared_layout_ok_btn);
		okButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(fajl1 != null && fajl2 != null){
					((ProgressFragment) getFragmentManager().findFragmentByTag("progressFragment")).forceDialogShowing();
					launchPreparationServiceForSelfPrepared();
				}
				else{
					showFilesNotSelectedToast();
				}
				
			}
		});
		
		radioGroup = (RadioGroup) findViewById(R.id.self_prepared_radio_group);
		
		syncCheckBox = (CheckBox) findViewById(R.id.syncCheckBox);
	}
	
	private void showFilesNotSelectedToast(){
		Toast.makeText(this, "Please select both files", Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		clickGuard.reset();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putSerializable("fajl1",fajl1);
		outState.putSerializable("fajl2", fajl2);
		outState.putInt("selectedTrack", selectedTrack);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		fajl1 = (File) savedInstanceState.getSerializable("fajl1");
		fajl2 = (File) savedInstanceState.getSerializable("fajl2");
		
		track1TextView.setText((fajl1!=null)?fajl1.getName():getString(R.string.self_prepared_select_track_1));
		track2TextView.setText((fajl2!=null)?fajl2.getName():getString(R.string.self_prepared_select_track_2));
		
		selectedTrack = savedInstanceState.getInt("selectedTrack");
	}
	
	protected void launchMyPicker() {
		Intent intent = new Intent(this, FilePickerActivity.class);
		startActivityForResult(intent, PICKFILE_RESULT_CODE);	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		//=====================================================================
		// On file chooser result
		//=====================================================================

		File fajl = null;

		if ((resultCode == Activity.RESULT_OK && requestCode == PICKFILE_RESULT_CODE)) {

			//removedLog.w("vratio sam se iz file pickera", "vratio sam se iz fajl pickera");
			//removedLog.w("file",data.getData().toString());
			//removedLog.w("file", data.getData().getPath());

			fajl = new File(data.getData().getPath());
			//removedLog.w("apsolutni", fajl.getAbsolutePath());

			boolean isLocalFile = fajl.exists();

			//removedLog.w(this.getClass().getName(),"Je li rezultat odabira vec fajl?: " + isLocalFile );
			//removedLog.w("filename", fajl.getName().toString());
			;

			if(isLocalFile){
				;
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
			
			if(testFileSupported(fajl)){
				if(selectedTrack==1){
					fajl1 = fajl;
					track1TextView.setText(fajl.getName());
				}else if(selectedTrack==2){
					fajl2 = fajl;
					track2TextView.setText(fajl.getName());
				}
				else{
					//should never happen
				}
			}
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
			return checkMp3(fajl);
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
			Toast.makeText(this, "File not supported, see help for the list of supported files", Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}

	private void showDataFolderInaccessibleMessage() {
		// TODO Auto-generated method stub
		
	}
	
	private void launchPreparationServiceForSelfPrepared() {
		// start test preparation service (comprised of various encoding/decoding jobs)
		Intent intent = new Intent(this, EncoderService.class);
		intent.putExtra(EncoderService.Constants.EXTRA_TYPE, EncoderService.Constants.JOB_SELF_PREPARED);
		intent.putExtra(EncoderService.Constants.EXTRA_FILE1,fajl1);
		intent.putExtra(EncoderService.Constants.EXTRA_FILE2,fajl2);
		intent.putExtra(EncoderService.Constants.EXTRA_SYNCHRONIZE, syncCheckBox.isChecked());
		
		startService(intent);
	}

	@Override
	public void onJobCompletion() {
		
		//removedLog.i(this.getClass().getName(),"onJobCompletion");
		EncoderService.resetServiceFlags(this); /*XXX: to prevent repeated calling of onJobCompletion from service
		* instead of this, ProgressFragment should somehow consume the job completion once it gets to this activity.
		* Currently this is achieved by reseting flags in the service directly, but that is awful.
		*/
		
		launchABXYTesterActivity();
	}
	
	public void launchABXYTesterActivity(){
//		Intent intent = new Intent(this, DummyABXYTester.class);
//		Intent intent = getPackageManager().getLaunchIntentForPackage("player.v01");
		Intent intent = new Intent(this, PlayerActivity.class);
		
		try {
			intent.putExtra("fajl1", dataManager.getSelfPreparedPath1(fajl1));
			
			//FIXME: both point to the first file during debugging
//			intent.putExtra("fajl2", dataManager.getSelfPreparedPath1(fajl1));
			intent.putExtra("fajl2", dataManager.getSelfPreparedPath2(fajl2));
			
			int selected = radioGroup.getCheckedRadioButtonId();
			if(selected == R.id.self_prepared_radioABXY){
				intent.putExtra("selectorType", PlayerActivity.SelectorType.DRAGGER_SELECTOR);
			}
			else if(selected == R.id.self_prepared_radioABX){
				intent.putExtra("selectorType", PlayerActivity.SelectorType.ABX_SELECTOR);
			}
			
			startActivity(intent);
		} catch (StorageUnaccessibleException e) {
			showDataFolderInaccessibleMessage();
		}	
	}

}
