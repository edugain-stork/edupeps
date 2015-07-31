package umu.eadmin.servicios.umu2stork;

/*
 * Copyright (C) 2015 Elena Torroglosa (emtg@um.es)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.x509.BasicX509Credential;


/**
 * @author Elena Torroglosa
 * Stores signature credentials
 */
public class SigningCredential {
    
    private final static Logger logger = Logger.getLogger(umu.eadmin.servicios.umu2stork.SigningCredential.class.getName());
    private Credential signingCredential = null;

    void intializeCredentials(String passwordParam, String certAliasNameParam, String keyAliasNameParam, String fileNameParam) {
        KeyStore ks = null;
        FileInputStream fis = null;
        char[] password = passwordParam.toCharArray();

        // Get Default Instance of KeyStore
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            logger.severe("Error while Intializing Keystore"+ e);
        }

        // Read Ketstore as file Input Stream
        try {
            fis = new FileInputStream(fileNameParam);
        } catch (FileNotFoundException e) {
            logger.severe("Unable to found KeyStore with the given keystoere name ::" + fileNameParam + " " + e);
        }

        // Load KeyStore
        try {
            ks.load(fis, password);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("Failed to Load the KeyStore:: "+ e);
        } catch (CertificateException e) {
            logger.severe("Failed to Load the KeyStore:: "+ e);
        } catch (IOException e) {
            logger.severe("Failed to Load the KeyStore:: "+ e);
        }

        // Close InputFileStream
        try {
            fis.close();
        } catch (IOException e) {
            logger.severe("Failed to close file stream:: "+ e);
        }

        // Get Private Key Entry From Certificate
        KeyStore.PrivateKeyEntry keyEntry = null;
        KeyStore.TrustedCertificateEntry certEntry = null;
        try {
            certEntry = (KeyStore.TrustedCertificateEntry) ks.getEntry(certAliasNameParam, null);
            keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(keyAliasNameParam, new KeyStore.PasswordProtection(passwordParam.toCharArray()));

        } catch (NoSuchAlgorithmException e) {
            logger.severe("Failed to Get Private Entry From the keystore:: " + fileNameParam + " " + e);
        } catch (UnrecoverableEntryException e) {
            logger.severe("Failed to Get Private Entry From the keystore:: " + fileNameParam + " " + e);
        } catch (KeyStoreException e) {
            logger.severe("Failed to Get Private Entry From the keystore:: " + fileNameParam + " " + e);
        }
        PrivateKey pk = keyEntry.getPrivateKey();

        X509Certificate certificate = (X509Certificate) certEntry.getTrustedCertificate();
        BasicX509Credential credential = new BasicX509Credential();
        credential.setEntityCertificate(certificate);
        credential.setPrivateKey(pk);
        signingCredential = credential;

        logger.info("Private Key" + pk.toString());
    }
    
    public Credential getSigningCredential() {
        return signingCredential;
    }
}
