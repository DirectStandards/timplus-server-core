package org.jivesoftware.openfire.vcard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.jivesoftware.Fixtures;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.util.JiveGlobals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class VCardManagerTest
{
    @BeforeClass
    public static void setUpClass() throws Exception 
    {
        Fixtures.reconfigureOpenfireHome();
    }
	
    @Before
    public void setUp() throws Exception 
    {
    	JiveGlobals.setProperty("provider.vcard.className", Fixtures.StubVCardProvider.class.getName());
    	
    }
	@Test
	public void testLoadvCardAsVCard() throws Exception 
	{
		final InputStream inStream = getClass().getResourceAsStream("/vcard/exampleVCard.xml");
		
		@SuppressWarnings("deprecation")
		final String xml = IOUtils.toString(inStream).trim();
		
		VCardManager mgr = new VCardManager();
		mgr.initialize(Fixtures.mockXMPPServer());
		
		((Fixtures.StubVCardProvider)VCardManager.getProvider()).setVCardXML(xml);
		
		final VCard vcard = mgr.getVCardAsVCard("testUser");
		assertNotNull(vcard);
		
		assertEquals(vcard.getFirstName(), "Peter");
		assertEquals(vcard.getLastName(), "Saint-Andre");
		assertEquals(vcard.getNickName(), "stpeter");
		assertEquals(vcard.getOrganization(), "XMPP Standards Foundation");
		assertEquals(vcard.getField("TITLE"), "Executive Director");
		assertEquals(vcard.getField("ROLE"), "Patron Saint");
		assertEquals(vcard.getPhoneHome("VOICE"), "303-555-1212");
		assertEquals(vcard.getPhoneWork("VOICE"), "303-308-3282");
		assertEquals(vcard.getJabberId(), "stpeter@jabber.org");
	}
	
	@Test
	public void testSaveCardAsXML() throws Exception 
	{
		final InputStream inStream = getClass().getResourceAsStream("/vcard/exampleVCard.xml");
		
		@SuppressWarnings("deprecation")
		final String xml = IOUtils.toString(inStream).trim();
		
		VCardManager mgr = new VCardManager();
		mgr.initialize(Fixtures.mockXMPPServer());
		
		mgr.setVCardAsXML("pete", xml);
	}
}
