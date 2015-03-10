package antonioplesic.simpleabxtester.settings;

import antonioplesic.simpleabxtester.R;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class PreferenceWithLongSummary extends Preference {

	public PreferenceWithLongSummary(Context context){
		super(context);
	}
	
	public PreferenceWithLongSummary(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	
	public PreferenceWithLongSummary(Context context, AttributeSet attrs, int defStyle){
		super(context,attrs,defStyle);
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
		summaryView.setMaxLines(50);
	}
	
}
