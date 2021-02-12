package org.jivesoftware.openfire.cluster;

public class ClusterNodeNotFoundException extends ClusterException
{
	private static final long serialVersionUID = 9056213942115533004L;

	public ClusterNodeNotFoundException() 
    {
        super();
    }

    public ClusterNodeNotFoundException(String msg) 
    {
        super(msg);
    }

    public ClusterNodeNotFoundException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public ClusterNodeNotFoundException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }
}
