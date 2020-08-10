package org.jivesoftware.openfire.trustbundle;

import java.security.cert.X509Certificate;
import java.util.Collection;


public interface TrustBundleProvider
{
	public Collection<TrustBundle> getTrustBundles(boolean loadAnchors) throws TrustBundleException;
	
	public TrustBundle getTrustBundle(String bundleName) throws TrustBundleException;
	
	public TrustBundle addTrustBundle(TrustBundle bundle) throws TrustBundleException;
	
	public TrustBundle updateTrustBundleAttributes(String bundleName, TrustBundle updatedBundle) throws TrustBundleException;
	
	public TrustBundle updateSigningCertificate(String bundleName, byte[] signingCert) throws TrustBundleException;
	
	public void deleteTrustBundle(String bundleName) throws TrustBundleException;
	
	public void deleteAnchorsByBundleId(String bundleId) throws TrustBundleException;
	
	public TrustBundleAnchor addTrustBundleAnchor(X509Certificate anchor, String trustBundleId) throws TrustBundleException;
}
