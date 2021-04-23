package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

public class DefaultTrustCircleProvider_deleteCirclesFromDomainTest extends TrustCircleBaseTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
			
		prov.createDomain("TestDomain", true);
		
		trustCircleProv.addCirclesToDomain("TestDomain", Collections.singleton("TestCircle"));
	}
	
	@Test
	public void testDeleteCirclesFromDomain_domainAndCircleAdded_assertDeleted() throws Exception
	{
		trustCircleProv.deleteCirclesFromDomain("TestDomain", Collections.singleton("TestCircle"));
		
		assertEquals(0, trustCircleProv.getCirclesByDomain("TestDomain", false, false).size());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void testDeleteCirclesFromDomain_circleNotExists_assertException() throws Exception
	{
		trustCircleProv.deleteCirclesFromDomain("TestDomain", Collections.singleton("TestCircle2"));
		
	}
}
