package antonioplesic.simpleabxtester.filepicker;

import java.util.concurrent.atomic.AtomicInteger;

import antonioplesic.simpleabxtester.R;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class CreateDirectoryDialog extends DialogFragment {
		
	EditText editText;
	
	public interface SelectionResultInterface{
		public void receiveSelectionResult(String result);
	}
		
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		
////		editText = (EditText) getView().findViewById(R.layout.create_folder_edit_text);
//	}
		
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		editText = (EditText) inflater.inflate(R.layout.create_folder_edit_text, null);
		
//		builder.setView(inflater.inflate(R.layout.create_folder_edit_text, null));
		builder.setView(editText);
		
		builder.setTitle("Enter directory name");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				((SelectionResultInterface) getTargetFragment()).receiveSelectionResult(editText.getText().toString());
				
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				 
				
			}
		});
		
		return builder.create();	
	}
	

}
