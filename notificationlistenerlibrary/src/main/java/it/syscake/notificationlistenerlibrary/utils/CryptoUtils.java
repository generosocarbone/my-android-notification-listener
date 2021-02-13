package it.syscake.notificationlistenerlibrary.utils;

import android.content.Context;

import com.yakivmospan.scytale.Crypto;
import com.yakivmospan.scytale.Options;
import com.yakivmospan.scytale.Store;

import javax.crypto.SecretKey;

import it.systemslab.cryptomodule.DHKEInstance;

public class CryptoUtils {
    
    private static final String ALIAS = "KEY_ALIAS";

    public static DHKEInstance getInstance() {
        return DHKEInstance.getInstance();
    }

    public static String encryptData(String data, Context context) {
        Store store = new Store(context);
        SecretKey key;
        if (!store.hasKey(ALIAS)) {
             key = store.generateSymmetricKey(ALIAS, null);
        } else {
            key = store.getSymmetricKey(ALIAS, null);
        }

        // encrypt data
        Crypto crypto = new Crypto(Options.TRANSFORMATION_SYMMETRIC);

        return crypto.encrypt(data, key);
    }

    public static String decryptData(String encryptedData, Context context) {
        if (encryptedData == null)
            return "";

        Store store = new Store(context);
        SecretKey key;
        if (!store.hasKey(ALIAS)) {
            key = store.generateSymmetricKey(ALIAS, null);
        } else {
            key = store.getSymmetricKey(ALIAS, null);
        }

        // decrypt data
        Crypto crypto = new Crypto(Options.TRANSFORMATION_SYMMETRIC);

        return crypto.decrypt(encryptedData, key);
    }
}
