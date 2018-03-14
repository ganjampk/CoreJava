
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * <code>DigitalSigner</code> class provides a utility method to digitally sign
 * XML document. This implementation uses .p12 file as a source of signer's
 * digital certificate. In production environments, a hardware security module
 * (HSM) should be used for digitally signing.
 *
 * @author UIDAI
 *
 */
public class DigitalSigner {

	private static final String MEC_TYPE = "DOM";
	private static final String WHOLE_DOC_URI = "";
	private static final String KEY_STORE_TYPE = "PKCS12";

	private final KeyStore.PrivateKeyEntry keyEntry;

	// used for dongle
	private Provider provider;
	private static final String KEY_STORE_TYPE_DONGLE = "PKCS11";

	/**
	 * Constructor
	 *
	 * @param keyStoreFile
	 *        - Location of .p12 file
	 * @param keyStorePassword
	 *        - Password of .p12 file
	 * @param alias
	 *        - Alias of the certificate in .p12 file
	 */
	public DigitalSigner(final String keyStoreFile, final char[] keyStorePassword, final String alias) {
		this.keyEntry = getKeyFromKeyStore(keyStoreFile, keyStorePassword, alias);

		if (keyEntry == null) {
			throw new RuntimeException("Key could not be read for digital signature. Please check value of signature " + "alias and signature password, and restart the Auth Client");
		}
	}

	/**
	 * Constructor
	 *
	 * read key from dongle file
	 *
	 *
	 * @param safesignfile
	 * @param providerName
	 * @param pin
	 */
	public DigitalSigner(final String safesignfile, final char[] pin) {
		this.provider = new sun.security.pkcs11.SunPKCS11(safesignfile);
		Security.addProvider(this.provider);
		this.keyEntry = getPrivateKeyFromDongle(pin);

		if (keyEntry == null) {
			throw new RuntimeException("Key could not be read for digital signature. Please check value of signature " + "alias and signature password, and restart the Auth Client");
		}
	}

	/**
	 * Method to digitally sign an XML document.
	 *
	 * @param xmlDocument
	 *        - Input XML Document.
	 * @return Signed XML document
	 */
	public String signXML(final String xmlDocument, final boolean includeKeyInfo) {
		if (this.provider == null) {
			this.provider = new BouncyCastleProvider();
		}
		Security.addProvider(this.provider);

		try {
			// Parse the input XML
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final Document inputDocument = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlDocument)));

			// Sign the input XML's DOM document
			final Document signedDocument = sign(inputDocument, includeKeyInfo);

			// Convert the signedDocument to XML String
			final StringWriter stringWriter = new StringWriter();
			final TransformerFactory tf = TransformerFactory.newInstance();
			final Transformer trans = tf.newTransformer();
			trans.transform(new DOMSource(signedDocument), new StreamResult(stringWriter));

			return stringWriter.getBuffer().toString();
		} catch (final Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error while digitally signing the XML document", e);
		}
	}

	private Document sign(final Document xmlDoc, final boolean includeKeyInfo) throws Exception {

		if (System.getenv("SKIP_DIGITAL_SIGNATURE") != null) {
			return xmlDoc;
		}

		// Creating the XMLSignature factory.
		final XMLSignatureFactory fac = XMLSignatureFactory.getInstance(MEC_TYPE);
		// Creating the reference object, reading the whole document for
		// signing.
		final Reference ref = fac.newReference(WHOLE_DOC_URI, fac.newDigestMethod(DigestMethod.SHA1, null), Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

		// Create the SignedInfo.
		final SignedInfo sInfo = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null), fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));

		if (keyEntry == null) {
			throw new RuntimeException("Key could not be read for digital signature. Please check value of signature alias and signature password, and restart the Auth Client");
		}

		final X509Certificate x509Cert = (X509Certificate) keyEntry.getCertificate();

		final KeyInfo kInfo = getKeyInfo(x509Cert, fac);
		final DOMSignContext dsc = new DOMSignContext(this.keyEntry.getPrivateKey(), xmlDoc.getDocumentElement());
		final XMLSignature signature = fac.newXMLSignature(sInfo, includeKeyInfo ? kInfo : null);
		signature.sign(dsc);

		final Node node = dsc.getParent();
		return node.getOwnerDocument();

	}

	@SuppressWarnings("unchecked")
	private KeyInfo getKeyInfo(final X509Certificate cert, final XMLSignatureFactory fac) {
		// Create the KeyInfo containing the X509Data.
		final KeyInfoFactory kif = fac.getKeyInfoFactory();
		final List x509Content = new ArrayList();
		x509Content.add(cert.getSubjectX500Principal().getName());
		x509Content.add(cert);
		final X509Data xd = kif.newX509Data(x509Content);
		return kif.newKeyInfo(Collections.singletonList(xd));
	}

	private KeyStore.PrivateKeyEntry getKeyFromKeyStore(final String keyStoreFile, final char[] keyStorePassword, final String alias) {
		// Load the KeyStore and get the signing key and certificate.
		//FileInputStream keyFileStream = null;
		InputStream keyFileStream = null;

		try {
			final KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE);
			//keyFileStream = new FileInputStream(keyStoreFile);

			keyFileStream = DigitalSigner.class.getClassLoader().getResourceAsStream(keyStoreFile);
			ks.load(keyFileStream, keyStorePassword);

			final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(keyStorePassword));
			return entry;

		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (keyFileStream != null) {
				try {
					keyFileStream.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static KeyStore.PrivateKeyEntry getPrivateKeyFromDongle(final char[] keyStorePassword) {
		KeyStore ks;
		try {
			ks = KeyStore.getInstance(KEY_STORE_TYPE_DONGLE);
			ks.load(null, keyStorePassword);
			final Enumeration<String> alias = ks.aliases();
			String signAlias = "";

			while (alias.hasMoreElements()) {
				final String aliasName = alias.nextElement();
				final X509Certificate cert = (X509Certificate) ks.getCertificate(aliasName);
				final boolean[] keyUsage = cert.getKeyUsage();
				for (int i = 0; i < keyUsage.length; i++) {
					if ((i == 0 || i == 1) && keyUsage[i] == true) {
						signAlias = aliasName;
						break;
					}
				}
			}
			return (KeyStore.PrivateKeyEntry) ks.getEntry(signAlias, new KeyStore.PasswordProtection(keyStorePassword));

		} catch (final KeyStoreException e) {
			e.printStackTrace();
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (final UnrecoverableEntryException e) {
			e.printStackTrace();
		} catch (final CertificateException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
