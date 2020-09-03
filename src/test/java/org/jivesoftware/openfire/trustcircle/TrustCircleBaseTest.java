package org.jivesoftware.openfire.trustcircle;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.SpringDataBaseTest;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.junit.Before;

public abstract class TrustCircleBaseTest extends SpringDataBaseTest
{
	protected X509Certificate testAnchor;
	
	protected TrustCircle testCircle;
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		
		final File bundleLocation = new File("./src/test/resources/bundles/providerTestBundle.p7b");
		
		final TrustBundle bundle = new TrustBundle();
		bundle.setBundleName("JUnit Bundle");
		bundle.setBundleURL(filePrefix + bundleLocation.getAbsolutePath());
		bundle.setRefreshInterval(24);

		trustBundleProv.addTrustBundle(bundle);
		
		testAnchor = CertUtils.toX509Certificate(IOUtils.resourceToByteArray("/certs/direct.securehealthemail.com.cer"));
		
		trustAnchorProv.addTrustAnchor(testAnchor);
		
		testCircle = trustCircleProv.addTrustCircle("TestCircle", Collections.singleton(Thumbprint.toThumbprint(testAnchor).toString()));
		trustCircleProv.addTrustBundleToCircle("TestCircle", "JUnit Bundle");
	}
}
