package org.jivesoftware.openfire.trustbundle.processor;

import org.jivesoftware.openfire.trustbundle.TrustBundle;

public interface BundleRefreshProcessor
{
	public void refreshBundle(TrustBundle bundle);
}
