package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultDomainProvider_isRegisteredDomainTest extends SpringDataBaseTest
{
	protected final DefaultDomainProvider prov = new DefaultDomainProvider();
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		prov.createDomain("domain.com", true);
	}
	
	@Test()
	public void testIsRegisteredDomain_domainRegistered_assertTrue() throws Exception
	{	
		assertTrue(prov.isRegisteredDomain("domain.com"));
	}
	
	@Test()
	public void testIsRegisteredDomain_domainNotRegistered_assertFalse() throws Exception
	{	
		assertFalse(prov.isRegisteredDomain("domain33.com"));
	}
}
