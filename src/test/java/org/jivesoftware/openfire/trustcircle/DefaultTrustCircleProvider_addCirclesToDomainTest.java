package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

public class DefaultTrustCircleProvider_addCirclesToDomainTest extends TrustCircleBaseTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
			
		prov.createDomain("TestDomain", true);
	}
	
	@Test
	public void addCirclesToDomain_circleAndDomainExists_assertAdded() throws Exception
	{
		trustCircleProv.addCirclesToDomain("TestDomain", Collections.singleton("TestCircle"));
		
		final Collection<TrustCircle> circles = trustCircleProv.getCirclesByDomain("TestDomain", true, true);
		
		assertEquals(1, circles.size());
		
		final TrustCircle circle = circles.iterator().next();
		
		assertEquals("TestCircle", circle.getName());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void testAddCirclesToDomain_circleNotExists_assertAdded() throws Exception
	{
		trustCircleProv.addCirclesToDomain("TestDomain", Collections.singleton("TestCircle2"));
	}
}
