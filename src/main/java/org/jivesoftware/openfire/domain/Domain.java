package org.jivesoftware.openfire.domain;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.util.cache.CacheSizes;
import org.jivesoftware.util.cache.Cacheable;
import org.jivesoftware.util.cache.CannotCalculateSizeException;
import org.jivesoftware.util.cache.ExternalizableUtil;
import org.xmpp.resultsetmanagement.Result;

public class Domain implements Cacheable, Externalizable, Result
{
	private String domainName;
    private boolean enabled;
    private Date creationDate;
    private Date modificationDate;
	
    public Domain()
    {
    	
    }
    
    public Domain(String domainName, boolean enabled)
    {
    	this(domainName, enabled, null, null);
    }
    
    public Domain(String domainName, boolean enabled, Date creationDate, Date modificationDate)
    {
        if (StringUtils.isEmpty(domainName))         	
            throw new IllegalArgumentException("Domain name cannot be null or empty");
        
        this.domainName = domainName;
        this.enabled = enabled;
        
        this.creationDate = (creationDate == null ) ? Calendar.getInstance().getTime() : creationDate;
        this.modificationDate = (modificationDate == null ) ? Calendar.getInstance().getTime() : modificationDate;
    }
    
	public String getDomainName()
	{
		return domainName;
	}

	public Date getCreationDate()
	{
		return creationDate;
	}

	public void setCreationDate(Date creationDate)
	{
		this.creationDate = creationDate;
	}

	public Date getModificationDate()
	{
		return modificationDate;
	}

	public void setModificationDate(Date modificationDate)
	{
		this.modificationDate = modificationDate;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

    @Override
    public String toString() 
    {
        return domainName;
    }
	
    @Override
    public boolean equals(Object object) 
    {
        if (this == object) 
            return true;
       
        if (object != null && object instanceof Domain)
        {
        	final Domain dom = (Domain)object;
            return domainName.equals(dom.getDomainName()) && dom.isEnabled() == this.enabled;
        }
        else
            return false;
    }    
    
    @Override
    public int hashCode() 
    {
        return domainName.hashCode();
    }
	
	@Override
	public String getUID()
	{
		return domainName;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
        ExternalizableUtil.getInstance().writeSafeUTF(out, domainName);
        ExternalizableUtil.getInstance().writeBoolean(out, enabled);
        ExternalizableUtil.getInstance().writeLong(out, creationDate.getTime());
        ExternalizableUtil.getInstance().writeLong(out, modificationDate.getTime());		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
        domainName = ExternalizableUtil.getInstance().readSafeUTF(in);
        enabled = ExternalizableUtil.getInstance().readBoolean(in);
        creationDate = new Date(ExternalizableUtil.getInstance().readLong(in));
        modificationDate = new Date(ExternalizableUtil.getInstance().readLong(in));
	}

	@Override
	public int getCachedSize() throws CannotCalculateSizeException
	{
        // Approximate the size of the object in bytes by calculating the size
        // of each field.
        int size = 0;
        size += CacheSizes.sizeOfObject();              // overhead of object
        size += CacheSizes.sizeOfString(domainName);    // domainName
        size += CacheSizes.sizeOfBoolean();             // enabled
        size += CacheSizes.sizeOfDate() * 2;            // creationDate and modificationDate
        return size;
	}

}
