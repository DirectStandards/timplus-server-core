package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Test;

public class DefaultTrustCircleProvider_addTrustCircleTest extends SpringDataBaseTest
{

	@Test
	public void testAddTrustCircle_newCircle_assertAdded() throws Exception
	{
		trustCircleProv.addTrustCircle("TestCircle", Collections.emptyList());
		
		assertEquals(1, trustCircleProv.getTrustCircles(false, false).size());
	}
	
	@Test(expected=TrustCircleAlreadyExistsException.class)
	public void testAddTrustCircle_existingCircle_assertException() throws Exception
	{
		trustCircleProv.addTrustCircle("TestCircle", Collections.emptyList());

		trustCircleProv.addTrustCircle("TestCircle", Collections.emptyList());

	}
}
