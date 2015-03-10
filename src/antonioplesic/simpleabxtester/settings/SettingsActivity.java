package antonioplesic.simpleabxtester.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;


public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		if(savedInstanceState == null){
			//Display the fragment as the main content
			getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
		}
	}
	
	@Override
	public void onBackPressed() {		
		Intent intent = new Intent();
		setResult(RESULT_OK, intent);
		finish();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			Intent intent = new Intent();
			setResult(RESULT_OK, intent);
			finish();
			return true;

		default:
			return false;
		}
	}
	
}
