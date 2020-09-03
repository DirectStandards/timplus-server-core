package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

public class DefaultTrustCircleProvider_deleteDomainsFromCircleTest extends TrustCircleBaseTest
{
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
			
		prov.createDomain("TestDomain", true);
		
		trustCircleProv.addCirclesToDomain("TestDomain", Collections.singleton("TestCircle"));
	}
	
	@Test
	public void testDeleteDomainsFromCircle_domainAndCircleAdded_assertDeleted() throws Exception
	{
		trustCircleProv.deleteDomainsFromCircle("TestCircle", Collections.singleton("TestDomain"));
		
		assertEquals(0, trustCircleProv.getCirclesByDomain("TestDomain", false, false).size());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void testDeleteDomainsFromCircle_circleNotExists_assertException() throws Exception
	{
		trustCircleProv.deleteDomainsFromCircle("TestCircle2", Collections.singleton("TestDomain"));
	}
}
