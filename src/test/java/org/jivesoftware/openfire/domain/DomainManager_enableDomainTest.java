package org.jivesoftware.openfire.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jivesoftware.SpringDataBaseTest;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.junit.Before;
import org.junit.Test;

public class DomainManager_enableDomainTest extends SpringDataBaseTest
{
	protected DomainManager domainManager;
	
	protected MultiUserChatManager chatManager;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		XMPPServer mockServer = mock(XMPPServer.class);
		
		chatManager = mock(MultiUserChatManager.class);
		when(mockServer.getMultiUserChatManager()).thenReturn(chatManager);
		
		domainManager = DomainManager.getInstance();
		
		domainManager.setXMPPServer(mockServer);
		
		prov.createDomain("domain.com", true);
		prov.createDomain("healthcare.com", false);
	}
	
	@Test
	public void testEnableDomain_domainExists_assertEnabled() throws Exception
	{
		domainManager.enableDomain("healthcare.com", true);
		
		final Domain dom = domainManager.getDomain("healthcare.com");
		
		assertTrue(dom.isEnabled());	
	}
	
	@Test
	public void testEnabledDomain_domainExists_assertEnabled() throws Exception
	{
		domainManager.enableDomain("domain.com", false);
		
		final Domain dom = domainManager.getDomain("domain.com");
		
		assertFalse(dom.isEnabled());
	}
	
	@Test(expected=DomainNotFoundException.class)
	public void testEnabledDomain_domainNotExists_assertException() throws Exception
	{
		domainManager.enableDomain("domain2.com", false);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testEnableDomain_emptyDomainName_assertException() throws Exception
	{
		domainManager.enableDomain("", true);
	}
}
