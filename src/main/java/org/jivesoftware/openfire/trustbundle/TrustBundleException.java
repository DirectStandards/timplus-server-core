package org.jivesoftware.openfire.trustbundle;

import java.io.PrintStream;
import java.io.PrintWriter;

public class TrustBundleException extends Exception 
{
	private static final long serialVersionUID = -7163020225025833183L;

	protected Throwable nestedThrowable = null;

    public TrustBundleException() 
    {
        super();
    }

    public TrustBundleException(String msg) 
    {
        super(msg);
    }

    public TrustBundleException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public TrustBundleException(String msg, Throwable nestedThrowable) 
    {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }

    @Override
    public void printStackTrace() 
    {
        super.printStackTrace();
        if (nestedThrowable != null) 
            nestedThrowable.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream ps) 
    {
        super.printStackTrace(ps);
        if (nestedThrowable != null) 
            nestedThrowable.printStackTrace(ps);
    }

    @Override
    public void printStackTrace(PrintWriter pw) 
    {
        super.printStackTrace(pw);
        if (nestedThrowable != null)
            nestedThrowable.printStackTrace(pw);
    }
}
