package org.jivesoftware.openfire.keystore.jce;

import java.security.Provider;

public class TIMPlusKeyStoreProvider extends Provider
{
	private static final long serialVersionUID = -4141472277270893416L;

	public static final String PROVIDER_NAME = "TIMPLUSKEYSTOREPRIVDER";
	
	public static final String KEY_STORE_TYPE = "TIMPLUSCertMgr";
	
	public TIMPlusKeyStoreProvider()
	{
		super(PROVIDER_NAME, 1.0, "TIMPlus KeyStore Provider");
		
		this.put("KeyStore." + KEY_STORE_TYPE, CertManagerKeyStore.class.getName());
	}
	
	
}
