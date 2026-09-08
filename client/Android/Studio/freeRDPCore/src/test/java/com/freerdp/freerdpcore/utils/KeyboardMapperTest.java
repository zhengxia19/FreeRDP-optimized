/*
   Android Keyboard Input Mapping Tests

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
   If a copy of the MPL was not distributed with this file, You can obtain one at
   http://mozilla.org/MPL/2.0/.
*/

package com.freerdp.freerdpcore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.view.KeyEvent;

import com.freerdp.freerdpcore.R;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyboardMapperTest
{
	private static final Map<Integer, Integer> resourceKeyCodes = new HashMap<>();
	private final List<String> events = new ArrayList<>();
	private final KeyboardMapper mapper = new KeyboardMapper();
	private final KeyboardMapper.KeyProcessingListener listener =
	    new KeyboardMapper.KeyProcessingListener() {
		    @Override public void processVirtualKey(int key, boolean down)
		    {
			    events.add(key + (down ? ":down" : ":up"));
		    }
		    @Override public void processUnicodeKey(int key)
		    {
			    events.add("unicode:" + key);
		    }
		    @Override public void switchKeyboard(int type) {}
		    @Override public void modifiersChanged() {}
	    };

	@Before public void setUp()
	{
		Context context = mock(Context.class);
		Resources resources = mock(Resources.class);
		when(context.getResources()).thenReturn(resources);
		when(resources.getInteger(anyInt())).thenAnswer(invocation -> {
			int id = invocation.getArgument(0);
			return resourceKeyCodes.computeIfAbsent(id, ignored -> resourceKeyCodes.size() + 1);
		});
		mapper.init(context);
		mapper.reset(listener);
	}

	private boolean key(int action, int code, boolean shift, boolean ctrl)
	{
		KeyEvent event = mock(KeyEvent.class);
		when(event.getAction()).thenReturn(action);
		when(event.getKeyCode()).thenReturn(code);
		when(event.isShiftPressed()).thenReturn(shift);
		when(event.isCtrlPressed()).thenReturn(ctrl);
		return mapper.processAndroidKeyEvent(event);
	}

	private void down(int code)
	{
		assertTrue(key(KeyEvent.ACTION_DOWN, code, true, false));
	}

	private void up(int code)
	{
		assertTrue(key(KeyEvent.ACTION_UP, code, false, false));
	}

	private void toggleShift()
	{
		mapper.processCustomKeyEvent(resourceKeyCodes.get(R.integer.keycode_toggle_shift));
	}

	@Test public void leftShiftTapSendsDownAndUp()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		assertEquals(Arrays.asList("160:down", "160:up"), events);
	}

	@Test public void rightShiftTapPreservesSide()
	{
		down(KeyEvent.KEYCODE_SHIFT_RIGHT);
		up(KeyEvent.KEYCODE_SHIFT_RIGHT);
		assertEquals(Arrays.asList("161:down", "161:up"), events);
	}

	@Test public void heldShiftIsNotReleasedBetweenLetters()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		down(KeyEvent.KEYCODE_A);
		down(KeyEvent.KEYCODE_B);
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		assertEquals(Arrays.asList("160:down", "65:down", "65:up", "66:down", "66:up", "160:up"), events);
	}

	@Test public void rightShiftCombinationDoesNotSynthesizeLeftShift()
	{
		down(KeyEvent.KEYCODE_SHIFT_RIGHT);
		down(KeyEvent.KEYCODE_DPAD_LEFT);
		up(KeyEvent.KEYCODE_SHIFT_RIGHT);
		assertEquals(Arrays.asList("161:down", "293:down", "293:up", "161:up"), events);
	}

	@Test public void bothShiftKeysHaveIndependentLifetimes()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		down(KeyEvent.KEYCODE_SHIFT_RIGHT);
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		down(KeyEvent.KEYCODE_A);
		up(KeyEvent.KEYCODE_SHIFT_RIGHT);
		assertEquals(Arrays.asList("160:down", "161:down", "160:up", "65:down", "65:up", "161:up"), events);
	}

	@Test public void repeatedShiftDownDoesNotGenerateExtraPresses()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		assertEquals(Arrays.asList("160:down", "160:up"), events);
	}

	@Test public void shiftMetadataStillWorksWithoutPhysicalEvents()
	{
		down(KeyEvent.KEYCODE_A);
		assertEquals(Arrays.asList("160:down", "65:down", "65:up", "160:up"), events);
	}

	@Test public void ctrlShiftCombinationKeepsPhysicalShiftHeld()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, true, true));
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		assertEquals(Arrays.asList("160:down", "162:down", "65:down", "65:up", "162:up", "160:up"), events);
	}

	@Test public void consumingSoftShiftDoesNotReleasePhysicalShift()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		toggleShift();
		down(KeyEvent.KEYCODE_A);
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		assertEquals(Arrays.asList("160:down", "65:down", "65:up", "160:up"), events);
	}

	@Test public void releasingPhysicalShiftPreservesSoftShift()
	{
		toggleShift();
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		down(KeyEvent.KEYCODE_A);
		assertEquals(Arrays.asList("160:down", "65:down", "65:up", "160:up"), events);
	}

	@Test public void clearingModifiersReleasesBothPhysicalShiftKeysOnce()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		toggleShift();
		down(KeyEvent.KEYCODE_SHIFT_RIGHT);
		mapper.clearlAllModifiers();
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		up(KeyEvent.KEYCODE_SHIFT_RIGHT);
		assertEquals(Arrays.asList("160:down", "161:down", "161:up", "160:up"), events);
	}

	@Test public void resetDoesNotCarryPhysicalShiftIntoNewSession()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		down(KeyEvent.KEYCODE_SHIFT_RIGHT);
		mapper.reset(listener);
		events.clear();
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		up(KeyEvent.KEYCODE_SHIFT_RIGHT);
		down(KeyEvent.KEYCODE_A);
		assertEquals(Arrays.asList("160:down", "65:down", "65:up", "160:up"), events);
	}

	@Test public void clearingPhysicalShiftReleasesItWithoutSoftShift()
	{
		down(KeyEvent.KEYCODE_SHIFT_LEFT);
		mapper.clearlAllModifiers();
		up(KeyEvent.KEYCODE_SHIFT_LEFT);
		assertEquals(Arrays.asList("160:down", "160:up"), events);
	}

	@Test public void resetClearsSoftShiftDoubleTapHistory()
	{
		toggleShift();
		mapper.reset(listener);
		events.clear();
		toggleShift();
		assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, false, false));
		assertEquals(Arrays.asList("160:down", "65:down", "65:up", "160:up"), events);
	}

	@Test public void clearingModifiersClearsSoftShiftDoubleTapHistory()
	{
		toggleShift();
		mapper.clearlAllModifiers();
		events.clear();
		toggleShift();
		assertEquals(Arrays.asList("160:down"), events);
		assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, false, false));
		assertEquals(Arrays.asList("160:down", "65:down", "65:up", "160:up"), events);
	}
}
