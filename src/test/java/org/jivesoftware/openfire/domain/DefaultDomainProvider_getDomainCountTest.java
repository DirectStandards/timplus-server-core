package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;


import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultDomainProvider_getDomainCountTest extends SpringDataBaseTest
{
	protected final DefaultDomainProvider prov = new DefaultDomainProvider();
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		prov.createDomain("domain1.com", true);
		prov.createDomain("domain2.com", false);
	}
	
	@Test
	public void testGetAllDomains_assertCount() throws Exception
	{
		assertEquals(2, prov.getDomainCount());
		
	}
}
