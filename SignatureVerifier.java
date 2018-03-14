import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Class that verifies digital signature of a signed XML document.
 *
 * @author UIDAI
 *
 */
public class SignatureVerifier {

	private String publicKeyFile = "";

	/**
	 * Constructor
	 * 
	 * @param publicKeyFile File name of signer's public key file (.cer)
	 */
	public SignatureVerifier(final String publicKeyFile) {
		this.publicKeyFile = publicKeyFile;
	}

	public boolean verify(final String signedXml) {

		boolean verificationResult = false;

		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final Document signedDocument = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(signedXml)));

			final NodeList nl = signedDocument.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			if (nl.getLength() == 0) {
				throw new IllegalArgumentException("Cannot find Signature element");
			}

			final XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

			final DOMValidateContext valContext = new DOMValidateContext(getCertificateFromFile(publicKeyFile).getPublicKey(), nl.item(0));
			final XMLSignature signature = fac.unmarshalXMLSignature(valContext);

			verificationResult = signature.validate(valContext);

		} catch (final Exception e) {
			System.out.println("Error while verifying digital siganature" + e.getMessage());
			e.printStackTrace();
		}

		return verificationResult;
	}

	private X509Certificate getCertificateFromFile(final String certificateFile) throws GeneralSecurityException, IOException {
		FileInputStream fis = null;
		try {
			final CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
			fis = new FileInputStream(certificateFile);
			return (X509Certificate) certFactory.generateCertificate(fis);
		} finally {
			if (fis != null) {
				fis.close();
			}
		}

	}

}
