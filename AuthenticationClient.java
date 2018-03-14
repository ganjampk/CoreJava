
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.sax.SAXSource;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.WebResource;

/**
 * <code>AuthClient</code> class can be used for submitting an Authentication
 * request to UIDAI Auth Server, and to get the response back. Given an
 * <code>Auth</code> object, this class (@see {@link AuthClient#authenticate})
 * will convert it to XML string, then, digitally sign it, and submit it to
 * UIDAI Auth Server using HTTP POST message. After, receiving the resonse, this
 * class converts the response XML into authentication response
 * 
 * @see AuthRes object
 * 
 * 
 * @author UIDAI
 * 
 */
public class AuthClient {
	private URI authServerURI = null;
	private String asaLicenseKey;
	private DigitalSigner digitalSignator;
	private String uidaiServerTimeout;
	/**
	 * Constructor
	 * 
	 * @param authServerUri
	 *            - URI of the authentication server
	 */
	public AuthClient(URI authServerUri, final String uidaiServerTimeout) {
		this.authServerURI = authServerUri;
		this.uidaiServerTimeout = uidaiServerTimeout;
	}

	/**
	 * Method to perform authentication
	 * 
	 * @param auth
	 *            Authentication request
	 * @return Authentication response
	 */
	public AuthResponseDetails authenticate(Auth auth) {
		try {
			String signedXML = generateSignedAuthXML(auth);
			// System.out.println(signedXML);

			String uriString = authServerURI.toString()
					+ (authServerURI.toString().endsWith("/") ? "" : "/")
					+ auth.getAc() + "/" + auth.getUid().charAt(0) + "/"
					+ auth.getUid().charAt(1);

			if (StringUtils.isNotBlank(asaLicenseKey)) {
				uriString = uriString + "/" + asaLicenseKey;
			}

			URI authServiceURI = new URI(uriString);

			WebResource webResource = Client
					.create(
							HttpClientHelper.getClientConfig(authServerURI
									.getScheme(),uidaiServerTimeout)).resource(authServiceURI);

			String responseXML = webResource.header("REMOTE_ADDR",
					InetAddress.getLocalHost().getHostAddress()).post(
					String.class, signedXML);

			System.out.println(responseXML);

			return new AuthResponseDetails(responseXML,
					parseAuthResponseXML(responseXML));

		}catch (ClientHandlerException timeoutex) {
			timeoutex.printStackTrace();
			
		} 
		catch (SocketTimeoutException ex) {
			ex.printStackTrace();
			
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Exception during authentication "
					+ e.getMessage(), e);
		}
	}

	public String generateSignedAuthXML(Auth auth) throws JAXBException,
			Exception {
		StringWriter authXML = new StringWriter();

		JAXBElement authElement = new JAXBElement(new QName(
				"http://www.uidai.gov.in/authentication/uid-auth-request/1.0",
				"Auth"), Auth.class, auth);

		JAXBContext.newInstance(Auth.class).createMarshaller().marshal(
				authElement, authXML);
		boolean includeKeyInfo = true;

		if (System.getenv().get("SKIP_DIGITAL_SIGNATURE") != null) {
			return authXML.toString();
		} else {
			return this.digitalSignator.signXML(authXML.toString(),
					includeKeyInfo);
		}
	}

	private AuthRes parseAuthResponseXML(String xmlToParse)
			throws JAXBException {

		// Create an XMLReader to use with our filter
		try {
			// Prepare JAXB objects
			JAXBContext jc = JAXBContext.newInstance(AuthRes.class);
			Unmarshaller u = jc.createUnmarshaller();

			XMLReader reader;
			reader = XMLReaderFactory.createXMLReader();

			// Create the filter (to add namespace) and set the xmlReader as its
			// parent.
			NamespaceFilter inFilter = new NamespaceFilter(
					"http://www.uidai.gov.in/authentication/uid-auth-response/1.0",
					true);
			inFilter.setParent(reader);

			// Prepare the input, in this case a java.io.File (output)
			InputSource is = new InputSource(new StringReader(xmlToParse));

			// Create a SAXSource specifying the filter
			SAXSource source = new SAXSource(inFilter, is);

			// Do unmarshalling
			AuthRes res = u.unmarshal(source, AuthRes.class).getValue();
			return res;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Method to inject an instance of <code>DigitalSigner</code> class.
	 * 
	 * @param digitalSignator
	 */
	public void setDigitalSignator(DigitalSigner digitalSignator) {
		this.digitalSignator = digitalSignator;
	}

	public void setAsaLicenseKey(String asaLicenseKey) {
		this.asaLicenseKey = asaLicenseKey;
	}

}
