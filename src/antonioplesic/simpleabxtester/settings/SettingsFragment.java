package antonioplesic.simpleabxtester.settings;

import java.io.File;

import javax.crypto.spec.OAEPParameterSpec;

import antonioplesic.simpleabxtester.DataManager;
import antonioplesic.simpleabxtester.R;
import antonioplesic.simpleabxtester.encoder.mp3EncoderDecoder;
import antonioplesic.simpleabxtester.filepicker.FilePickerActivity;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment {
	
//	PreferenceScreen qualityOuter = null;
	
	SettingsHelper helper; //helper for easier access to various settings
	
	private final static int DIRECTORY_REQUEST = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		helper = new SettingsHelper(getActivity());
		
		/*Creates preference screen relevant to context from which it is launched
		 * For example, if launched from encoder dialog, show only encoder options
		 * and add an OK button. Othervise, when preferences are launched from options
		 * menu, show all application settings. 								*/
		Bundle extras = getActivity().getIntent().getExtras();
		if( extras==null){
			addEnkoderPreferences();
			addSyncPreferences();
			addDataPreferences();
		}
		//TODO: do not hardcode 5, use const. int. or similar
		else if(  extras.getInt("TYPE") == 5    ){
			addEnkoderPreferences();
			addOkButton();
		}
		
	}
	
	
	/**Adds preference that acts like a OK button.
	 * Action performed is the same as pressing back button.  */
	private void addOkButton() {
		addPreferencesFromResource(R.xml.close_preference);

		Preference okBtn = findPreference("closePreference");
		okBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				getActivity().onBackPressed();
				return true;
			}
		});
	}

	void addEnkoderPreferences(){
		addPreferencesFromResource(R.xml.enkoder_settings);
		
		ListPreference myPref = (ListPreference) findPreference("encoderBitrate");
		myPref.setSummary( mp3EncoderDecoder.getBitrateDescription( Integer.parseInt( myPref.getValue())));
		myPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				((ListPreference) preference).setSummary(mp3EncoderDecoder.getBitrateDescription( Integer.parseInt( (String) newValue)));
//				((ListPreference) preference).setSummary((String) newValue);
				return true;
			}
		});

		
		final PreferenceScreen qualityOuter = (PreferenceScreen) findPreference("qualitySubscreen");
		
		ListPreference qualityPref = (ListPreference) findPreference("encoderQuality");
		
		qualityPref.setSummary(qualityPref.getValue());
		qualityOuter.setSummary(qualityPref.getValue());
		
		qualityPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				((ListPreference) preference).setSummary((String) newValue);
				qualityOuter.setSummary((String) newValue);
				((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged(); /* very intuitive -.-
				*thanks to: http://stackoverflow.com/a/5040842 and http://stackoverflow.com/a/15281630
				*/
				return true;
			}
		});
		
//		//ideja je bila resetirati na default ako se iskljuci advanced mode, ali mislim da je bolje
		//da se sejvaju trenutne vrijednosti, a implicitno koriste defaultne
//		CheckBoxPreference advancePrefChkBox = (CheckBoxPreference) findPreference("advancedEncoder");
//		advancePrefChkBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//			
//			@Override
//			public boolean onPreferenceChange(Preference preference, Object newValue) {
//				
//				if(  Boolean.parseBoolean((String) newValue) == false ){
//					//reset advanced properties do default			
//				}
//				return true;
//			}
//		});
	}
	
	void addDataPreferences(){
		
		addPreferencesFromResource(R.xml.data_settings);
		
		CheckBoxPreference myPref = (CheckBoxPreference) findPreference("usesCustomDirectory");
		myPref.setSummaryOff("Currently uses internal storage (unacsessible to file explorers on unrooted devices)");
				
		Preference selectDirectoryPref = (Preference) findPreference("customDirectorySelector");
		selectDirectoryPref.setSummary(helper.getActualCustomDirectory());
		selectDirectoryPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						launchMyPicker();
						return true;
					}
				});
	}
	
	void addSyncPreferences(){
		
		addPreferencesFromResource(R.xml.sync_settings);
		
		final PreferenceScreen outer = (PreferenceScreen) findPreference("syncSubscreen");
		SeekbarPreference seekbarPref = (SeekbarPreference) findPreference("syncWindowLength");
		
		seekbarPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				outer.setSummary("" + (int) newValue + " samples, " + SeekbarPreference.toMilis((int) newValue, 44.1) + " ms @44.1kHz");
				((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
				
				return true;
			}
		});
		
		
		outer.setSummary("" + seekbarPref.getValue() + " samples, " + SeekbarPreference.toMilis(seekbarPref.getValue(), 44.1) + " ms @44.1kHz");
		
		
		
	}
	
	protected void launchMyPicker() {
		Intent intent = new Intent(getActivity(), FilePickerActivity.class);
		startActivityForResult(intent, DIRECTORY_REQUEST);	
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if ((resultCode == Activity.RESULT_OK && requestCode == DIRECTORY_REQUEST)){
			
			String receivedDirectoryPath = data.getData().getPath();
			//removedLog.d(this.getClass().getName(),"Received directory: " + receivedDirectoryPath);
			
			//check if it is really a directory before setting it
			if( new File(receivedDirectoryPath).isDirectory()       ){
				helper.setCustomDirectory(receivedDirectoryPath);
			}
			else{
				Toast.makeText(getActivity(), "Please select a valid directory!", Toast.LENGTH_LONG).show();
			}
			
			Preference selectDirectoryPref = (Preference) findPreference("customDirectorySelector");
			selectDirectoryPref.setSummary(helper.getActualCustomDirectory());
			
		}
		else{
			//removedLog.d(this.getClass().getName(),"Directory selection canceled");
			SharedPreferences foo = PreferenceManager.getDefaultSharedPreferences(getActivity());
			//removedLog.w(this.getClass().getName(), foo.getString("encoderQuality","bullshit"));
		}
	}

}
