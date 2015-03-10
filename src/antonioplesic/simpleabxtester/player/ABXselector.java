package antonioplesic.simpleabxtester.player;

import antonioplesic.simpleabxtester.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


//=============================================================================
/* Note: 
 * 
 * This whole class is an afterthought, modeled after the DraggerFragment
 * class. Idea is that one can can use either ABXY or ABX variant of the test,
 * depending on preference. In my interpretation of ABXY variant, user is supposed
 * to match tracks that sound the same by dragging them close together. 
 *
 * In this class, procedure is simpler, but not as "interesting", or "fun". Only
 * three track selectors are shown, each represented by an ordinary button. Two of
 * the buttons point to the same track (either lossy or lossless) and the goal is 
 * that the "testee" recognizes which button points to the other track. If she can do
 * so, then obviously the difference can be heard.
 * 
 * Logic of testing is basically the same as in ABXY variant, but the extra Y track is
 * removed, cuz it's not really needed, even though it could ease the ability to find
 * which track is which, cuz user might prefer for example looking for "worse sounding"
 * track first, or looking for "better sounding" track first. Here, user doesen't know
 * if there are two "worse" tracks and one "better", or two "better" tracks and one "worse"
 * present. Though, it could be set up that two out of three tracks are always the "better"
 * ones, or the opposite, but I don't think it is really important. 
 * 
 * This variant is inspired by "Phillips Golden Ears Challange" site, while the other
 * one is inspired by Foobar's ABX(Y) plugin.
 * 
 * Why: cuz in foobar you have to remeber which is A, which is X, then select "claims"
 * Here, you just match them positionaly, using spacial memory, which i think is better
 * than remembering which one is which while choosing answer.
 * Also, there is no audible break introduced while switching tracks as in Foobar,
 * transitions are seamless. Break in Foobar's tester i personally find very irritating,
 * and it messes with my concentration.
 *  
 */
//=============================================================================

public class ABXselector extends TrackSelectorFragment implements OnClickListener{

	ToggleButton buttonA;
	ToggleButton buttonB;
	ToggleButton buttonC;
	Button buttonDummy;
	Button buttonOK;
	
	TextView descriptionTextView; 
	
	CheckBox chooseDifferentChkBox;
	boolean chooseDifferent = false;
	
	SelectedButton selectedButton;
	
	enum SelectedButton{
		A,B,C,dummy,none;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.abx_three_button_frag_layout, container,
				false);
		
		buttonA = (ToggleButton) v.findViewById(R.id.toggleButtonA);
		buttonA.setOnClickListener(this);
		
		buttonB = (ToggleButton) v.findViewById(R.id.toggleButtonB);
		buttonB.setOnClickListener(this);
		
		buttonC = (ToggleButton) v.findViewById(R.id.toggleButtonC);
		buttonC.setOnClickListener(this);
		
		buttonOK = (Button) v.findViewById(R.id.buttonOK_ABX);
		buttonOK.setOnClickListener(this);
		
		chooseDifferentChkBox = (CheckBox) v.findViewById(R.id.checkBoxChooseDifferent);
		//load saved preference
		chooseDifferentChkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setDescriptionText();
			}
		});
		
		descriptionTextView = (TextView) v.findViewById(R.id.dragger_textView_Y);
		setDescriptionText();
		
		return v;
	}
	
	protected void setDescriptionText() {
		if(chooseDifferentChkBox.isChecked()){
			descriptionTextView.setText(R.string.ABXdescriptioOpposite);
		}
		else{
			descriptionTextView.setText(R.string.ABXdescriptioNormal);
		}
		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if(savedInstanceState!=null){
			selectedButton = (SelectedButton) savedInstanceState.getSerializable("selectedButton");
		}
		else{
			initialize();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		outState.putSerializable("selectedButton", selectedButton);
		
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onClick(View v) {
		
		IButtonPress activity = (IButtonPress) getActivity();
		
		if(v==buttonA){
			selectedButton = SelectedButton.A;
			activity.pressedA();
			
		}
		else if(v==buttonB){
			selectedButton = SelectedButton.B;
			activity.pressedB();
		
		}
		else if(v==buttonC){
			selectedButton = SelectedButton.C;
			activity.pressedX();
		}
		else if(v==buttonOK){
			
			//TYPE_SCPECIAL - mathematics is wrong, do not use
			
//			//if A is different
//			if(selectedButton==SelectedButton.A){
//				activity.pritisnutOK(false, true, true, false);
//			}
//			//if B is different
//			else if(selectedButton==SelectedButton.B){
//				activity.pritisnutOK(true, false, false, true);
//			}
//			//if C(X) is different
//			else if(selectedButton==SelectedButton.C){
//				activity.pritisnutOK(false, false, false, false);
//			}
			
			//TYPE_ONE_REFERENCE_TWO_CHOICES
			if(selectedButton == SelectedButton.A){
				Toast.makeText(getActivity(), "Please select either track A or B, as explained above", Toast.LENGTH_SHORT).show();
			}
			else if(selectedButton == SelectedButton.B){
//				activity.pritisnutOK(AisX, AisY, BisX, BisY);
				if(!chooseDifferentChkBox.isChecked())
					activity.pressedOK(false,false,false,false); //implies A is B, X is Y
				else
					activity.pressedOK(true, false, true, false);
			}
			else if(selectedButton == SelectedButton.C){
				if(!chooseDifferentChkBox.isChecked())
					activity.pressedOK(true, false, false, true);
				else
					activity.pressedOK(false, false, false, false);
			}
				
		}
		
		if(v==buttonA || v==buttonB || v==buttonC){
			setButtonBackgrounds();
		}
		
	}

	private void setButtonBackgrounds() {
				
		//use default button appearence for now, and its setPressed method
//		buttonA.setBackgroundResource(R.drawable.roundedbuttonstatic);
//		buttonB.setBackgroundResource(R.drawable.roundedbuttonstatic);
//		buttonC.setBackgroundResource(R.drawable.roundedbuttonstatic);
		
		buttonA.setChecked(false);
		buttonB.setChecked(false);
		buttonC.setChecked(false);
		
		if(selectedButton == SelectedButton.A){
			buttonA.setChecked(true);
		}
		else if(selectedButton == SelectedButton.B){
			buttonB.setChecked(true);
		}
		else if(selectedButton == SelectedButton.C){
			buttonC.setChecked(true);
		}	
	}
	
	@Override
	public void initialize(){
		onClick(buttonA);
	}
	
	@Override
	public void notifiyPlaybackStopped() {
		
	}
	
	@Override
	public void notifiyPlaybacksStarted() {
		
	}

}
