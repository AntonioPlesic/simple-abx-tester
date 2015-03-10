package antonioplesic.simpleabxtester;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * To be used as entry point into application.<br>
 * Could be used for splash screen purposes in the future, but this is not the main reason for its use.
 * Main reason is described here 
 * <a href="https://groups.google.com/forum/#!msg/android-developers/mvkWWuZ50zc/ZWxfI57vG0cJ">https://groups.google.com/forum/#!msg/android-developers/mvkWWuZ50zc/ZWxfI57vG0cJ.</a>
 * Initially, StartScreenActivity was used as the entry point to the application, but this causes awkward behavior,
 * as explained in the linked thread, when starting activity has launchMode="singletask", as is required here.
 */
public class Splash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		Intent intent = new Intent(this, StartScreenActivity.class);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intent);
//		overridePendingTransition(0, 0);
		
	}
	
	
}
