/*
   Android Mouse Input Mapping Tests

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
   If a copy of the MPL was not distributed with this file, You can obtain one at
   http://mozilla.org/MPL/2.0/.
*/

package com.freerdp.freerdpcore.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MouseTest
{
	@Test
	public void positivePhysicalWheelAxisScrollsUp()
	{
		assertFalse(Mouse.isPhysicalScrollDown(1.0f));
	}

	@Test
	public void negativePhysicalWheelAxisScrollsDown()
	{
		assertTrue(Mouse.isPhysicalScrollDown(-1.0f));
	}
}
