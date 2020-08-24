package org.jivesoftware.openfire.keystore.jce;

import static org.junit.Assert.assertNotNull;

import java.security.KeyStore;

import org.directtruststandards.timplus.common.crypto.CryptoUtils;
import org.junit.Test;

public class TIMPlusKeyStoreProvider_keyStoreTest
{
	static
	{
		CryptoUtils.registerJCEProvider(new TIMPlusKeyStoreProvider());
	}
	
	@Test
	public void testKeyStoreCreate_assertCreated() throws Exception
	{		
		final KeyStore store = KeyStore.getInstance(TIMPlusKeyStoreProvider.KEY_STORE_TYPE);
		
		assertNotNull(store);
	}
}
