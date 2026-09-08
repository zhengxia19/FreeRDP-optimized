/*
   External Display Manager

   Copyright 2026 Ibrahim Sevinc <ibrahim.sevinc.mail@gmail.com>

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
   If a copy of the MPL was not distributed with this file, You can obtain one at
   http://mozilla.org/MPL/2.0/.
*/

package com.freerdp.freerdpcore.presentation;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;

import androidx.appcompat.app.AlertDialog;

import com.freerdp.freerdpcore.R;

import java.util.ArrayList;

class ExternalDisplayManager
{
	private final Activity activity;
	private final DisplayManager displayManager;

	ExternalDisplayManager(Activity activity)
	{
		this.activity = activity;
		this.displayManager = (DisplayManager)activity.getSystemService(Activity.DISPLAY_SERVICE);
	}

	void launchSessionWithDisplayPicker(String refStr)
	{
		Display current = activity.getWindowManager().getDefaultDisplay();
		int currentDisplayId = current.getDisplayId();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
		{
			launchSessionOnDisplay(refStr, currentDisplayId);
			return;
		}

		Display[] secondary = getSecondaryDisplays(currentDisplayId);
		if (secondary.length == 0)
		{
			launchSessionOnDisplay(refStr, currentDisplayId);
			return;
		}

		String[] labels = new String[1 + secondary.length];
		labels[0] = currentDisplayId == Display.DEFAULT_DISPLAY
		                ? activity.getString(R.string.display_main_screen)
		                : current.getName();
		for (int i = 0; i < secondary.length; i++)
			labels[i + 1] = secondary[i].getName();

		new AlertDialog.Builder(activity)
		    .setTitle(R.string.select_display_title)
		    .setItems(labels,
		              (d, which) -> {
			              int id = (which == 0) ? currentDisplayId
			                                    : secondary[which - 1].getDisplayId();
			              launchSessionOnDisplay(refStr, id);
		              })
		    .show();
	}

	private void launchSessionOnDisplay(String refStr, int displayId)
	{
		Intent intent = new Intent(activity, SessionActivity.class);
		intent.putExtra(SessionActivity.PARAM_CONNECTION_REFERENCE, refStr);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			ActivityOptions opts = ActivityOptions.makeBasic();
			opts.setLaunchDisplayId(displayId);
			activity.startActivity(intent, opts.toBundle());
		}
		else
		{
			activity.startActivity(intent);
		}
	}

	private Display[] getSecondaryDisplays(int currentDisplayId)
	{
		ArrayList<Display> available = new ArrayList<>();
		for (Display display :
		     displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION))
		{
			if (display.getDisplayId() != currentDisplayId && display.isValid() &&
			    display.getState() != Display.STATE_OFF)
				available.add(display);
		}
		return available.toArray(new Display[0]);
	}
}
