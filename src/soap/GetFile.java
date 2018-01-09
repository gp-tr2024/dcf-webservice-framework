package soap;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import config.Config;
import user.IDcfUser;

/**
 * Generic get file request to the dcf.
 * @author avonva
 *
 */
public abstract class GetFile extends SOAPRequest {

	private static final String NAMESPACE = "http://dcf-elect.efsa.europa.eu/";
	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dcf-dp-ws/elect2/?wsdl";
	
	private String resourceId;
	
	/**
	 * Make a get file request for a specific resource
	 * @param resourceId
	 */
	public GetFile(IDcfUser user, String resourceId) {
		super(user, NAMESPACE);
		this.resourceId = resourceId;
	}
	
	/**
	 * Get the url for making get file requests
	 * @return
	 */
	public static String getUrl() {
		Config config = new Config();
		return config.isProductionEnvironment() ? URL : TEST_URL;
	}
	
	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {

		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ("dcf");
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();
		SOAPElement soapElem = soapBody.addChildElement("GetFile", "dcf");

		// add resource id
		SOAPElement arg = soapElem.addChildElement("trxResourceId");
		arg.setTextContent(resourceId);

		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}
}
