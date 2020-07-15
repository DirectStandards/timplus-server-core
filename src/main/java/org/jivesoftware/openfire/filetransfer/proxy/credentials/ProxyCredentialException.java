package org.jivesoftware.openfire.filetransfer.proxy.credentials;

import java.io.PrintStream;
import java.io.PrintWriter;

public class ProxyCredentialException extends Exception
{
	private static final long serialVersionUID = 6088520484175835501L;
	
	private Throwable nestedThrowable = null;

    public ProxyCredentialException() 
    {
        super();
    }

    public ProxyCredentialException(String msg) 
    {
        super(msg);
    }

    public ProxyCredentialException(Throwable nestedThrowable) 
    {
        this.nestedThrowable = nestedThrowable;
    }

    public ProxyCredentialException(String msg, Throwable nestedThrowable) 
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
