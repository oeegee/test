/**
 * 
 */
package com.android.phone.videophone.menu;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.android.phone.R;



/**
 * 
 * 2depth Camera Setting UI Class
 * It has two adapter(extends BaseAdapter) , and has item-click event handler. 
 * This class managed only UI action and animation.
 * 
 * @author hogyun.kim
 */

//Please reference to {@link android.widget.ZoomButtonsController} for detail
//information about adding window to WindowManager.
public class OnScreenSettings {
	@SuppressWarnings("unused")
	private static final String TAG = "OnScreenSettings";
	private static final int MSG_POST_SET_VISIBLE = 1;

    private static final boolean DBG = 
        (SystemProperties.getInt("ro.debuggable", 0) == 1);
	
	public static final int DIRECTION_LEFT = 0;
	public static final int DIRECTION_RIGHT = 1;
	public static final int DIRECTION_UP = 2;
	public static final int DIRECTION_DOWN = 3;




	public interface OnVisibilityChangedListener {
		public void onVisibilityChanged(boolean visibility);
	}

	private LayoutParams mContainerLayoutParams;
	private final Context mContext;
	private final Container mContainer;
	private final WindowManager mWindowManager;
	private final View mOwnerView;
	private ListView mMainMenu;
	private ListView mSubMenu;
	private View mMainPanel;
	private boolean mIsVisible = false;
	private OnVisibilityChangedListener mVisibilityListener;
	private MainMenuAdapter mMainAdapter;
	// private SubMenuAdapter mSub

	private final LayoutInflater mInflater;

	// We store the override values here. For a given preference,
	// if the mapping value of the preference key is not null, we will
	// use the value in this map instead of the value read from the preference
	//
	// This is design for the scene mode, for example, in the scene mode
	// "Action", the focus mode will become "infinite" no matter what in the
	// preference settings. So, we need to put a {pref_camera_focusmode_key,
	// "infinite"} entry in this map.
	private HashMap<String, String> mOverride = new HashMap<String, String>();

	//hgkim add it
	private ArrayList<Preference> mPreferenceList = null;
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_POST_SET_VISIBLE:
				setVisible(true);
				break;
			}
		}
	};

	public OnScreenSettings(View ownerView) {
		mContext = ownerView.getContext();
		mInflater = (LayoutInflater)
		mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mWindowManager = (WindowManager)
		mContext.getSystemService(Context.WINDOW_SERVICE);
		mOwnerView = ownerView;
		mContainer = createContainer();
	}

	/** 
	 * //hgkim - pre show 2depth
	 * by hgkim */
	 public void initSubView()
	{
		if(null != mPreferenceList)
		{
			//    		int index = getSelectedIndex();
			Preference preference = mPreferenceList.get(0);
			if(preference != null)
			{
				SubMenuAdapter adapter = new SubMenuAdapter(
						mContext, (ListPreference) preference);
				mSubMenu.setAdapter(adapter);
				mSubMenu.setOnItemClickListener(adapter);
				showSubMenu();
			}            
		}
	}

	 public boolean isVisible() {
		 return mIsVisible;
	 }

	 public void setOnVisibilityChangedListener(
			 OnVisibilityChangedListener listener) {
		 mVisibilityListener = listener;
	 }

	 static boolean isFirst = true;
	 public void setVisible(boolean visible) {
		 mHandler.removeMessages(MSG_POST_SET_VISIBLE);
		 if (visible) {
			 if (mOwnerView.getWindowToken() == null) {
				 /*
				  * We need a window token to show ourselves, maybe the owner's
				  * window hasn't been created yet but it will have been by the
				  * time the looper is idle, so post the setVisible(true) call.
				  */
				 mHandler.sendEmptyMessage(MSG_POST_SET_VISIBLE);
				 return;
			 }
		 }

		 if (mIsVisible == visible) {
			 return;
		 }
		 mIsVisible = visible;

		 if (visible) 
		 {
			 // Update main adapter before show up
			 if (mMainAdapter != null)
			 {
				 mMainAdapter.notifyDataSetChanged();
			 }
			 //    	 if (mSubAdapter != null)
			 //				mSubAdapter.notifyDataSetChanged();

			 if (mContainerLayoutParams.token == null) 
			 {
				 mContainerLayoutParams.token = mOwnerView.getWindowToken();
			 }

			 mWindowManager.addView(mContainer, mContainerLayoutParams);
			 updateLayout();
		 } 
		 else 
		 {
			 // Reset the two menus
			 mWindowManager.removeView(mContainer);
		 }

		 if (mVisibilityListener != null) 
		 {
			 mVisibilityListener.onVisibilityChanged(mIsVisible);
		 }
	 }

	 // Override the preference settings, if value == null, then disable the
	 // override.
	 public void overrideSettings(String key, String value) {
		 if (value == null) {
			 if (mOverride.remove(key) != null && mMainAdapter != null) {
				 mMainAdapter.notifyDataSetChanged();
			 }
		 } else {
			 if (mOverride.put(key, value) == null && mMainAdapter != null) {
				 mMainAdapter.notifyDataSetChanged();
			 }
		 }
	 }

	 public void updateLayout() {
		 // if the mOwnerView is detached from window then skip.
		 if (mOwnerView.getWindowToken() == null) return;
		 Display display = mWindowManager.getDefaultDisplay();
		 //by hgkim 수정
		 /*        mContainerLayoutParams.x = 0;
     mContainerLayoutParams.y = 0;

     mContainerLayoutParams.width = display.getWidth() / 2;
     mContainerLayoutParams.height = display.getHeight();
		  */
		 mContainerLayoutParams.x = 0;
		 mContainerLayoutParams.y = 850;//750;

		 mContainerLayoutParams.width = display.getWidth();
		 mContainerLayoutParams.height = display.getHeight()/2;


		 if (mIsVisible) {
			 mWindowManager.updateViewLayout(mContainer, mContainerLayoutParams);
		 }
	 }

	 private void showSubMenu() {
		 //slideOut(mMainMenu, DIRECTION_LEFT);
		 slideIn(mSubMenu, DIRECTION_RIGHT);    	
		 //        mSubMenu.requestFocus();
	 }

	 private void closeSubMenu() {
		 slideOut(mSubMenu, DIRECTION_RIGHT);
		 //        slideIn(mMainMenu, DIRECTION_LEFT);
	 }
	 ///
	 //hgkim 추가 
	 ///
	 public Animation slideOut(View view, int to) {
		 view.setVisibility(View.INVISIBLE);
		 Animation anim = null;
		 //        switch (to) {
		 //            case DIRECTION_LEFT:
		 //                anim = new TranslateAnimation(0, -view.getWidth(), 0, 0);
		 //                break;
		 //            case DIRECTION_RIGHT:
		 //                anim = new TranslateAnimation(0, view.getWidth(), 0, 0);
		 //                break;
		 //            case DIRECTION_UP:
		 //                anim = new TranslateAnimation(0, 0, 0, -view.getHeight());
		 //                break;
		 //            case DIRECTION_DOWN:
		 //                anim = new TranslateAnimation(0, 0, 0, view.getHeight());
		 //                break;
		 //            default:
		 //                throw new IllegalArgumentException(Integer.toString(to));
		 //        }
		 //        anim.setDuration(500);
		 //        view.startAnimation(anim);
		 return anim;
	 }

	 public Animation slideIn(View view, int from) {
		 view.setVisibility(View.VISIBLE);
		 Animation anim = null;
		 //        switch (from) {
		 //            case DIRECTION_LEFT:
		 //                anim = new TranslateAnimation(-view.getWidth(), 0, 0, 0);
		 //                break;
		 //            case DIRECTION_RIGHT:
		 //                anim = new TranslateAnimation(view.getWidth(), 0, 0, 0);
		 //                break;
		 //            case DIRECTION_UP:
		 //                anim = new TranslateAnimation(0, 0, -view.getHeight(), 0);
		 //                break;
		 //            case DIRECTION_DOWN:
		 //                anim = new TranslateAnimation(0, 0, view.getHeight(), 0);
		 //                break;
		 //            default:
		 //                throw new IllegalArgumentException(Integer.toString(from));
		 //        }
		 //        anim.setDuration(500);
		 //        view.startAnimation(anim);
		 return anim;
	 }    


	 private Container createContainer() {
		 LayoutParams lp = new LayoutParams(
				 LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		 lp.flags = LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
		 lp.gravity = Gravity.TOP | Gravity.LEFT;
		 lp.height = LayoutParams.WRAP_CONTENT;
		 lp.width = LayoutParams.WRAP_CONTENT;
		 lp.type = LayoutParams.TYPE_APPLICATION_PANEL;
		 lp.format = PixelFormat.OPAQUE;
		 lp.windowAnimations = R.style.Animation_OnScreenMenu;

		 mContainerLayoutParams = lp;

		 Container container = new Container(mContext);
		 container.setLayoutParams(lp);

		 mInflater.inflate(R.layout.vt_on_screen_menu, container);

		 mMainPanel = container.findViewById(R.id.main_panel);
		 mMainMenu = (ListView) container.findViewById(R.id.menu_view);
		 mSubMenu = (ListView) container.findViewById(R.id.sub_menu);

		 return container;
	 }

	 //	private class GripperTouchListener implements View.OnTouchListener {
	 //		public boolean onTouch(View view, MotionEvent event) {
	 //			switch (event.getAction()) {
	 //			case MotionEvent.ACTION_DOWN:
	 //				return true;
	 //			case MotionEvent.ACTION_UP:
	 //				setVisible(false);
	 //				return true;
	 //			}
	 //			return false;
	 //		}
	 //	}

	 private boolean onContainerKey(KeyEvent event) {
		 switch (event.getKeyCode()) {
		 case KeyEvent.KEYCODE_CAMERA:
		 case KeyEvent.KEYCODE_FOCUS:
		 case KeyEvent.KEYCODE_BACK:
		 case KeyEvent.KEYCODE_MENU:
			 if (event.getAction() == KeyEvent.ACTION_UP) {
				 setVisible(false);
				 return true;
			 }
		 }
		 return false;
	 }

	 // Add the preference and it's children recursively to the given list. So
	 // that we can show the preference (and it's children) in the list view.
	 private static void addPreference(
			 Preference preference, ArrayList<Preference> list) {
		 list.add(preference);
		 if (preference instanceof PreferenceGroup) {
			 PreferenceGroup group = (PreferenceGroup) preference;
			 for (int i = 0, n = group.getPreferenceCount(); i < n; ++i) {
				 Preference child = group.getPreference(i);
				 addPreference(child, list);
			 }
		 }
	 }

	 public void setPreferenceScreen(PreferenceScreen screen) {
		 ArrayList<Preference> list = new ArrayList<Preference>();

		 // We don't want the screen add to the list, we add the first level
		 // preference here.
		 int count = screen.getPreferenceCount();
		 for (int i = 0, n = count; i < n; ++i) {
			 addPreference(screen.getPreference(i), list);
		 }
		 mMainAdapter = new MainMenuAdapter(mContext, list);
		 mMainMenu.setAdapter(mMainAdapter);
		 mMainMenu.setOnItemClickListener(mMainAdapter);

		 //hgkim
		 mPreferenceList = list;
	 }

	 public void setPreferenceValue(String key, String value) {		 
		 if (mPreferenceList != null)
		 {
			 Preference pref;
			 for (int i = 0, n = mPreferenceList.size(); i < n; i++)
			 {
				 pref = mPreferenceList.get(i);
				 if (pref.getKey().equals(key))
				 {
					 ((ListPreference)pref).setValue(value);					 
					 break;
				 }
			 }
		 }
	 }
	 
	 private View inflateIfNeed(
			 View view, int resource, ViewGroup root, boolean attachToRoot) {
		 if (view != null)
			 return view;
		 return mInflater.inflate(resource, root, attachToRoot);
	 }

	 /** 
	  * For GUI, finalize a member variable of MainMenuAdapter.
	  *   
	  * */
	 public void finalize()
	 {
	     if(DBG)
		 {
	         Log.d(TAG, "finalize() called!");
	         Log.d(TAG, "mSelectedGroup = " + mMainAdapter.mSelectedGroup);
		 }
		  

		 if(mMainAdapter != null)
		 {
			 //		 
			 mMainAdapter.mIsFirstCase = false;		 
			 //		mMainAdapter.mSelectedGroup = null;

		 }	 
	 }

	 // public int getSelectedIndex()
	 // {
	 //	 if(mMainAdapter != null)
	 //	 {
	 //		 return mMainAdapter.mSelectedIndex;
	 //	 }
	 //	 
	 //	 return 0;
	 // }
	 public class ViewHolder {
		 public int position;
		 public ImageView imageView;
		 public TextView		textView;
		 public ViewGroup  parent;
	 }

	 private class MainMenuAdapter 
	 extends BaseAdapter implements OnItemClickListener {

		 private final ArrayList<Preference> mPreferences;
		 //		public View mSelectedView = null;
		 public boolean mIsFirstCase = true; 

		 //		public int mSelectedIndex = 0;
		 public ViewGroup mSelectedGroup = null;


		 public MainMenuAdapter(Context context,
				 ArrayList<Preference> preferences) {
			 mPreferences = preferences;
		 }


		 public void onItemClick(AdapterView<?> parent, View view, int position,
				 long id) {
			 // add it by hgkim
			 if(view != null)
			 {
				 ViewHolder vh = (ViewHolder)view.getTag();
				 mSelectedGroup.setBackgroundDrawable(null);
				 //				view.setBackgroundResource(R.drawable.vt_list_selector_background_focus);
				 //first position
				 if(position == 0)
				 {
					 view.setBackgroundResource(R.drawable.vt_camera_setting_selected_top);
				 }
				 //last position
				 else if(position == mPreferences.size()-1)
				 {
					 view.setBackgroundResource(R.drawable.vt_camera_setting_selected_bott);
				 }
				 else
				 {
					 view.setBackgroundResource(R.drawable.vt_camera_setting_selected_mid);	
				 }


//				 ImageView icon = vh.imageView;
//				 if (icon != null) {
//					 android.view.ViewGroup.LayoutParams params;
//					 params = icon.getLayoutParams();
//
//					 // margin
//					 ((MarginLayoutParams) params).leftMargin = 11;
//
//					 icon.setLayoutParams(params);
//					 icon.invalidate();
//				 }

				 mSelectedGroup = (ViewGroup)view;
				 //				mSelectedIndex = position;
			 }


			 Preference preference = mPreferences.get(position);
			 SubMenuAdapter adapter = new SubMenuAdapter(mContext,
					 (ListPreference) preference);
			 mSubMenu.setAdapter(adapter);
			 mSubMenu.setOnItemClickListener(adapter);
			 showSubMenu();
		 }


		 public View getView(int position, View convertView, ViewGroup parent) 
		 {
		      if(DBG)
			 Log.d(TAG, "getView("+position+") ,convertView="+convertView+" called!");

			 ViewHolder holder = null;

			 if (convertView == null) 
			 {
				 convertView = inflateIfNeed(convertView,
						 R.layout.vt_on_screen_menu_list_item, parent, false);

				 holder = makeViewHolder(convertView);		
			 } 
			 else 
			 {
				 holder = (ViewHolder) convertView.getTag();
			 }

			 setViewHolder(position, holder);
			 convertView.setTag(holder);

			 return convertView;
		 }


		 protected void setViewHolder(int position, ViewHolder vh){
			 vh.position = position;

			 Preference preference = mPreferences.get(position);

			 //every time, the focus is first postion.
			 if (position == 0 && mIsFirstCase == true) 
			 {	
				 if(mSelectedGroup != null)
				 {
					 mSelectedGroup.setBackgroundDrawable(null);
				 }

				 ViewGroup vg = (ViewGroup)vh.imageView.getParent(); 
				 //				vg.setBackgroundResource(R.drawable.vt_list_selector_background_focus);
				 //first position
				 if(position == 0)
				 {
					 vg.setBackgroundResource(R.drawable.vt_camera_setting_selected_top);
				 }
				 //last position
				 else if(position == mPreferences.size()-1)
				 {
					 vg.setBackgroundResource(R.drawable.vt_camera_setting_selected_bott);
				 }
				 else
				 {
					 vg.setBackgroundResource(R.drawable.vt_camera_setting_selected_mid);	
				 }


				 //icon
//				 if (vh.imageView != null) {
//					 android.view.ViewGroup.LayoutParams params;
//					 params = vh.imageView.getLayoutParams();
//
//					 // margin
//					 ((MarginLayoutParams) params).leftMargin = 11;
//
//					 vh.imageView.setLayoutParams(params);
//					 vh.imageView.invalidate();
//				 }

				 //selected Relative layout
				 mSelectedGroup = vg;
			        if(DBG)
				 Log.d(TAG, "mSelectedGroup = "+ mSelectedGroup);

				 mIsFirstCase = false;				
			 }

			 //			String override = mOverride.get(preference.getKey());

			 // for image icon
			 CharSequence mainTitle = preference.getTitle();
			 ImageView img = vh.imageView;

			 // brightness
			 if (mainTitle.equals(mContext.getResources().getString(
					 R.string.vt_camera_brightness_title))) {
				 img.setBackgroundResource(R.drawable.vt_camerasetting_brightness_selector);
			 }
			 // white Balance
			 else if (mainTitle.equals(mContext.getResources().getString(
					 R.string.vt_camera_whitebalance_title))) {
				 img.setBackgroundResource(R.drawable.vt_camerasetting_white_selector);
			 }
			 // Colour Effect
			 else if (mainTitle.equals(mContext.getResources().getString(
					 R.string.vt_camera_coloureffect_title))) {
				 img.setBackgroundResource(R.drawable.vt_camerasetting_color_selector);
			 }
			 // Zoom
			 else if (mainTitle.equals(mContext.getResources().getString(
					 R.string.vt_camera_zoom_title))) {
				 img.setBackgroundResource(R.drawable.vt_camerasetting_zoom_selector);
			 }
			 // Night mode
			 else if (mainTitle.equals(mContext.getResources().getString(
					 R.string.vt_camera_night_title))) {
				 img.setBackgroundResource(R.drawable.vt_camerasetting_night_selector);
			 }

			 TextView title = vh.textView;
			 title.setText(mainTitle);
		 }

		 protected ViewHolder makeViewHolder(View convertView){
			 ViewHolder vh = new ViewHolder();
			 vh.imageView = (ImageView)convertView.findViewById(R.id.icon_img);
			 vh.textView = (TextView)convertView.findViewById(R.id.title);
			 vh.parent = (ViewGroup)vh.imageView.getParent();

			 return vh;
		 }

		 @Override
		 public boolean areAllItemsEnabled() {
			 return true;
		 }

		 @Override
		 public boolean isEnabled(int position) {
			 Preference preference = mPreferences.get(position);
			 return !(preference instanceof PreferenceGroup);
		 }

		 public int getCount() {
			 return mPreferences.size();
		 }

		 public Object getItem(int position) {
			 return null;
		 }

		 public long getItemId(int position) {
			 return position;
		 }

		 @Override
		 public int getItemViewType(int position) {
			 Preference pref = mPreferences.get(position);
			 if (pref instanceof PreferenceGroup)
				 return 0;
			 if (pref instanceof ListPreference)
				 return 1;
			 throw new IllegalStateException();
		 }

		 @Override
		 public int getViewTypeCount() {
			 // we have two types, see getItemViewType()
			 return 2;
		 }

		 @Override
		 public boolean hasStableIds() {
			 return true;
		 }

		 @Override
		 public boolean isEmpty() {
			 return mPreferences.isEmpty();
		 }
	 }

	 private class SubMenuAdapter extends BaseAdapter implements
	 OnItemClickListener {
		 private final ListPreference mPreference;
		 // private final IconListPreference mIconPreference;
		 //		private ImageView checkImage = null;

		 public SubMenuAdapter(Context context, ListPreference preference) {
			 mPreference = preference;
			 // mIconPreference = (preference instanceof IconListPreference)
			 // ? (IconListPreference) preference
			 // : null;
		 }

		 public View getView(int position, View convertView, ViewGroup parent) {

			 int index = position;

			 //UI 항목 리스트 
			 CharSequence entry[] = mPreference.getEntries();
			 //현재 선택된 view 의 UI 항목
			 CharSequence currentString = entry[index];

			 convertView = inflateIfNeed(convertView,
					 R.layout.vt_on_screen_submenu_item, parent, false);

			 // entry setting
			 ((TextView) convertView.findViewById(R.id.title))
			 .setText(currentString);

			 //xml에 저장된 UI항목에 대한 값 
			 String selectedValue = mPreference.getValue();
			 //UI항목 리스트에 대한 값 리스트 중 index에 해당되는 값 
			 String currentValue = (String) mPreference.getEntryValues()[index];

			 //index에 해당되는 값 과 
			 boolean checked = selectedValue.equalsIgnoreCase(currentValue);


			 ImageView checkImage = (ImageView) convertView.findViewById(R.id.check_img);


			 if ( checked) {
				 checkImage.setVisibility(View.VISIBLE);
			 } else {
				 checkImage.setVisibility(View.GONE);
			 }

			 return convertView;
		 }

		 @Override
		 public boolean areAllItemsEnabled() {
			 return false;
		 }

		 @Override
		 public boolean isEnabled(int position) {
			 return getItemViewType(position) != 0;
		 }

		 public int getCount() {
			 // add one for the header
			 return mPreference.getEntries().length;
		 }

		 public Object getItem(int position) {
			 return null;
		 }

		 public long getItemId(int position) {
			 return position;
		 }

		 @Override
		 public int getItemViewType(int position) {
			 // return position == 0 ? 0 : 1;
			 return 1;
		 }

		 @Override
		 public int getViewTypeCount() {
			 return 2;
		 }

		 @Override
		 public boolean hasStableIds() {
			 return true;
		 }

		 public void onItemClick(AdapterView<?> parent, View view, int position,
				 long id) {
			 CharSequence values[] = mPreference.getEntryValues();
			 //			 int idx = mPreference.findIndexOfValue(mPreference.getValue());
			 int idx = 0;
			 String value = mPreference.getValue();

			 for(idx = 0; idx < values.length; idx++)
			 {
				 if(((String) values[idx]).equalsIgnoreCase(value))
				 {
					 break;
				 }
				 idx++;				
			 }

			 if (idx != position) {
				 mPreference.setValueIndex(position);
				 notifyDataSetChanged();
				 // mMainAdapter.notifyDataSetChanged();
				 return;
			 }

			 // Close the sub menu when user presses the original option.
			 // closeSubMenu();
		 }
	 }

	 // 수정함 private --> public
	 public class Container extends FrameLayout {
		 public Container(Context context) {
			 super(context);
		 }

		 @Override
		 public boolean onTouchEvent(MotionEvent event) {
			 if (super.onTouchEvent(event))
				 return true;
			 if (event.getAction() == MotionEvent.ACTION_DOWN) {
				 setVisible(false);
				 return true;
			 }
			 return false;
		 }

		 /*
		  * Need to override this to intercept the key events. Otherwise, we
		  * would attach a key listener to the container but its superclass
		  * ViewGroup gives it to the focused View instead of calling the key
		  * listener, and so we wouldn't get the events.
		  */
		 @Override
		 public boolean dispatchKeyEvent(KeyEvent event) {
			 return onContainerKey(event) ? true : super.dispatchKeyEvent(event);
		 }
	 }
}
