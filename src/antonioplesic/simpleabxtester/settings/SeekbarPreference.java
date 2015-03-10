package antonioplesic.simpleabxtester.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import antonioplesic.simpleabxtester.R;

public class SeekbarPreference extends Preference{
	
	final static int min = 5000;
	int value;
	
	TextView textView;
	SeekBar seekBar; 
	
	OnPreferenceChangeListener listener = getOnPreferenceChangeListener();
	
	public SeekbarPreference(Context context) {
		super(context);
	}

	public SeekbarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SeekbarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		
//		setPersistent(true); //set in xml
		value = getPersistedInt(min);
		
		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View v = inflater.inflate(R.layout.slider_layout, parent, false);
		
		textView = (TextView) v.findViewById(R.id.sliderPosition);
		textView.setText("" + value + " samples, " + toMilis(value, 44.1) + " ms @44.1kHz");
		
		seekBar = (SeekBar) v.findViewById(R.id.seekBarSlider);
		seekBar.setProgress(value-min);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				persistInt(value);
//				notifyListener();
				callChangeListener(value);
//				getEditor().commit();
			};
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			};
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				value=min+progress;
				textView.setText("" + value + "samples, " + toMilis(value, 44.1) + " ms @44.1kHz");
			}
		});
		
		return v;
	}
	
//	private void notifyListener(){
//		if(listener!=null){
//			//removedLog.w(this.getClass().getName(),"listener not null");
//			listener.onPreferenceChange(this, this.value);
//		}
//	}
	
	
	public int getValue(){
		return getPersistedInt(min);
	}
	
	static int toMilis(int samples, double samplerateInKHz){
		return (int) ((double) samples/samplerateInKHz);
	}
	
}
