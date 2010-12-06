/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.android.phone;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.accessibility.AccessibilityEvent;

 /**
 * Abstract Class to control activity lifecycle of the voice call and video call
 */
public abstract class AbstractInCallScreen  extends Activity{
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		onSubCreate(icicle);
	}

	@Override
	protected void onStart() {
		super.onStart();
		onSubStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		onSubResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		onSubPause();
	}

	@Override
	protected void onStop() {
		super.onStop(); 
		onSubStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		onSubDestroy();
	}

	@Override
	public void onBackPressed() {
		if(onSubBackPressed())
			super.onBackPressed();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if(dispatchSubKeyEvent(event))
			return true;
		else
			return super.dispatchKeyEvent(event); 
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(onSubKeyUp(keyCode, event))
			return true;
		else
			return super.onKeyUp(keyCode, event);		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(onSubKeyDown(keyCode, event))
			return true;
		else
			return super.onKeyDown(keyCode, event); 
	}

	@Override
	public void onPanelClosed(int featureId, Menu menu) {
		super.onPanelClosed(featureId, menu) ;
		onSubPanelClosed(featureId, menu);
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		super.dispatchPopulateAccessibilityEvent( event);
		dispatchSubPopulateAccessibilityEvent(event);
		return true;
	}
	
	abstract protected void onSubCreate(Bundle icicle);
	abstract protected void onSubStart();
	abstract protected void onSubResume();
	abstract protected void onSubPause();
	abstract protected void onSubStop();
	abstract protected void onSubDestroy();
	abstract public boolean onSubBackPressed();
	abstract public boolean dispatchSubKeyEvent(KeyEvent event);
	abstract public boolean onSubKeyUp(int keyCode, KeyEvent event);
	abstract public boolean onSubKeyDown(int keyCode, KeyEvent event);
	abstract public void onSubPanelClosed(int featureId, Menu menu);
	abstract public void dispatchSubPopulateAccessibilityEvent(AccessibilityEvent event);
}
