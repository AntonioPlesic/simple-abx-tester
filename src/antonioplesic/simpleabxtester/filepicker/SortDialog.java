package antonioplesic.simpleabxtester.filepicker;

import antonioplesic.simpleabxtester.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

public class SortDialog extends DialogFragment{
	
	int sortType;
	
	RadioGroup radioGroup;
	RadioButton radioButton1;
	
	public interface SortSelectionDialogListener{
		public void receiveSortSelectionResult(int result);
	}
	
	public SortDialog(){
		this.sortType = MyComparator.SORT_NONE;
	}
	
	public SortDialog(int currentSortType){
		this.sortType = currentSortType;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt("sortType", sortType);
		
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		if(savedInstanceState!=null){
			sortType = savedInstanceState.getInt("sortType");
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		radioGroup = (RadioGroup) inflater.inflate(R.layout.sort_dialog_layout, null);
		
//		//nacin na koji dobivas pojedine elemente
//		radioButton1 = (RadioButton) radioGroup.findViewById(R.id.radioButton1);
//		radioButton1.setText("probni text");
		
		if(sortType!=0){	
			switch (sortType) {
			case MyComparator.SORT_AtoZ:
				radioGroup.check(R.id.radioButton1);
				break;
			case MyComparator.SORT_ZtoA:
				radioGroup.check(R.id.radioButton2);
				break;
			case MyComparator.SORT_CREATED_ASC:
				radioGroup.check(R.id.radioButton3);
				break;
			case MyComparator.SORT_CREATED_DESC:
				radioGroup.check(R.id.radioButton4);
				break;
			default:
				//none is checked
				break;
			}
		}
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				//removedLog.w(this.getClass().getName(),"Checked changed");
				
				switch (checkedId) {
				case R.id.radioButton1:
					//removedLog.w(this.getClass().getName(),"Pritisnut radio1");
					sortType = MyComparator.SORT_AtoZ;
					break;
				case R.id.radioButton2:
					//removedLog.w(this.getClass().getName(),"Pritisnut radio2");
					sortType = MyComparator.SORT_ZtoA;
					break;
				case R.id.radioButton3:
					//removedLog.w(this.getClass().getName(),"Pritisnut radio3");
					sortType = MyComparator.SORT_CREATED_ASC;
					break;
				case R.id.radioButton4:
					//removedLog.w(this.getClass().getName(),"Pritisnut radio4");
					sortType = MyComparator.SORT_CREATED_DESC;
					break;
				default:
					break;
				}
				
				((SortSelectionDialogListener) getTargetFragment()).receiveSortSelectionResult(sortType);
				dismiss();
				
				
			}
		});
		
		builder.setView(radioGroup);
		
		builder.setTitle("Sort by:");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		
		return builder.create();
	}
	

}
