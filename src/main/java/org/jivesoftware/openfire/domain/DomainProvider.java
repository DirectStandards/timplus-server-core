package org.jivesoftware.openfire.domain;

import java.util.Collection;

public interface DomainProvider
{
	public Domain getDomain(String domainName) throws DomainNotFoundException;
	
	public Collection<Domain> getDomains(boolean enabledOnly);
	
	public Collection<String> getDomainNames(boolean enabledOnly);	
	
	public Collection<Domain> findDomains(String searchStr, boolean enabledOnly);
	
	public int getDomainCount();
	
	public boolean isRegisteredDomain(String domainName);
	
	public Domain createDomain(String domainName, boolean enabled) throws DomainAlreadyExistsException;
	
	public void deleteDomain(String domainName);
	
	public void enableDomain(String domainName, boolean enable) throws DomainNotFoundException;
	
	public Collection<Domain> getDomainsByTrustCircle(String circleName);
}
