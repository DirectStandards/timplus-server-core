package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultDomainProvider_getDomainsTest extends SpringDataBaseTest
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
	public void testGetAllDomains_assertDomains() throws Exception
	{
		Collection<Domain> domains = prov.getDomains(false);
		assertEquals(2, domains.size());
		
	}
	
	@Test
	public void testGetEnabledDomains_assertDomains() throws Exception
	{
		Collection<Domain> domains = prov.getDomains(true);
		assertEquals(1, domains.size());
		
	}
}
