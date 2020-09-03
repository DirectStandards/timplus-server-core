package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultDomainProvider_deleteDomainTest extends SpringDataBaseTest
{
	protected final DefaultDomainProvider prov = new DefaultDomainProvider();
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		prov.createDomain("domain.com", true);
	}
	
	@Test
	public void testDeleteDomain_assertDeleted() throws Exception
	{
		prov.deleteDomain("domain.com");
		
		assertEquals(0, prov.getDomainCount());
	}
}
