package umu.eadmin.servicios.umu2stork;

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


public class SigningCredential {
    
    private final static Logger logger = Logger.getLogger(umu.eadmin.servicios.umu2stork.SigningCredential.class.getName());
    private Credential signingCredential = null;

    // final Signature signature = null;
    // final String password = "secret";
    // final String certificateAliasName = "selfsigned";
    // final String fileName = "/opt/keystores/storkDemoKeys.jks";

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
            //pkEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(certificateAliasNameParam, new KeyStore.PasswordProtection(passwordParam.toCharArray()));
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
