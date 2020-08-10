package org.jivesoftware.openfire.trustcircle;

import java.util.Collection;

public interface TrustCircleProvider
{
	/**
	 * Gets all trust circles in the system.
	 * @param loadBundles Indicates if trust bundles will be returned with the trust circle.
	 * @param loadAnchors Indicates if trust anchors 
	 * @return All trust circles in the system.  Returns an empty list if no circles are present in the system.
	 * @throws ServiceException
	 */
	public Collection<TrustCircle> getTrustCircles(boolean loadBundles, boolean loadAnchors) throws TrustCircleException;
	
	
	/**
	 * Gets a trust anchor by name.
	 * @param circleName The name of the trust circle to retrieve.
	 * @param loadBundles Indicates if trust bundles will be returned with the trust circle.
	 * @param loadAnchors Indicates if trust anchors 
	 * @return The trust circle corresponding to the requested name.  Returns null if a trust circle cannot be found.
	 * @throws ServiceException
	 */
	public TrustCircle getTrustCircle(String circleName, boolean loadBundles, boolean loadAnchors) throws TrustCircleException;
	
	/**
	 * Adds a new trust circles to the system with an initial set of the trust anchors.  Trust circles names
	 * are unique meaning two circles with the same name (case insensitive) cannot exist.
	 * @param circleName The name of the circle to add.
	 * @param thumbprints Collection of thumb prints representing the {@link TrustAnchor TrustAnchors} that will be
	 * associated with the circle.
	 * @return The newly created trust circle.
	 * @throws ServiceException Thrown if a circle with the same name already exists.
	 */
	public TrustCircle addTrustCircle(String circleName, Collection<String> thumbprints) throws TrustCircleException;
	
	/**
	 * Adds an anchor to an existing trust circle.
	 * @param circleName The name of the circle to add anchors to.
	 * @param thumbprint Thumb prints representing the {@link TrustAnchor TrustAnchor} that will be
	 * associated with the circle.
	 * @return The update trust circle with the new anchor.
	 * @throws ServiceException Thrown if a circle with the requested name does not exist.
	 */
	public TrustCircle addAnchorToCircle(String circleName, String thumbprint) throws TrustCircleException;
	
	/**
	 * Deletes an anchor from an existing trust circles.
	 * @param circleName The name of the circle to removed anchors from.
	 * @param thumbprint Thumb print representing the {@link TrustAnchor TrustAnchor} that will be
	 * removed from the circle.
	 * @return The update trust circle with the anchor removed.
	 * @throws ServiceException Thrown if a circle with the requested name does not exist.
	 */
	public TrustCircle deleteAnchorFromCircle(String circleName, String thumbprint) throws TrustCircleException;
	
	/**
	 * Adds a trust bundle with directional trust to a trust circle.
	 * @param circleName The name of the trust circle.
	 * @param bundleName The trust bundle name to add to the trust circle.
	 * @return The update trust circle with the new trust bundles.
	 * @throws ServiceException Thrown if a circle with the requested name does not exist.
	 */
	public TrustCircle addTrustBundleToCircle(String circleName, String bundleName) throws TrustCircleException;
	
	/**
	 * Deletes trust bundles from an existing trust circle.
	 * @param circleName The name of the circle to removed trust bundles from.
	 * @param bundleNames Collection of bundles names that will be
	 * removed from the circle.
	 * @return The update trust circle with the trust bundles removed.
	 * @throws ServiceException Thrown if a circle with the requested name does not exist.
	 */
	public TrustCircle deleteTrustBundlesFromCircle(String circleName, Collection<String> bundleNames) throws TrustCircleException;
	
	/**
	 * Deletes a trust circle from the system.
	 * @param circleName Name of the trust circle to delete.
	 * @throws ServiceException
	 */
	public void deleteCircle(String circleName) throws TrustCircleException;
	
	/**
	 * Gets the trust circles associated with a domain.
	 * @param domainName The domain to retrieve trust circles for.
	 * @param loadBundles Indicates if trust bundles will be returned with the trust circle.
	 * @param loadAnchors Indicates if trust anchors 
	 * @return A collection of trust circles associated with the domain.  Returns an empty list if the domain does not exist or
	 * if no circles are associated with the domain.
	 * @throws ServiceException
	 */
	public Collection<TrustCircle> getCirclesByDomain(String domainName, boolean loadBundles, boolean loadAnchors) throws TrustCircleException;	
	
	/**
	 * Adds trust circles to a domain.
	 * @param domainName The domain to add the trust circles to.
	 * @param circleNames Collection of trust circles names to add to the domain.
	 * @throws ServiceException Thrown if the domain names does not exist.
	 */
	public void addCirclesToDomain(String domainName, Collection<String> circleNames) throws TrustCircleException;
	
	/**
	 * Adds domains to a trust circle
	 * @param circleName The trust circle to add the domains to.
	 * @param domainNames Collection of domain names to add to the trust circle.
	 * @throws ServiceException Thrown if the trust circle names does not exist.
	 */
	public void addDomainsToCircle(String circleName, Collection<String> domainNames) throws TrustCircleException;	
	
	/**
	 * Delete trust circles from a domain.
	 * @param domainName The domain to remove the trust circles from.
	 * @param circleNames Collection of trust circles names to remove from the domain.
	 * @throws ServiceException Thrown if the domain names does not exist.
	 */
	public void deleteCirclesFromDomain(String domainName, Collection<String> circleNames) throws TrustCircleException;
	
	/**
	 * Delete domains from a trust circle.
	 * @param circleName The trust circle to remove the domains from.
	 * @param domainNames Collection of domain names to remove from the trust circle.
	 * @throws ServiceException Thrown if the domain names does not exist.
	 */
	public void deleteDomainsFromCircle(String circleName, Collection<String> domainNames) throws TrustCircleException;	
	
}
