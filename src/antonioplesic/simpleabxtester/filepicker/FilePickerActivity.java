package antonioplesic.simpleabxtester.filepicker;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

public class FilePickerActivity extends Activity {
		
	String result = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		/*TODO: find a suitable name for activity, probably "Pick a file" or "Pick a directory",
		 * depending on what is being picked ofc */
		//getActionBar().setTitle("");
		
		if(savedInstanceState==null){
			//Display the fragment as the main content
			getFragmentManager().beginTransaction().replace(android.R.id.content, new FilePickerFragment()).commit();
		}
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		default:
			return false;
		}
	}
	
	
	
//	public int getSortMode

	
}
