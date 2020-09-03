package org.jivesoftware;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.database.ConnectionProvider;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.DefaultConnectionProvider;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.certificate.Certificate;
import org.jivesoftware.openfire.certificate.DefaultCertificateProvider;
import org.jivesoftware.openfire.domain.DefaultDomainProvider;
import org.jivesoftware.openfire.domain.Domain;
import org.jivesoftware.openfire.domain.DomainManager;
import org.jivesoftware.openfire.trustanchor.DefaultTrustAnchorProvider;
import org.jivesoftware.openfire.trustanchor.TrustAnchor;
import org.jivesoftware.openfire.trustbundle.DefaultTrustBundleProvider;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.jivesoftware.openfire.trustcircle.DefaultTrustCircleProvider;
import org.jivesoftware.openfire.trustcircle.TrustCircle;
import org.jivesoftware.util.JiveGlobals;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@JdbcTest
public abstract class SpringDataBaseTest
{
	protected String filePrefix;
	
	protected DefaultDomainProvider prov;
	
	protected DefaultTrustCircleProvider trustCircleProv;
	
	protected DefaultTrustAnchorProvider trustAnchorProv;

	protected DefaultTrustBundleProvider trustBundleProv;
	
	protected DefaultCertificateProvider certProv;
	
	protected ConnectionProvider oldConnectionProvider;
	@SuppressWarnings("deprecation")
	@Before
	public void setUp() throws Exception
	{	
		
		// check for Windows... it doens't like file://<drive>... turns it into FTP
		File file = new File("./src/test/resources/bundles/signedbundle.p7b");
		if (file.getAbsolutePath().contains(":/"))
			filePrefix = "file:///";
		else
			filePrefix = "file:///";		
		
		prov = new DefaultDomainProvider();
		
		trustCircleProv = new DefaultTrustCircleProvider();
		
		trustAnchorProv = new DefaultTrustAnchorProvider();
		
		trustBundleProv = new DefaultTrustBundleProvider();
		
		oldConnectionProvider = DbConnectionManager.getConnectionProvider();
		
		certProv = new DefaultCertificateProvider();
		
		DbConnectionManager.setConnectionProvider(new DatasourceConnectionProvider());

        final URL configFile = ClassLoader.getSystemResource("conf/openfire.xml");
        if (configFile == null) 
        {
            throw new IllegalStateException("Unable to read openfire.xml file; does conf/openfire.xml exist in the test classpath, i.e. test/resources?");
        }
        final File openfireHome = new File(configFile.toURI()).getParentFile().getParentFile();
        JiveGlobals.setHomeDirectory(openfireHome.toString());
        JiveGlobals.setXMLProperty("connectionProvider.className", "org.jivesoftware.DatasourceConnectionProvider");

        XMPPServer.setInstance(Fixtures.mockXMPPServer());
        
        cleanData();
		
	}
	
	protected void cleanData() throws Exception
	{
		DomainManager.getInstance().clearCaches();
		
		for (TrustCircle circle : trustCircleProv.getTrustCircles(false, false))
		{
			final Collection<Domain> domains = prov.getDomainsByTrustCircle(circle.getName());
			
			for (Domain dom : domains)
				trustCircleProv.deleteCirclesFromDomain(dom.getDomainName(), Collections.singleton(circle.getName()));
			
			for (TrustBundle bundle : circle.getTrustBundles())
				trustCircleProv.deleteTrustBundlesFromCircle(circle.getName(), Collections.singleton(bundle.getBundleName()));
			
			for (TrustAnchor anchor : circle.getAnchors())
				trustCircleProv.deleteAnchorFromCircle(circle.getName(), anchor.getThumbprint());
			
			trustCircleProv.deleteCircle(circle.getName());
		}

		for (Domain dom : prov.getDomains(false))
		{
			prov.deleteDomain(dom.getDomainName());
		}
		
		for (TrustAnchor anchor : trustAnchorProv.getAnchors())
			trustAnchorProv.deleteTrustAnchor(anchor.getThumbprint());
		
		for (TrustBundle bundle: trustBundleProv.getTrustBundles(false))
			trustBundleProv.deleteTrustBundle(bundle.getBundleName());
		
		for (Certificate cert : certProv.getCertificates())
			certProv.deleteCertificate(cert.getThumbprint());

	}
	
	@After
	public void tearDown()
	{
		try
		{
			DbConnectionManager.setConnectionProvider(oldConnectionProvider == null ? new DefaultConnectionProvider() : oldConnectionProvider);
		}
		catch (Exception e) {}
	}
	
}
