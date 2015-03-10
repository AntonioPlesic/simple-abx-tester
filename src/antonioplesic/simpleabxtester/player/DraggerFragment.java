package antonioplesic.simpleabxtester.player;

import java.util.Arrays;
import java.util.HashMap;

import antonioplesic.simpleabxtester.R;


import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DraggerFragment extends TrackSelectorFragment implements OnDragListener,
		OnClickListener {

	Button buttonA; //xml instantiead to class MyButton that extends Button
	Button buttonB; //xml instantiead to class MyButton that extends Button
	Button buttonX;
	Button buttonY;
	
	TextView textViewX;
	TextView textViewY;
	
	enum SelectedButton{
		A,B,X,Y;
	}
	
	SelectedButton selectedButton;
	
	private volatile boolean isPlaying = false;
		
	Button buttonOK;
	LinearLayout linearLayout;
	LinearLayout layoutX;
	LinearLayout layoutY;
	LinearLayout layoutAB;
	FrameLayout dynamicHolderY;
	FrameLayout dynamicHolderX;
	FrameLayout startHolderA;
	FrameLayout startHolderB;
	
//	private IButtonPress suceljeKaActivitiju; //not used

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.dragger_frag_layout, container,
				false);

		buttonA = (Button) v.findViewById(R.id.buttonA);
//		buttonA.setOnLongClickListener(onLongClick);
		buttonA.setOnClickListener(this);
		buttonA.setSoundEffectsEnabled(false);

		buttonB = (Button) v.findViewById(R.id.buttonB);
//		buttonB.setOnLongClickListener(onLongClick);
		buttonB.setOnClickListener(this);
		buttonB.setSoundEffectsEnabled(false);
		

		buttonX = (Button) v.findViewById(R.id.buttonX);
		buttonX.setOnClickListener(this);
		buttonX.setSoundEffectsEnabled(false);
		buttonY = (Button) v.findViewById(R.id.buttonY);
		buttonY.setOnClickListener(this);
		buttonY.setSoundEffectsEnabled(false);
		
		buttonOK = (Button) v.findViewById(R.id.btnOK);
		buttonOK.setOnClickListener(this);

		linearLayout = (LinearLayout) v.findViewById(R.id.glavniLayout);
		linearLayout.setOnDragListener(this);

		layoutX = (LinearLayout) v.findViewById(R.id.dropableX);
		layoutX.setOnDragListener(this);

		layoutY = (LinearLayout) v.findViewById(R.id.dropableY);
		layoutY.setOnDragListener(this);
		
		dynamicHolderY = (FrameLayout) v.findViewById(R.id.dynamicHolderY);
		dynamicHolderX = (FrameLayout) v.findViewById(R.id.dynamicHolderX);
		
		startHolderA = (FrameLayout) v.findViewById(R.id.startHolderA);
		startHolderB = (FrameLayout) v.findViewById(R.id.startHolderB);

		// TODO: razdvojiti ovo na dva layouta
		layoutAB = (LinearLayout) v.findViewById(R.id.linearLayoutPocetni);
		layoutAB.setOnDragListener(this);
		
		textViewX = (TextView) v.findViewById(R.id.dragger_textView_X);
		textViewY = (TextView) v.findViewById(R.id.dragger_textView_Y);

		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);

		// if(! (activity instanceof JobCallbacks)){
		// throw new
		// IllegalStateException("Activity must implement the TaskCallbacks interface.");
		// }
		//
		// //Referenca na roditeljski activity preko koje javljam rezultate
		// myCallbacks = (JobCallbacks) activity;
		// // attached.set(true); // moj dodatak
		// //removedLog.w("spojen activity ",myCallbacks.toString());

	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		//removedLog.w("draggerFragment", "On activity created");
		// uvijek mora biti
		super.onActivityCreated(savedInstanceState);

		//removedLog.w("draggerFragment","savedInstanceState == " + Boolean.toString(savedInstanceState == null));
		
		if (savedInstanceState != null) {
			repopulateLayoutFromSavedInstanceState(layoutX, "X", savedInstanceState);
			repopulateLayoutFromSavedInstanceState(dynamicHolderX, "dX", savedInstanceState);
			repopulateLayoutFromSavedInstanceState(layoutY, "Y", savedInstanceState);
			repopulateLayoutFromSavedInstanceState(dynamicHolderY, "dY", savedInstanceState);
//			repopulateLayoutFromSavedInstanceState(layoutAB, "AB", savedInstanceState);
			repopulateLayoutFromSavedInstanceState(startHolderA, "startA", savedInstanceState);
			repopulateLayoutFromSavedInstanceState(startHolderB, "startB", savedInstanceState);

			// alternativno, mogao bih traziti activiti da mi kaze trenutno
			// stanje playera i sl.
			// jer moze se promijeniti dok je neaktivan fragment, pa ce loadat
			// gumb u play stanju
			// iako je playback stao
			buttonA.setText(savedInstanceState.getString("buttonA"));
			buttonB.setText(savedInstanceState.getString("buttonB"));
			buttonX.setText(savedInstanceState.getString("buttonX"));
			buttonY.setText(savedInstanceState.getString("buttonY"));
			
			textViewX.setVisibility(savedInstanceState.getInt("textViewXVisibility"));
			textViewY.setVisibility(savedInstanceState.getInt("textViewYVisibility"));
			
			selectedButton =  (SelectedButton) savedInstanceState.getSerializable("selectedButton");
			
			isPlaying = savedInstanceState.getBoolean("isPlaying");
			
			setButtonBackgrounds();
			

		}
		else{
			initialize();
		}
		


	}

	private void repopulateLayoutFromSavedInstanceState(ViewGroup layout,
			String mark, Bundle savedInstanceState) 
	{
		for (int id : savedInstanceState.getIntArray(mark)) {
			//removedLog.w("load id", "id:" + Integer.toString(id));

			// razlika u odnosu na activity pristup
			View v = getView().findViewById(id);

			((ViewGroup) v.getParent()).removeView(v);
			//removedLog.w("Loadani view", v.toString());
			layout.addView(v);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

		saveLayoutChildren(layoutX, "X", outState);
		saveLayoutChildren(dynamicHolderX, "dX", outState);
		saveLayoutChildren(layoutY, "Y", outState);
		saveLayoutChildren(dynamicHolderY, "dY", outState);
//		saveLayoutChildren(layoutAB, "AB", outState);
		saveLayoutChildren(startHolderA, "startA", outState);
		saveLayoutChildren(startHolderB, "startB", outState);

		outState.putString("buttonA", buttonA.getText().toString());
		outState.putString("buttonB", buttonB.getText().toString());
		outState.putString("buttonX", buttonX.getText().toString());
		outState.putString("buttonY", buttonY.getText().toString());
		
		outState.putInt("textViewXVisibility", textViewX.getVisibility());
		outState.putInt("textViewYVisibility", textViewY.getVisibility());
		
		outState.putSerializable("selectedButton", selectedButton);
		
		outState.putBoolean("isPlaying", isPlaying);

		// UVIJEK MORA BITI kad se overloada onSaveInstanceState
		super.onSaveInstanceState(outState);
	}

	private void saveLayoutChildren(ViewGroup layout, String mark,
			Bundle outState) {
		int[] childredOfLayout = new int[layout.getChildCount()];
		for (int i = 0; i < layout.getChildCount(); i++) {
			int id = layout.getChildAt(i).getId();
			//removedLog.w("saved id", "id:" + Integer.toString(id));
			childredOfLayout[i] = layout.getChildAt(i).getId();
		}
		outState.putIntArray(mark, childredOfLayout);
	}

	@Override
	public boolean onDrag(View v, DragEvent event) {
		// TODO: radi ok, ali neke stvari koje nebi trebale ponove se vise puta,
		// prosljedjivanje eventa samomm gumbu nije napravljeno (naoko ni ne
		// treba),
		// ali radi kompletnosti bilo bi dobro ovo napisati ispocetka kak se
		// spada
		
		final int action = event.getAction();
		
		switch (action) {

		case DragEvent.ACTION_DRAG_STARTED:
			//removedLog.i(this.getClass().getName(),"DRAG STARTED IN HOLDER: " + v.toString());
			//removedLog.i(this.getClass().getName(),"event: " + event.getLocalState() );
			Button buttonDragged = ((Button) event.getLocalState());
			buttonDragged.setVisibility(View.INVISIBLE);
			
			if(buttonDragged.getParent() == textViewX.getParent() ){
				textViewX.setVisibility(View.VISIBLE);
			}
			else if( buttonDragged.getParent() == textViewY.getParent()){
				textViewY.setVisibility(View.VISIBLE);
			}
			
			return true;

		case DragEvent.ACTION_DRAG_ENTERED:
			return true;

		case DragEvent.ACTION_DRAG_LOCATION:
			return true;

		case DragEvent.ACTION_DRAG_EXITED:			
			return true;

		case DragEvent.ACTION_DROP:

			Button buttonJustDropped = (Button) event.getLocalState();
			ViewGroup parentThatReceivedDrop = ((ViewGroup) v);
			ViewGroup otherParent = (ViewGroup) buttonJustDropped.getParent();

			//odbacen u Y
			if( v == layoutY ){
				
				//ako je netko vec unutra (osim textviewa)
				if(dynamicHolderY.getChildCount() > 1){
					
					//dohvacam index 1 jer je index 0 textview
					Button alreadyPresentButton = (Button) dynamicHolderY.getChildAt(1);
					
					//ako je taj unutra razlicit od novopridoslog
					if(alreadyPresentButton != buttonJustDropped){
						
						dynamicHolderY.removeView(alreadyPresentButton);
						otherParent.removeView(buttonJustDropped);
						
						//zamijeni ih
						dynamicHolderY.addView(buttonJustDropped);
						otherParent.addView(alreadyPresentButton);
						
						buttonJustDropped.setVisibility(View.VISIBLE);
						//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");
						
						return true;
					}
					//inace je isti, tj. vraca se otkud je krenuo
					else{
						buttonJustDropped.setVisibility(View.VISIBLE);
						//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");
						return true;
					}
				}
				//inace unutra nema jos nikoga
				else{
					otherParent.removeView(buttonJustDropped);
					dynamicHolderY.addView(buttonJustDropped);
					buttonJustDropped.setVisibility(View.VISIBLE);
					//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");
					return true;
				}
			}
			
			//odbacen u X
			if( v == layoutX ){
								
				//ako je netko vec unutra (osim textviewa)
				if(dynamicHolderX.getChildCount() > 1){
					
					//dohvacam index 1 jer je index 0 textview
					Button alreadyPresentButton = (Button) dynamicHolderX.getChildAt(1);
					
					//ako je taj unutra razlicit od novopridoslog
					if(alreadyPresentButton != buttonJustDropped){
						
						dynamicHolderX.removeView(alreadyPresentButton);
						otherParent.removeView(buttonJustDropped);
						
						//zamijeni ih
						dynamicHolderX.addView(buttonJustDropped);
						otherParent.addView(alreadyPresentButton);
						
						buttonJustDropped.setVisibility(View.VISIBLE);
						//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");
						
						return true;
					}
					//inace je isti, tj. vraca se otkud je krenuo
					else{
						buttonJustDropped.setVisibility(View.VISIBLE);
						//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");
						return true;
					}
				}
				//inace unutra nema jos nikoga
				else{
					otherParent.removeView(buttonJustDropped);
					dynamicHolderX.addView(buttonJustDropped);
					buttonJustDropped.setVisibility(View.VISIBLE);
					//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");
					return true;
				}
			}
			
			
			//odbacen u AB
			if(v == layoutAB){
				
				otherParent.removeView(buttonJustDropped);
				
				if(startHolderA.getChildCount() == 0){
					startHolderA.addView(buttonJustDropped);
					buttonJustDropped.setVisibility(View.VISIBLE);
					//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");
				}
				else if(startHolderB.getChildCount()==0){
					startHolderB.addView(buttonJustDropped);
					buttonJustDropped.setVisibility(View.VISIBLE);
					//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");
				}
				return true;
				
			}

			// odbacen bilo gdje drugdje
			// vrati odakle je dosao

			// odbacen drugdje
			//removedLog.w("odbacen unutar fragmenta, ali izvan meta" , "odbacen unutar fragmenta, ali izvan meta");
			buttonJustDropped.setVisibility(View.VISIBLE);
			//removedLog.i(this.getClass().getName(),"buttonJustDropped.setVisibility(View.VISIBLE);");

			return false;

		case DragEvent.ACTION_DRAG_ENDED:
			//removedLog.w("ended fired", "ended fired");
			// ovo ne valja raditi ovjde, zato jer svi registrirani viewovi
			// pokrecu ovo (dogodi se 3 puta)
			// sto potvrduje log
			// mislim, ovo radi, ali nije lijepo

			((Button) event.getLocalState()).setVisibility(View.VISIBLE);
			//removedLog.i(this.getClass().getName(),"((Button) event.getLocalState()).setVisibility(View.VISIBLE);");
			
			setGlobalTextViewVisibility();
			
			// vjerojatno rjesenje:
			// http://developer.android.com/reference/android/view/View.OnDragListener.html
			/*
			 * If the listener wants to fall back to the hosting view's
			 * onDrag(event) behavior, it should return 'false' from this
			 * callback.
			 * 
			 * Dakle treba vratiti false, i napraviti da view koji se draga ima
			 * settiranog onDragListenera, kojemu se prosljeduje drop.
			 */
			return true;

		default:
			break;
		}

		//removedLog.w("nesmije se dogoditi", "nesmije se dogoditi");
		return false;
	}

	private void setGlobalTextViewVisibility() {
		if(dynamicHolderX.getChildCount()>=2){
			textViewX.setVisibility(View.INVISIBLE);
		}
		else{
			textViewX.setVisibility(View.VISIBLE);
		}
		if(dynamicHolderY.getChildCount()>=2){
			textViewY.setVisibility(View.INVISIBLE);
		}
		else{
			textViewY.setVisibility(View.VISIBLE);
		}
	}

/*	during most of the development, button drag was started with longClick, however
 *  that is not dynamic enough for final product, as it takes too long to enter drag
 *  mode.
 */
//	private OnLongClickListener onLongClick = new OnLongClickListener() {
//
//		public boolean onLongClick(View view) {
//			//removedLog.w("long click", "long click");
//			DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
//
//			view.startDrag(null, shadowBuilder, view, 0);
//			view.setVisibility(view.INVISIBLE);
//			return true;
//		}
//	};
		
	@Override
	public void notifiyPlaybackStopped() {
		//removedLog.w("HHHHHHHHHHHHHHH", "primio kraj");
		
		isPlaying = false;
		
		// svim gumbima koji trenutno imaju oznaku sviranja daj oznaku
		// "ne svira"
		for (Button button : Arrays.asList(buttonA, buttonB, buttonX, buttonY)) {
			button.setText(button.getText().toString().replace("*", "P"));
		}
		
		setButtonBackgrounds();

	}

	@Override
	public void notifiyPlaybacksStarted() {
		// gumbima koji sviraju daj oznaku "svira"
		//removedLog.w("HHHHHHHHHHHHHHH", "primio pocetak");
		
		isPlaying = true;
		
		for (Button button : Arrays.asList(buttonA, buttonB, buttonX, buttonY)) {
			button.setText(button.getText().toString().replace("P", "*"));
		}
		
		setButtonBackgrounds();

	}

	@Override
	public void onClick(View v) {

		// koja je razlika izmedju ovakvog pozivanja i onog kakvog sam
		// napravio u onom prvom projektu (kada registriram activity u onAttach)
		IButtonPress activity = (IButtonPress) getActivity();

		/* TODO Seems overcomplicated, no real advantage in accessing it through map, don't remember
		 * why I did this in the first place */
		HashMap<Button, String> btnDefaultText = new HashMap<Button, String>();
		btnDefaultText.put(buttonA, getResources().getString(R.string.buttonAtext));
		btnDefaultText.put(buttonB, getResources().getString(R.string.buttonBtext));
		btnDefaultText.put(buttonX, getResources().getString(R.string.buttonXtext));
		btnDefaultText.put(buttonY, getResources().getString(R.string.buttonYtext));
		
		
		/* Notify activity about a button press, and dependeing on return
		 * value -> whether audio is playing, modify button text if needed,
		 * and mark currently selected button */
		if (v == buttonA) {			
//			buttonA.setText(btnDefaultText.get(buttonA) + ((activity.pressedA()) ? " *" : "P"));
			activity.pressedA();
			selectedButton = SelectedButton.A;
			
		} else if (v == buttonB) {
//			buttonB.setText(btnDefaultText.get(buttonB) + ((activity.pressedB()) ? " *" : " P"));
			activity.pressedB();
			selectedButton = SelectedButton.B;
			
		} else if (v == buttonX) {
//			buttonX.setText(btnDefaultText.get(buttonX) + ((activity.pressedX()) ? " *" : " P"));
			activity.pressedX();
			selectedButton = SelectedButton.X;
			
		} else if (v == buttonY) {
//			buttonY.setText(btnDefaultText.get(buttonY) + ((activity.pressedY()) ? " *" : " P"));
			activity.pressedY();
			selectedButton = SelectedButton.Y;
			
		} else if (v == buttonOK){
			
			boolean AisX = false;
			boolean AisY = false;
			boolean BisX = false;
			boolean BisY = false;
			
			if( (buttonA.getParent() == dynamicHolderX)
			 || (buttonB.getParent() == dynamicHolderY) ){
				AisX = true;
				AisY = false;
				BisX = false;
				BisY = true;
			}
			
			if( (buttonA.getParent() == dynamicHolderY)
			 || (buttonB.getParent() == dynamicHolderX) ){
				AisX = false;
				AisY = true;
				BisX = true;
				BisY = false;
			}
			
			//if paired, send to activity, which then calls the arbiter
			if( (AisX || AisY || BisX || BisY) != false ){
				activity.pressedOK(AisX, AisY, BisX, BisY);
				
			}
			else{
				Toast.makeText(getActivity(), "Must match at least one pair of tracks!", Toast.LENGTH_SHORT).show();
			}	
		}
		
		if(v == buttonA || v==buttonB || v== buttonX || v== buttonY){
			setButtonBackgrounds();
		}

		if (v!=buttonOK) {
			// makni oznaku seletiranosti iz gumba koji nisu pritisnuti
			for (Button button : btnDefaultText.keySet()) {
				if (button != v) {
					// TODO: ultra glupo, MEGA GLUPO
					button.setText(btnDefaultText.get(button));
				}
			}
		}
	}
	
	private void setButtonBackgrounds(){
		/*TODO: ugly piece of code, made before I learned about list/selector drawables,
		 * This work's however, so writing it in a nicer way is not a priority  */
		
		//removedLog.i(this.getClass().getName(),"setting button backgrounds");
		
		buttonA.setBackgroundResource(R.drawable.rounded_button_moveable);
		buttonB.setBackgroundResource(R.drawable.rounded_button_moveable);
//		buttonB.setBackgroundResource(R.drawable.button_moveable_with_depth); //some alternate button design
		buttonX.setBackgroundResource(R.drawable.roundedbuttonstatic);
		buttonY.setBackgroundResource(R.drawable.roundedbuttonstatic);
//		buttonY.setBackgroundResource(R.drawable.button_static_with_depth); //some alternate button design
		
		if(selectedButton == SelectedButton.A){
			buttonA.setBackgroundResource(R.drawable.rounded_button_moveable_selected);
		}
		else if(selectedButton == SelectedButton.B){
			buttonB.setBackgroundResource(R.drawable.rounded_button_moveable_selected);
		}
		else if(selectedButton == SelectedButton.X){
			buttonX.setBackgroundResource(R.drawable.rounded_button_static_selected);
			
			//if you want to animate button while playing do it something like this
//			buttonX.setBackgroundResource((isPlaying) ? R.drawable.animation1 : R.drawable.rounded_button_static_selected);
//			if(isPlaying){
//				((AnimationDrawable) buttonX.getBackground()).start();
//			}
		}
		else if(selectedButton == SelectedButton.Y){
			buttonY.setBackgroundResource(R.drawable.rounded_button_static_selected);
		}
		
	}
	
	

	@Override
	public void initialize(){
		
		//place buttons to correct starting positions
		((ViewGroup) buttonA.getParent()).removeView(buttonA);
		((ViewGroup) buttonB.getParent()).removeView(buttonB);
		startHolderA.addView(buttonA);
		startHolderB.addView(buttonB);
		
		setGlobalTextViewVisibility();
		
		onClick(buttonX);	
	}
	
}
