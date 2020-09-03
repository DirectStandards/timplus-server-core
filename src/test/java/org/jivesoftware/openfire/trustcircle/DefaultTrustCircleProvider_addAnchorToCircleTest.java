package org.jivesoftware.openfire.trustcircle;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.openfire.trustcircle.TrustCircle;
import org.jivesoftware.openfire.trustcircle.TrustCircleException;
import org.jivesoftware.openfire.trustcircle.TrustCircleNotFoundException;
import org.junit.Before;
import org.junit.Test;

public class DefaultTrustCircleProvider_addAnchorToCircleTest extends TrustCircleBaseTest
{
	protected X509Certificate addedAnchor;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		addedAnchor = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/messaging.cerner.com.der"));
		
		trustAnchorProv.addTrustAnchor(addedAnchor);
	}
	
	@Test
	public void testAddAnchorToCircle_existingCircleAndAncor_assertAdded() throws Exception
	{
		trustCircleProv.addAnchorToCircle("TestCircle", Thumbprint.toThumbprint(addedAnchor).toString());
		
		final TrustCircle circle = trustCircleProv.getTrustCircle("TestCircle", false, true);
		
		assertEquals(2, circle.getAnchors().size());
	}
	
	@Test(expected=TrustCircleNotFoundException.class)
	public void testAddAnchorToCircle_circleNotExists_assertException() throws Exception
	{
		trustCircleProv.addAnchorToCircle("TestCircle2", Thumbprint.toThumbprint(addedAnchor).toString());
	}
	
	@Test(expected=TrustCircleException.class)
	public void testAddAnchorToCircle_anchorNotExists_assertException() throws Exception
	{
		trustCircleProv.addAnchorToCircle("TestCircle", "12345");
	}
	
	@Test(expected=TrustCircleException.class)
	public void testAddAnchorToCircle_anchorAlreadyAdded_assertException() throws Exception
	{
		trustCircleProv.addAnchorToCircle("TestCircle", Thumbprint.toThumbprint(addedAnchor).toString());
		
		trustCircleProv.addAnchorToCircle("TestCircle", Thumbprint.toThumbprint(addedAnchor).toString());
	}
}
