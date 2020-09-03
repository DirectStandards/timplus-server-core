package org.jivesoftware.openfire.certificate;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultCertificateProvider_getCertificateByThumbprintTest extends SpringDataBaseTest
{
	protected X509Certificate testCert;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		testCert = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/direct.securehealthemail.com.cer"));
		
		Certificate cert = new Certificate();
		cert.setCertData(testCert.getEncoded());
		cert.setDomain("testdomain");
		cert.setStatus(CertificateStatus.GOOD);
		
		certProv.addCertificate(cert);
	}
	
	@Test
	public void testGetCertificateByThumbprint_tpExist_assertRetrieved() throws Exception
	{
		final Certificate cert = certProv.getCertificateByThumbprint(Thumbprint.toThumbprint(testCert).toString());
		
		assertEquals(testCert, cert.asX509Certificate());
		assertEquals(testCert.getSubjectDN().toString(), cert.getDistinguishedName());
		assertEquals(testCert.getSerialNumber().toString(16),  cert.getSerial());
		assertEquals(Thumbprint.toThumbprint(testCert).toString(), cert.getThumbprint());
		assertEquals(Thumbprint.toThumbprint(testCert).toString().toUpperCase(), cert.getThumbprintAllCaps());
		assertEquals(testCert.getNotAfter().toInstant(), cert.getValidEndDate());
		assertEquals(testCert.getNotBefore().toInstant(), cert.getValidStartDate());
		assertEquals("testdomain", cert.getDomain());
		assertEquals("testdomain".toUpperCase(), cert.getDomainAllCaps());
		assertEquals(CertificateStatus.GOOD, cert.getStatus());
	}
	
	@Test(expected=CertificateNotFoundException.class)
	public void testGetCertificates_certNotAvailable_assertException() throws Exception
	{
		certProv.getCertificateByThumbprint("12345");
	}
}
