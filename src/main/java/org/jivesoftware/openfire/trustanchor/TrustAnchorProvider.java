package org.jivesoftware.openfire.trustanchor;

import java.security.cert.X509Certificate;
import java.util.Collection;

public interface TrustAnchorProvider
{
	/**
	 * Gets all trust anchors in the store
	 * @return All trust anchors in the store
	 * @throws CertificateDAOException
	 */
	public Collection<TrustAnchor> getAnchors() throws TrustAnchorException;
	
	/**
	 * Gets a single trust anchor by thumb print
	 * @param thumbprint The thumb print to search for
	 * @return The trust anchor that matches the provided thumb print.  Returns null if an anchor cannot be located
	 * @throws CertificateDAOException
	 */
	public TrustAnchor getAnchorByThumbprint(String thumbprint) throws TrustAnchorException;
	
	/**
	 * Gets a collection of trust anchor from a list of thumb prints.
	 * @param thumbprints Collection of thumb print to search for
	 * @return Collection of trust anchors matching the list of thumb prints.  The returned list may be a sub set of the requested list
	 * if all of the anchors cannot be found.  Returns an empty list if no anchors can be found 
	 * @throws CertificateDAOException
	 */
	public Collection<TrustAnchor> getAnchorsByThumbprints(Collection<String> thumbprint)  throws TrustAnchorException;	
	
	/**
	 * Adds an anchor to the store.
	 * @param anchor The anchor to add.
	 * @throws CertificateDAOException Throws a {@link TrustAnchorException} exception if the anchor already exists.
	 */
	public TrustAnchor addTrustAnchor(X509Certificate anchor) throws TrustAnchorException;	
	
	/**
	 * Deletes a list of anchors from the store by thumb print.
	 * @param thumbprints Collection of thumb print indicating which anchors to delete.
	 * @throws CertificateDAOException
	 */
	public void deleteTrustAnchors(Collection<String> thumbprints) throws TrustAnchorException;	
	
	/**
	 * Deletes a single anchor from the store by thumb print
	 * @param thumbprint Thumb print of the anchor to delete
	 * @throws CertificateDAOException
	 */
	public void deleteTrustAnchor(String thumbprint) throws TrustAnchorException;
}
