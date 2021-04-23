package org.jivesoftware.openfire.certificate;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Test;

public class DefaultCertificateProvider_addCertificatesTest extends SpringDataBaseTest
{
	@Test
	public void testAddCertificate_newCertifcate_assertAdded() throws Exception
	{
		final X509Certificate testCert = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/direct.securehealthemail.com.cer"));
		
		Certificate addCert = new Certificate();
		addCert.setCertData(testCert.getEncoded());
		addCert.setDomain("testdomain");
		addCert.setStatus(CertificateStatus.GOOD);		
		
		certProv.addCertificate(addCert);
		
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
	
	@Test(expected=CertificateException.class)
	public void testAddCertificate_existingCertifcate_assertException() throws Exception
	{
		final X509Certificate testCert = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/direct.securehealthemail.com.cer"));
		
		Certificate addCert = new Certificate();
		addCert.setCertData(testCert.getEncoded());
		addCert.setDomain("testdomain");
		addCert.setStatus(CertificateStatus.GOOD);		
		
		certProv.addCertificate(addCert);
		
		certProv.addCertificate(addCert);
	}
}
