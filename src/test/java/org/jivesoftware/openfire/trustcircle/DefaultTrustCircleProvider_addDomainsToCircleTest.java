package org.jivesoftware.openfire.trustcircle;


import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

public class DefaultTrustCircleProvider_addDomainsToCircleTest extends TrustCircleBaseTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
			
		prov.createDomain("TestDomain", true);
	}
	
	@Test
	public void testAddDomainsToCircle_circleAndDomainExist_assertAdded() throws Exception
	{
		trustCircleProv.addDomainsToCircle("TestCircle", Collections.singleton("TestDomain"));
		
		final Collection<TrustCircle> circles = trustCircleProv.getCirclesByDomain("TestDomain", true, true);
		
		assertEquals(1, circles.size());
		
		final TrustCircle circle = circles.iterator().next();
		
		assertEquals("TestCircle", circle.getName());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void testAddDomainsToCircle_circleNotExists_assertAdded() throws Exception
	{
		trustCircleProv.addDomainsToCircle("TestCircle2", Collections.singleton("TestDomain"));
	}
}
