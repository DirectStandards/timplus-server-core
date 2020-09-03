package org.jivesoftware.openfire.certificate;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.junit.Before;
import org.junit.Test;

public class DefaultCertificateProvider_deleteCertificateTest extends SpringDataBaseTest
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
	public void testDeleteCertificate_certExists_assertDeleted() throws Exception
	{
		certProv.deleteCertificate(Thumbprint.toThumbprint(testCert).toString());
		
		assertEquals(0, certProv.getCertificates().size());
	}

}
