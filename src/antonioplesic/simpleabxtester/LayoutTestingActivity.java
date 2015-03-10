package antonioplesic.simpleabxtester;


import antonioplesic.simpleabxtester.R;
import antonioplesic.simpleabxtester.player.DraggerFragment;
import antonioplesic.simpleabxtester.player.IButtonPress;
import antonioplesic.simpleabxtester.player.TrackSelectorFragment;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public class LayoutTestingActivity extends Activity implements IButtonPress {

	TrackSelectorFragment selectorFragment;
	
	TextView resultsTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_player_main);
		
		resultsTextView = (TextView) findViewById(R.id.dragger_textView_Y);
		
		if (savedInstanceState != null) {
//			getFragmentManager().getFragment(savedInstanceState,"selectorFragment");
//			//removedLog.i(this.getClass().getName(),"Fragment kakti loadan u onCreate");
			
		}
		else{
			selectorFragment = new DraggerFragment();
			
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.add(R.id.frameLayout, selectorFragment);
			transaction.commit();
			//removedLog.i(this.getClass().getName(),"fragment stvoren prvi put");
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		getFragmentManager().putFragment(outState, "selectorFragment", selectorFragment);
		//removedLog.i(this.getClass().getName(),"Fragment sejvan");
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		selectorFragment = (TrackSelectorFragment) getFragmentManager().getFragment(savedInstanceState, "selectorFragment");
		//removedLog.i(this.getClass().getName(),"Fragment loadan u onRestoreInstanceState");
	}

	@Override
	public boolean pressedA() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pressedB() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pressedX() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pressedY() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pressedOK(boolean AisX, boolean AisY, boolean BisX,
			boolean BisY) {
		
		resultsTextView.setText("Correct " + 345 + "/" + 500 + " trials\nProbability of guessing: " + "100.00" + "%");
		resultsTextView.setText("Correct " + 345 + "/" + 500 + " trials\nProbability of guessing: " + "100.00" + "%");
		
		return false;
	}
	
}
