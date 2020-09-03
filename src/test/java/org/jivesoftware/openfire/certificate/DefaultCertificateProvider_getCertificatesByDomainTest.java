package org.jivesoftware.openfire.certificate;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultCertificateProvider_getCertificatesByDomainTest extends SpringDataBaseTest
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
	public void testGetCertificatesByDomain_domainAvailable_assertRetrieved() throws Exception
	{
		final Collection<Certificate> certs = certProv.getCertificatesByDomain("testDOMAIN");
		
		assertEquals(1, certs.size());
		
		final Certificate cert = certs.iterator().next();
		
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
	
	@Test
	public void testGetCertificatesByDomain_domainAvailable_subSearch_assertRetrieved() throws Exception
	{
		final Collection<Certificate> certs = certProv.getCertificatesByDomain("estDO");
		
		assertEquals(1, certs.size());
	}
	
	@Test(expected=CertificateNotFoundException.class)
	public void testGetCertificatesByDomain_domainNotAvailable_assertNotRetrieved() throws Exception
	{
		certProv.getCertificatesByDomain("bogus");
	}
}
