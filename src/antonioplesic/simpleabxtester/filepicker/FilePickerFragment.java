package antonioplesic.simpleabxtester.filepicker;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import antonioplesic.simpleabxtester.R;
import antonioplesic.simpleabxtester.filepicker.SortDialog.SortSelectionDialogListener;
import antonioplesic.simpleabxtester.lifted.StorageUtils;
import antonioplesic.simpleabxtester.lifted.StorageUtils.StorageInfo;



import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.CursorJoiner.Result;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FilePickerFragment extends Fragment implements CreateDirectoryDialog.SelectionResultInterface, SortSelectionDialogListener {
	
	public static final String FILE_PICKER_PREFERENCES	= "filePickerPreferences";
	public static final String SORT_MODE = "sortMode";
	public static final String LAST_DIRECTORY = "lastDirectory";
	
//	String currentDirectoryPath = "/storage/emulated/0";
	String currentDirectoryPath = "";
	String selectedPathUpToItem = null;
	boolean[] selectedItems;
	private String newDirectoryName = null; //there is no point in this being a field
	/*TODO: probaj napraviti bez da se sejva, receiveResult nek odmah napravi folder.
	 * Mislim da se orientation ne moze promijeniti prije nego se cijeli on receive izvrsi,
	 * tako da nema bojazni da ce rezultanti string nestati
	 */
	
	
	TextView textViewCurrentDirectory;
	ListView listView;
	Button upButton;
	Button okButton;
	Button createFolder;
	Button sortByButton;
//	Button footerOkButton;
	Button okButton2;
	
	
	private MyAdapter mAdapter;
	
	ActionMode mActionMode;

	
	private void loadLastDirectory() {
		
		//removedLog.i(this.getClass().getName(),"LOADING SAVED DIRECTORY");
		
		SharedPreferences m = getActivity().getSharedPreferences(FILE_PICKER_PREFERENCES, 0);
		currentDirectoryPath = m.getString(LAST_DIRECTORY, ""); //TODO: neka "" bude neka konstanta NONE_SELECTED = "";
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//removedLog.i(this.getClass().getName(),"ON DESTROY");
		
		if(getActivity().isFinishing()){
			//removedLog.i(this.getClass().getName(),"ON DESTROY, ACTIVITY FINISHING");
			//save current directory
			SharedPreferences m = getActivity().getSharedPreferences(FILE_PICKER_PREFERENCES, 0);
			m.edit().putString(LAST_DIRECTORY, currentDirectoryPath).commit();
		}
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		//TODO: call first or last? First I think.
		super.onCreate(savedInstanceState);
		
		/*"hack" to force action overflow regardless of physical menu key
		 * (i consider it a hack since it is not part of the api)
		 * Sorce: http://stackoverflow.com/a/11438245 
		 */
		try {
	        ViewConfiguration config = ViewConfiguration.get(getActivity());
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception ex) {
	        // Ignore
	    }
		
		setHasOptionsMenu(true);
		
		//removedLog.i(this.getClass().getName(),"ON CREATE");
		
		if(savedInstanceState!=null){
			currentDirectoryPath = savedInstanceState.getString("currentDirectoryPath");
			selectedPathUpToItem = savedInstanceState.getString("selectedPathUpToItem");
			selectedItems = savedInstanceState.getBooleanArray("selectedItems");
			if(isSomethingSelected()){
				mActionMode = getActivity().startActionMode(mActionModeCallback);
			}
			newDirectoryName = savedInstanceState.getString("newDirectoryName");			
		}
		else{
			loadLastDirectory();
		}
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		inflater.inflate(R.menu.picker_menu, menu);
		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.up:
			upOneLevel();
			return true;
		case R.id.sort:
			showSortDialog();
			return true;
		case R.id.create_folder:
			showCreteDirectoryDialog();
			return true;
//			//handling moved to activity
//			case android.R.id.home:
//				getActivity().finish();
//				return true;
		default:
			return false;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		//removedLog.i(this.getClass().getName(),"ON CREATE VIEW");
		
		View v = inflater.inflate(R.layout.picker_fragment_layout, container, false);
		
		textViewCurrentDirectory = (TextView) v.findViewById(R.id.dragger_textView_Y);
		
		
		listView = (ListView) v.findViewById(R.id.filePickerListView);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				//removedLog.w(this.getClass().getName(),"ON ITEM CLICK, position: " + position);
				File clickedFile = (File) listView.getItemAtPosition(position);
				
//				//isprintaj view, position i id da vidim o cem se radi
//				//removedLog.w("pickerFragmentOnItemClick","adapterView where click happened" + parent);
//				//removedLog.w("pickerFragmentOnItemClick","view that was clicked within AdapterView " + view);
//				//removedLog.w("pickerFragmentOnItemClick","position:" + position);
//				//removedLog.w("pickerFragmentOnItemClick","row id:" + id);
				
				
				//clicking a directory navigates inside that directory
				if(clickedFile.isDirectory()){
					if(!inRoot()){
						currentDirectoryPath = currentDirectoryPath + "/" + clickedFile.getName();
					}
					else{
						currentDirectoryPath = clickedFile.getAbsolutePath();
					}
					removeSelection();
					dismissActionMode();
					updateList();
				}
				
				//clicking a file selects that file
				if(clickedFile.isFile()){
//					selectedItems[position] = !selectedItems[position];
//					mAdapter.notifyDataSetChanged();
					
//					CheckBox checkbox = (CheckBox)  listView.getChildAt(position).findViewById(R.id.picker_item_check_box);
					CheckBox checkbox = (CheckBox)  view.findViewById(R.id.picker_item_check_box);
					
					checkbox.setChecked(!checkbox.isChecked());
					
//					//also return
//					returnResult();
					
				}

			}
		});
		
		
//		//=====================================================================
//		//Functionality transfered to contextActionBar
//		//=====================================================================
//
//		footerOkButton = new Button(getActivity());
//		footerOkButton.setText("OK");
//		footerOkButton.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				returnResult();
//				
//			}
//		});
//		
//		((ViewGroup) v).addView(footerOkButton, ((ViewGroup)v).getChildCount());
//		
//		if(selectedItems==null){
//			footerOkButton.setVisibility(View.GONE);
//		}
//		else{
//			footerOkButton.setVisibility(View.VISIBLE);
//		}
//
//		upButton = (Button) v.findViewById(R.id.upButton);
//		upButton.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				upOneLevel();
//				
//			}
//		});
//		
//		
//		okButton = (Button) v.findViewById(R.id.button2);
//		okButton.setVisibility(  (getActivity().getCallingActivity()!=null) ? View.VISIBLE : View.GONE);
//		okButton.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				returnResult();
//				
//			}
//		});
////		//vidi oneliner malo iznad
////		if(getActivity().getCallingActivity() == null){
////			okButton.setVisibility(View.GONE);
////		}
//		
//		okButton2 = (Button) v.findViewById(R.id.button3);
//		okButton2.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				returnResult();
//				
//			}
//		});
//		
//		setVisibility(okButton,okButton2);
//		
//		
//		createFolder = (Button) v.findViewById(R.id.button1);
//		createFolder.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				showCreteDirectoryDialog();
//				
//			}
//		});
//		
//		
//		sortByButton = (Button) v.findViewById(R.id.buttonSortBy);
//		sortByButton.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				showSortDialog();
//				
//			}
//		});
		

		updateList();
		
		return v;
	}


//	//Not used due to buttons being replaced by contextActionBar
//
//	private void setVisibility(Button... buttons) {
//		int valueToSet;
//		if(isSomethingSelected()){
//			valueToSet = View.VISIBLE;
//		}
//		else{
//			valueToSet = View.GONE;
//		}
//		for(Button button : buttons){
//			button.setVisibility(valueToSet);
//		}
//	}

	private boolean isSomethingSelected(){
		if(selectedItems==null){
			return false;
		}
		return contains(selectedItems, true);
	}
	
	private boolean contains(boolean[] array, boolean value){
		for(boolean b:array){
			if(b==value){
				return true;
			}
		}
		return false;
	}
	
	
	//=========================================================================
	// Create sort dialog
	//=========================================================================
	
	private void showSortDialog(){
		SortDialog sortDialog = new SortDialog(getComparatorType());
		sortDialog.setTargetFragment(this, 0);
		sortDialog.show(getFragmentManager(), "sortDialog");
	}
	
	@Override
	public void receiveSortSelectionResult(int result) {
		saveComparatorType(result);
//		dummyLongJob(); 
		updateList();
		
	}
	
	
	

	//=========================================================================
	// Create directory dialog
	//=========================================================================
	
	private void showCreteDirectoryDialog(){
		if(!inRoot()){
			CreateDirectoryDialog createDirectoryDialog = new CreateDirectoryDialog();
			createDirectoryDialog.setTargetFragment(this, 0); //0 cause i'm not using that parameter really, calling back with interface
			//But maybe you should invoke onactivity result and create folder there?
			createDirectoryDialog.show(getFragmentManager(), "createDirectoryDialog");
		}
		else{
			Toast.makeText(getActivity(), "Can't make new directory here", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void receiveSelectionResult(String result) {
		this.newDirectoryName = result; //pointless really
		//removedLog.w(this.getClass().getName(),"Received new folder name: " + result);
		
//		dummyLongJob();
		
		if(newDirectoryName != null && !inRoot()){
			File newDirectory = new File(currentDirectoryPath + "/" + newDirectoryName);
			boolean success = newDirectory.mkdir();
			//removedLog.w(this.getClass().getName(),"" + success);
			updateList();
			
			
		}
		
	}

	private void dummyLongJob() {
		/*sluzi za dokazivanje samom sebi da se orijentacija nece promijeniti sve dok se ova metoda
		 * u potpunosti ne izvrsi i da nema potrebe za sejvanjem rezultata.
		 * Medjutim ako callbackani posao dugo traje, onda nastaju problemi,
		 * jer se dialog ne dismissa odmah, vec tek nakon sto callback zavrsi.
		 * Sto ako zelim poceti s "callbackom" tek nakon sto se dialog dismissa.
		 */
		for(int i = 0;i < 10000;i++){
			for(int j = 0; j< 100000;j++){
				;
			}
			//removedLog.w(this.getClass().getName(), "Long action " + i);
		}
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		outState.putString("currentDirectoryPath", currentDirectoryPath);
		outState.putString("selectedPathUpToItem", selectedPathUpToItem);
		outState.putBooleanArray("selectedItems", selectedItems);
		outState.putString("newDirectoryName",newDirectoryName);
		
		//TODO: call first or last?
		super.onSaveInstanceState(outState);
		
	}
	

	protected void upOneLevel() {
//		//already in top level ("disk" selection)
//		if(currentDirectoryPath.equals("")){
//			currentDirectoryPath = "";
//			return;
//		}
		//in one of the disk roots
		if(!inRoot() && !getRootDirectoriesList().contains(new File(currentDirectoryPath))){
			currentDirectoryPath = new File(currentDirectoryPath).getParent();
		}
		else{
			currentDirectoryPath = "";
		}
		removeSelection(); //but should the selection really be removed? See javadoc for removeSelection() method.
		dismissActionMode();
		updateList();
	}


	private void dismissActionMode() {
		if(mActionMode!=null){
			mActionMode.finish(); //can't be part of removeSelection as it leads to stack overflow; and i really want removeSelection call in onDestroyActionMode, as then selection is removed whatever way the action mode is closed
		}
	}


	/** Unselects the currently selected items. (for example when navigating up-down through the file hierarchy). <p>
	 * 
	 * Selection should probably be removed depending on the mode: <br><br>
	 * 
	 * 1) Single selection mode:<br>
	 * Remove selection. If screen changes, the user expects that
	 * the previous screen is invalidated (i.e. user pressed back/up because she changed
	 * her mind about selected file, and wants to navigate to and select another one). 
	 * <p>
	 * 2) Multiple selection mode:<br>
	 * Do not remove selection. Maybe user wants to select two files that are not in the same
	 * directory. That seems like a reasonable scenario to me, but for example the built-in
	 * file explorer for Samsung Galaxy S3 clears selection even in such cases. I actually
	 * believe, but will not investigate further for now, that most file explorers behave
	 * just like the S3 default one.
	 * <p>
	 * Conclusion:
	 * For now, since I only need single selection mode, i will not bother with scenario 2
	 * at all. This particular file/directory picker is currently not supposed to evolve 
	 * into a full fledged file explorer (maybe one day). I would actually prefer that users
	 * of the app for which this picker was developed (ABX comparator) use some other, dedicated
	 * and much richer file explorer available on the Play Store (as I did during the development,
	 * this picker is a very late addition).However, I cannot depend on users ability to correctly 
	 * install it, and they should not be forced to do so, because some could see that as a hassle, 
	 * which would negatively effect the user experience. So, the app uses this lightweight 
	 * file/folder picker as an in-built one. 
	 */
	private void removeSelection() {
		selectedItems = null;
		
//		if(mActionMode!=null)
//			mActionMode.finish();  //causes stackoverflow, cause finish() calls onDestroyActionMode(), which itself calls removeSelection()
		
//		setVisibility(okButton,okButton2); //functionality replaced by contextActionBar
//		Arrays.fill(selectedItems, false);
		
		selectedPathUpToItem = "";// Why not null? See the comment below. 
		/* Why not null? That way selectedPathUpToItem.equales(something) 
		 * would throw NullPointer exception. I do not feel like handling it
		 */ 
		
		mAdapter.notifyDataSetChanged();
		
	}
	
	private void createList(){
		mAdapter = new MyAdapter(getCurrentDirectorContent());
	}
	
	private void setList(){
		listView.setAdapter(mAdapter);
	}
	

	private void updateList() {
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1 , getCurrentDirectorContent());
//		listView.setAdapter(adapter);
		
		mAdapter = new MyAdapter(getCurrentDirectorContent());
		listView.setAdapter(mAdapter);
		
//		selectedItems= new boolean[getCurrentDirectorContent().length];
//		selectedPathUpToItem = "";
		
		
		textViewCurrentDirectory.setText(currentDirectoryPath);
	}
	
	private File[] getCurrentDirectorContent(){
		
		File currentDirectory = new File(currentDirectoryPath);
		//removedLog.w(this.getClass().getName(),"Currently shown folder: " + currentDirectoryPath);
		
		/*if current path is member of root directories
		 * then present those root directories as contetnt
		 */
		if(inRoot()){
			//removedLog.w(this.getClass().getName(),"Trebam vratiti rootove");

			return getRootDirectories();
		}
		
		File[] containedFiles = currentDirectory.listFiles(new FileFilter() {
		
			@Override
			public boolean accept(File pathname) {
//				return !pathname.isHidden() && pathname.isDirectory();
				return !pathname.isHidden();
			}
		});
		//removedLog.w(this.getClass().getName(),"current directory exists?" + currentDirectory.exists() );
		//removedLog.w(this.getClass().getName(),"Contained files == null?" + (containedFiles == null) );
		
		if(containedFiles==null){
			// Current directory not existing anymore is the most probable cause for this.
			/* Even though this is an exceptional situation, it is not handled by an exception
			 * because exception would occur only in the case of sorting (only use of variable
			 * "containedFiles" in this method), which is not mandatory, and could be removed.
			 * I could also return this null value and let callers take appropriate action,
			 * but i find this solution better.
			 */			
			currentDirectoryPath = "";
			return getRootDirectories();	
		}
		
		Arrays.sort(containedFiles, new MyComparator(getComparatorType()));
		
		return containedFiles;
	}

	private boolean inRoot() {
		return currentDirectoryPath.equals("");
	}
	
	private void returnResult() {
		
		String result = getSelectedPath();
		
		if(result!=null){
			//removedLog.w(this.getClass().getName(),"selection result:\n" + getSelectedPath());
			
			Intent resultIntent = new Intent();
			resultIntent.setData(Uri.fromFile(new File(result)));
			getActivity().setResult(Activity.RESULT_OK, resultIntent);
			getActivity().finish();
		}
		else{
			//removedLog.w(this.getClass().getName(),"nothing is selected");
		}
		
		
	}
	
	private String getSelectedPath(){
		
		if(selectedItems!=null){
			
			//=================================================================
			//TODO: WTF, zasto ne radi s indexOf?
			//=================================================================
			
			int position = -1; 
			for(int i = 0; i<selectedItems.length; i++){
				if(selectedItems[i] == true){
					position = i;
				}
			}
			
			//iz nekog razlog asList vraca listu boolean[], a ne listu boolean
//			int position = Arrays.asList(selectedItems).indexOf(Boolean.valueOf(false)); //zake ovo uvek vraca -1?
//			int position = Arrays.asList(selectedItems).indexOf(false); //zake ovo uvek vraca -1?
			//nasao objasnjnje na stackoverflowu
			
			//==================================================================
			
			//removedLog.w("bla","" + position);
			if(position!=-1){
//				return selectedPathUpToItem + "/" +  mAdapter.getItem(position);
				return ((File) mAdapter.getItem(position)).getPath();
			}			
		}
		return null;
	}
	
	//TODO: implement in case multiple selection gets implemented
	private String getSelectedPaths(){
		return null;
	}

	private static class ItemViewHolder{
		public CheckBox checkbox;
		public TextView textView;
		public ImageView imageView;
	}
	
	private class MyAdapter extends BaseAdapter {

		private File[] objects;

		public MyAdapter(File[] objects){

			super(); //nije li ovo implicitno, lol, kak to neznam
			this.objects = objects;
		}

		@Override
		public int getCount() {
			return objects.length;
		}

		@Override
		public Object getItem(int position) {
			return objects[position];
		}

		@Override
		public long getItemId(int position) {
			return position; 
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			//utility holder for views that comprise the item view being created or reused
			ItemViewHolder holder = null;

			//if view is not being reused, it should be inflated from scratch
			if(convertView==null){
				//removedLog.w(this.getClass().getName(),"inflating new item view");

				convertView = getActivity().getLayoutInflater().inflate(R.layout.picker_item_layout, parent, false);

				holder = new ItemViewHolder();

				//checkbox subitem (accesory)
				holder.checkbox = (CheckBox) convertView.findViewById(R.id.picker_item_check_box);
				//TODO: stavi listener koji nije anoniman
				holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

						if(selectedItems==null){
							selectedItems = new boolean[getCurrentDirectorContent().length];
						}

						//TODO: napravi bez try blocka, kak onaj lik savjetuje
						try {
							int position = ((ListView) buttonView.getParent().getParent()).getPositionForView(buttonView);
							if(isChecked){
								selectedPathUpToItem = currentDirectoryPath;
								//no multiple selection allowed for now
								Arrays.fill(selectedItems, false);
								notifyDataSetChanged();
								selectedItems[position] = true;
								
								if(mActionMode==null){
									mActionMode = getActivity().startActionMode(mActionModeCallback);
								}
								
							}
							else{
								selectedItems[position] = false;
								if(!isSomethingSelected()){
									mActionMode.finish();
								}
							}
//							setVisibility(okButton,okButton2); //functinality replaced by contextActionBar
						} catch (Exception e) {
							//XXX: really? do it right -.-
						}
					}
				});

				//texview subitem
				holder.textView = (TextView) convertView.findViewById(R.id.picker_item_text_view);

				//icon subitem (used to represent directory, generic file, mp3 file, flac file ...)
				holder.imageView = (ImageView) convertView.findViewById(R.id.picker_item_image_view);

				convertView.setTag(holder);			
			}
			//if view is being reused
			else{
				holder = (ItemViewHolder) convertView.getTag();
			}

			//if item is logically checked, then check it's checkbox
			if( currentDirectoryPath.equals(selectedPathUpToItem) && selectedItems!=null && selectedItems[position] == true){
				holder.checkbox.setChecked(true);
			}
			//else uncheck it's checkbox
			else{
				holder.checkbox.setChecked(false);
			}

			//set text displayed in item (file/directory name)
			if(!currentDirectoryPath.equals("")){
				holder.textView.setText(objects[position].getName());
			}
			else{
				holder.textView.setText(objects[position].getAbsolutePath());
			}
			

			//file/directory that the item (or its holder) points to
			File holderFile = (File) objects[position];

			
			
			//=================================================================
			// Setting file/directory icons
			//=================================================================
			//TODO: MAJOR Use separate icons for different screen densities!
			
			//if directroy, set the folder icon
			if( holderFile.isDirectory() ){
				//if ordinary directory
				if(!inRoot()){
					holder.imageView.setImageResource(R.drawable.ic_myfolder);
					//				String fileName = holderFile.getName();
					//				//removedLog.w(this.getClass().getName(), fileName);
					//removedLog.w(this.getClass().getName(), "folder");
				}
				//else it is some kind of a root directory (i.e. phoneStorage, sdCard1, sdCard2, USB1 ...)
				else{
					/*Position 1 is "hardcoded" to represent device's "internal" storage location
					 * Yes, it's bad way to do things. Despite that, this is how it will stay like
					 * for a while. */
					if(position==0){
						holder.imageView.setImageResource(R.drawable.ic_phone);
					}
					/* Else, it is presumed to be some sort of removable storage, all represented by an
					 * microSD card icon (for now). //TODO: Should differentiate beetween cards and USBs though. 
					 * Sticking to SD cards for now because it is the most common port on android devices. Quick
					 * and drity way, which will not work a lot of times i guess is to check if 
					 * path.toLower().contains("usb")
					 * */
					else{
						holder.imageView.setImageResource(R.drawable.ic_sdcard);
					}
				}
			}
			//else if it is directory...
			else if(holderFile.isFile()){

				//...then set icon depending on extension
				String fileName = holderFile.getName();
				//				//removedLog.w(this.getClass().getName(), fileName);

				//mp3
				if(fileName.endsWith(".mp3")){
					holder.imageView.setImageResource(R.drawable.ic_mp3file);
					//removedLog.w(this.getClass().getName(), "mp3");
				}
				//flac
				else if(fileName.endsWith(".flac")){
					holder.imageView.setImageResource(R.drawable.ic_flacfile);
					//removedLog.w(this.getClass().getName(), "flac");
				}
				//wav
				else if(fileName.endsWith(".wav")){
					holder.imageView.setImageResource(R.drawable.ic_wavfile);
					//removedLog.w(this.getClass().getName(), "wav");
				}
				//other files
				else{
					holder.imageView.setImageResource(R.drawable.ic_file);
					//removedLog.w(this.getClass().getName(), "other");
				}	
			}
			/*if not directory or file.
			 * This probably can't happen normally, but, a File object could point
			 * to invalid location, then this case would apply, but I'm pretty sure
			 * in that case program would break before reaching this. This could 
			 * happen maybe if currentDirectory.getFiles() returns invalid files.
			 */
			else{

			}
			
			//=================== end of section ==============================

			
			return convertView;
		}

	}
	
	/**Gets comparator type saved in preferences, to use when listing files/folders.
	 * 
	 * @return
	 */
	public int getComparatorType(){
		SharedPreferences m = getActivity().getSharedPreferences(FILE_PICKER_PREFERENCES, 0);
		return m.getInt(SORT_MODE, MyComparator.SORT_AtoZ);
	}
	
	public void saveComparatorType(int sortMode){
		SharedPreferences m = getActivity().getSharedPreferences(FILE_PICKER_PREFERENCES, 0);
		Editor editor = m.edit();
		editor.putInt(SORT_MODE, sortMode);
		editor.commit();
	}
	
	
	public File[] getRootDirectories(){
		
		File[] myRootDirectories;
		
		List<StorageInfo> roots = StorageUtils.getStorageList();
		myRootDirectories = new File[roots.size()];
		
		/*This looks ugly, and thats why I love Python (enumerate)
		 * In this case, maybe it would be better to use traditional for loop:
		 * 
		 *		for(int i = 0; i<roots.size(); i++){
		 *			myRootDirectories[i] = roots.get(i);
		 *		}
		 * 
		 * But that reduces clarity further imo.
		 */
		int i = 0;
		for(StorageInfo rootInfo : roots){
			myRootDirectories[i] = new File( rootInfo.path );
			i++;
		}
		
		return myRootDirectories;
	}
	
	public List<File> getRootDirectoriesList(){
		
		List<File> myRootDirectories;
		
		List<StorageInfo> roots = StorageUtils.getStorageList();
		myRootDirectories = new ArrayList<File>(roots.size());
		
		for(StorageInfo rootInfo : roots){
			myRootDirectories.add(new File(rootInfo.path));
		}

		return myRootDirectories;
	}
	
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.picker_item_selected_contextual_action_menu, menu);
			
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
						
			return false;
		}
		
		
		
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.action_cancel:
				
				mode.finish();
				return true;

			default:
				returnResult();
				return true;
			}
			
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			//removedLog.d(this.getClass().getName(), "ONDESTROYACTIONMODE");
			removeSelection();
			mActionMode = null;
		}
	};
	
	
}
