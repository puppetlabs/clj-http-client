package com.puppetlabs.http.client.impl;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class SslUtils {


    // TODO: this code is copied verbatim from the CA library; we should get rid
    // of this file and just specify a dependency on that library once we release it


    /**
     * Given a PEM reader, decode the contents into a collection of objects of the corresponding
     * type from the java.security package.
     *
     * @param reader Reader for a PEM-encoded stream
     * @return The list of decoded objects from the stream
     * @throws IOException
     * @see #writeToPEM
     */
    public static List<Object> pemToObjects(Reader reader)
            throws IOException
    {
        PEMParser parser = new PEMParser(reader);
        List<Object> results = new ArrayList<Object>();
        for (Object o = parser.readObject(); o != null; o = parser.readObject())
            results.add(o);
        return results;
    }


    /**
     * Decodes the provided object (read from a PEM stream via {@link #pemToObjects}) into a private key.
     *
     * @param obj The object to decode into a PrivateKey
     * @return The PrivateKey decoded from the object
     * @throws PEMException
     * @see #pemToPrivateKey
     * @see #pemToPrivateKeys
     */
    public static PrivateKey objectToPrivateKey(Object obj)
            throws PEMException
    {
        // Certain PEMs will hand back a keypair with a nil public key
        if (obj instanceof PrivateKeyInfo)
            return new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) obj);
        else if (obj instanceof PEMKeyPair)
            return new JcaPEMKeyConverter().getKeyPair((PEMKeyPair) obj).getPrivate();
        else
            throw new IllegalArgumentException("Expected a KeyPair or PrivateKey, got " + obj);
    }

    /**
     * Given a PEM reader, decode the contents into a list of private keys.
     *
     * @param reader Reader for a PEM-encoded stream
     * @return The list of decoded private keys from the stream
     * @throws IOException
     * @throws PEMException
     * @see #pemToPrivateKey
     * @see #writeToPEM
     */
    public static List<PrivateKey> pemToPrivateKeys(Reader reader)
            throws IOException, PEMException
    {
        List<Object> objects = pemToObjects(reader);
        List<PrivateKey> results = new ArrayList<PrivateKey>(objects.size());
        for (Object o : objects)
            results.add(objectToPrivateKey(o));
        return results;
    }

    /**
     * Given a PEM reader, decode the contents into a private key.
     * Throws an exception if multiple keys are found.
     *
     * @param reader Reader for a PEM-encoded stream
     * @return The decoded private key from the stream
     * @throws IOException
     * @throws IllegalArgumentException
     * @see #pemToPrivateKeys
     * @see #writeToPEM
     */
    public static PrivateKey pemToPrivateKey(Reader reader)
            throws IOException
    {
        List<PrivateKey> privateKeys = pemToPrivateKeys(reader);
        if (privateKeys.size() != 1)
            throw new IllegalArgumentException("The PEM stream must contain exactly one private key");
        return privateKeys.get(0);
    }


    /**
     * Given a PEM reader, decode the contents into a list of certificates.
     *
     * @param reader Reader for a PEM-encoded stream
     * @return The list of decoded certificates from the stream
     * @throws CertificateException
     * @throws IOException
     * @see #writeToPEM
     */
    public static List<X509Certificate> pemToCerts(Reader reader)
            throws CertificateException, IOException
    {
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        List<Object> pemObjects = pemToObjects(reader);
        List<X509Certificate> results = new ArrayList<X509Certificate>(pemObjects.size());
        for (Object o : pemObjects)
            results.add(converter.getCertificate((X509CertificateHolder) o));
        return results;
    }


    /**
     * Add a private key to a keystore.
     *
     * @param keystore The keystore to add the private key to
     * @param alias An alias to associate with the private key
     * @param privateKey The private key to add to the keystore
     * @param password To protect the key in the keystore
     * @param cert The certificate for the private key; a private key cannot
     *             be added to a keystore without a signed certificate
     * @return The provided keystore
     * @throws KeyStoreException
     * @see #associatePrivateKeyFromReader
     */
    public static KeyStore associatePrivateKey(KeyStore keystore, String alias, PrivateKey privateKey,
                                               String password, X509Certificate cert)
            throws KeyStoreException
    {
        keystore.setKeyEntry(alias, privateKey, password.toCharArray(), new Certificate[]{cert});
        return keystore;
    }

    /**
     * Add the private key from a PEM reader to the keystore.
     *
     * @param keystore The keystore to add the private key to
     * @param alias An alias to associate with the private key
     * @param pemPrivateKey Reader for a PEM-encoded stream with the private key
     * @param password To protect the key in the keystore
     * @param pemCert Reader for a PEM-encoded stream with the certificate; a private
     *                key cannot be added to a keystore without a signed certificate
     * @return The provided keystore
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @see #associatePrivateKey
     */
    public static KeyStore associatePrivateKeyFromReader(KeyStore keystore, String alias, Reader pemPrivateKey,
                                                         String password, Reader pemCert)
            throws CertificateException, KeyStoreException, IOException
    {
        PrivateKey privateKey = pemToPrivateKey(pemPrivateKey);
        List<X509Certificate> certs = pemToCerts(pemCert);

        if (certs.size() > 1)
            throw new IllegalArgumentException("The PEM stream contains more than one certificate");

        X509Certificate firstCert = certs.get(0);
        return associatePrivateKey(keystore, alias, privateKey, password, firstCert);
    }

    /**
     * Add a certificate to a keystore.
     *
     * @param keystore The keystore to add the certificate to
     * @param alias An alias to associate with the certificate
     * @param cert The certificate to add to the keystore
     * @return The provided keystore
     * @throws KeyStoreException
     * @see #associateCertsFromReader
     */
    public static KeyStore associateCert(KeyStore keystore, String alias, X509Certificate cert)
            throws KeyStoreException
    {
        keystore.setCertificateEntry(alias, cert);
        return keystore;
    }

    /**
     * Add all certificates from a PEM reader to the keystore.
     *
     * @param keystore The keystore to add all the certificates to
     * @param prefix An alias to associate with the certificates. Each certificate will
     *               have a numeric index appended to the prefix (starting with '-0')
     * @param pem Reader for a PEM-encoded stream of certificates
     * @return The provided keystore
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @see #associateCert
     */
    public static KeyStore associateCertsFromReader(KeyStore keystore, String prefix, Reader pem)
            throws CertificateException, KeyStoreException, IOException
    {
        List<X509Certificate> certs = pemToCerts(pem);
        ListIterator<X509Certificate> iter = certs.listIterator();
        for (int i = 0; iter.hasNext(); i++)
            associateCert(keystore, prefix + "-" + i, iter.next());
        return keystore;
    }



    /**
     * Create an empty in-memory key store.
     *
     * @return New key store
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    public static KeyStore createKeyStore()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null);
        return ks;
    }

    /**
     * Given PEM readers for a certificate, private key, and CA certificate,
     * create an in-memory keystore and truststore.
     *
     * Returns a map containing the following:
     * <ul>
     *  <li>"keystore" - a keystore initialized with the cert and private key</li>
     *  <li>"keystore-pw" - a string containing a dynamically generated password for the keystore</li>
     *  <li>"truststore" - a keystore containing the CA cert</li>
     * <ul>
     *
     * @param cert Reader for a PEM-encoded stream with the certificate
     * @param privateKey Reader for a PEM-encoded stream with the correspnding private key
     * @param caCert Reader for a PEM-encoded stream with the CA certificate
     * @return Map containing the keystore, keystore password, and truststore
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static Map<String, Object> pemsToKeyAndTrustStores(Reader cert, Reader privateKey, Reader caCert)
            throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException
    {
        KeyStore truststore = createKeyStore();
        associateCertsFromReader(truststore, "CA Certificate", caCert);

        KeyStore keystore = createKeyStore();
        String keystorePassword = UUID.randomUUID().toString();
        associatePrivateKeyFromReader(keystore, "Private Key", privateKey, keystorePassword, cert);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("truststore", truststore);
        result.put("keystore", keystore);
        result.put("keystore-pw", keystorePassword);
        return result;
    }


    /**
     * Given a keystore and keystore password (as generated by {@link #pemsToKeyAndTrustStores}),
     * return a key manager factory that contains the keystore.
     *
     * @param keystore The keystore to get a key manager for
     * @param password The password for the keystore
     * @return A key manager factory for the provided keystore
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     */
    public static KeyManagerFactory getKeyManagerFactory(KeyStore keystore, String password)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
    {
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keystore, password.toCharArray());
        return factory;
    }

    /**
     * Given a truststore (as generated by {@link #pemsToKeyAndTrustStores}),
     * return a trust manager factory that contains the truststore.
     *
     * @param truststore The truststore to get a trust manager for
     * @return A trust manager factory for the provided truststore
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public static TrustManagerFactory getTrustManagerFactory(KeyStore truststore)
            throws NoSuchAlgorithmException, KeyStoreException
    {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(truststore);
        return factory;
    }

    /**
     * Given PEM readers for a certificate, private key, and CA certificate, create an
     * in-memory SSL context initialized with a keystore/truststore generated from the
     * provided certificates and key.
     *
     * @param cert Reader for PEM-encoded stream with the certificate
     * @param privateKey Reader for PEM-encoded stream with the corresponding private key
     * @param caCert Reader for PEM-encoded stream with the CA certificate
     * @return The configured SSLContext
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    public static SSLContext pemsToSSLContext(Reader cert, Reader privateKey, Reader caCert)
            throws KeyStoreException, CertificateException, IOException,
            NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException
    {
        Map<String, Object> stores = pemsToKeyAndTrustStores(cert, privateKey, caCert);
        KeyStore keystore = (KeyStore) stores.get("keystore");
        String password = (String) stores.get("keystore-pw");
        KeyStore truststore = (KeyStore) stores.get("truststore");
        KeyManagerFactory kmf = getKeyManagerFactory(keystore, password);
        TrustManagerFactory tmf = getTrustManagerFactory(truststore);
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return context;
    }
}