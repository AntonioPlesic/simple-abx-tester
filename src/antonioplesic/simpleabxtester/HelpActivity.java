package antonioplesic.simpleabxtester;

import antonioplesic.simpleabxtester.R;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class HelpActivity extends Activity {

	WebView webView;
	
	int preciseScrollPosition = 0;
	
	int heightWhenLandscape = 0;
	int heightWhenPortrait = 0;
	float ratio = 1;
	
	float scrollPercentage = 0;
	boolean hasToRestoreState = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_activity_layout);
		
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("Help/About");
		
		//removedLog.i(this.getClass().getName(),"onCreate");
		
		webView = (WebView) findViewById(R.id.help_activity_web_view);
		if(savedInstanceState==null){
			
			//removedLog.i(this.getClass().getName(),"savedInstance==null -> creating");
			
			webView.loadDataWithBaseURL("file:///android_asset/", "<meta http-equiv=\"refresh\" content=\"0; url=helpIndex.html\" />", "text/html", "utf-8", null);
			webView.getSettings().setBuiltInZoomControls(true);
			webView.getSettings().setDisplayZoomControls(false);
			
		}
		
		
		final ViewTreeObserver vto = webView.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new  ViewTreeObserver.OnGlobalLayoutListener() {
			
			//XXX: method called three times, should remove it somehow after the first call
			
			@Override
			public void onGlobalLayout() {
				//removedLog.i(this.getClass().getName(),"onGlobalLayout");
				inferHeights();
				
				}
		});

		webView.setWebViewClient(new WebViewClient() {
			
			//more precise (although not completely) position restoration on orientation change
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if(hasToRestoreState){
					hasToRestoreState = false;

					final WebView finalWebView = view;

					view.postDelayed(new Runnable() {

						@Override
						public void run() {
							float webviewsize = webView.getContentHeight() - webView.getTop();

							if (webviewsize == 0) {
								//removedLog.w(this.getClass().getName(), "still zero");
								finalWebView.postDelayed(this, 10);
								return;
							}

							float positionInWV = webviewsize * scrollPercentage;
							int positionY = Math.round(webView.getTop() + positionInWV);

							//removedLog.w(this.getClass().getName(), "this happened");
							webView.scrollTo(0, positionY);
						}
					}, 10);

				}
			}
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
								
				//open web links in real browser, local ones are opened in the webView
				if(!Uri.parse(url).getHost().equals("")){
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(intent);
					return true;
				}
				
				return false;
			}

		});
		
		
		webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		

	}
		
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()){
			webView.goBack();
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
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
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		//removedLog.i(this.getClass().getName(),"onSaveInstanceState");
				
		webView.saveState(outState); 
		
		outState.putFloat("scrollPercentage", scrollPercentage());
		
		outState.putInt("heightWhenLandscape", heightWhenLandscape);
		outState.putInt("heightWhenPortrait", heightWhenPortrait);
		
		hasToRestoreState = true;
		outState.putBoolean("hasToRestoreState", hasToRestoreState);
		
		
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		//removedLog.i(this.getClass().getName(),"onRestoreInstanceState");
		
		webView.restoreState(savedInstanceState);
		
		scrollPercentage = savedInstanceState.getFloat("scrollPercentage");
		hasToRestoreState = savedInstanceState.getBoolean("hasToRestoreState");
		
		heightWhenLandscape = savedInstanceState.getInt("heightWhenLandscape");
		heightWhenPortrait = savedInstanceState.getInt("heightWhenPortrait");
		
		webView.getSettings().setBuiltInZoomControls(true);
				
		
	}
	
//	@Override
//	protected void onResume() {
//		super.onResume();
//		
//		//removedLog.i(this.getClass().getName(),"ON RESUME");
//		
//		//more precise restoration of position
//		webView.setScrollY(preciseScrollPosition);
//	}
	
	
//	@Override
//	protected void onStop() {
//		super.onStop();
//		
//		hasToRestoreState = true;
//	}

	private void inferHeights() {
		if(isLandscape()){
			//removedLog.i(this.getClass().getName(),"LANDSCAPE, height = " + webView.getHeight());
			heightWhenLandscape = webView.getHeight();
		}
		if(isPortrait()){
			//removedLog.i(this.getClass().getName(),"PORTRAIT, height = " + webView.getHeight());
			heightWhenPortrait = webView.getHeight();
		}
		if(heightWhenLandscape != 0 && heightWhenPortrait != 0){
			ratio = ((float) heightWhenPortrait)/heightWhenLandscape;
		}
	}
	
	boolean isLandscape(){
		return getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
	}
	
	boolean isPortrait(){
		return getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT;
	}
	
	float currentScreenHeight(){
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		
		if(isPortrait()){
			return (height>width)?height:width;
		}
		else{
			return (width>height)?height:width;
		}
		
		
	}
	
	float scrollPercentage(){
				
		float positionTopView = webView.getTop(); //XXX: asumes that the parent of the webview is root view, which is not necesarily the case
		//removedLog.w(this.getClass().getName(),"his top: " +positionTopView);
		
//		positionTopView = currentScreenHeight() - webView.getHeight();
//		//removedLog.w(this.getClass().getName(),"my top: " + positionTopView);
		
		
		float contentHeight = webView.getContentHeight();
		float scrollPositionY = webView.getScrollY();
		
		return (scrollPositionY - positionTopView)/contentHeight;
		
	}
	
}
