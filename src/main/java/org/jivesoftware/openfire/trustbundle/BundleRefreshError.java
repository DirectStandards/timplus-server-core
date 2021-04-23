package org.jivesoftware.openfire.trustbundle;

public enum BundleRefreshError
{
	/**
	 * Successful update.
	 */
	SUCCESS,
	
	/**
	 * The bundle was not found at the URL specified.
	 */
	NOT_FOUND,
	
	/**
	 * The download from the URL timed out.
	 */
	DOWNLOAD_TIMEOUT,
	
	/**
	 * The bundle is either corrupt or in an unrecognized format.
	 */
	INVALID_BUNDLE_FORMAT,
	
	/**
	 * The signing certificate specified is not a valid certificate.
	 */
	INVALID_SIGNING_CERT,
	
	/**
	 * The signature on the signed bundle did not validate successfully against the signing certificate.
	 */
	UNMATCHED_SIGNATURE
}
