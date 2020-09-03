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

public class DomainManager_isRegisteredComponentTest extends SpringDataBaseTest
{
	protected DomainManager domainManager;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		XMPPServer mockServer = mock(XMPPServer.class);
		
		final MultiUserChatManager chatManager = mock(MultiUserChatManager.class);
		when(mockServer.getMultiUserChatManager()).thenReturn(chatManager);
		
		domainManager = DomainManager.getInstance();
		
		domainManager.setXMPPServer(mockServer);
		
		prov.createDomain("domain.com", true);
		prov.createDomain("healthcare.com", false);
	}
	
	@Test
	public void testIsResisteredComponent_fileTransferDomainExists_assertRegistered()
	{
		assertTrue(domainManager.isRegisteredComponentDomain("ftproxystream.domain.com"));
	}
	
	@Test
	public void testIsResisteredComponent_groupChatDomainExists_assertRegistered()
	{
		assertTrue(domainManager.isRegisteredComponentDomain("groupchat.domain.com"));
	}
	
	@Test
	public void testIsResisteredComponent_groupChatDomainNotExists_assertNotRegistered()
	{
		assertFalse(domainManager.isRegisteredComponentDomain("groupchat.domain2.com"));
	}
	
	@Test
	public void testIsResisteredComponent_invalidSubDomain_assertNotRegistered()
	{
		assertFalse(domainManager.isRegisteredComponentDomain("groupchatty.domain.com"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testisRegisteredComponent_emptyDomainName_assertException() throws Exception
	{
		domainManager.isRegisteredComponentDomain("");
	}
}
