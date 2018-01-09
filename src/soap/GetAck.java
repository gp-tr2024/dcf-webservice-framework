package soap;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ack.DcfAck;
import ack.DcfAckLog;
import ack.FileState;
import config.Config;
import user.IDcfUser;

/**
 * Get acknowledge request
 * @author avonva
 *
 */
public class GetAck extends SOAPRequest {

	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2/";
	private static final String NAMESPACE = "http://dcf-elect.efsa.europa.eu/";
	private static final String TEST_URL = "https://dcf-01.efsa.test/dcf-dp-ws/elect2/?wsdl";
	
	private String messageId;
	
	private SOAPMessage response;
	
	/**
	 * Get an ack for the chosen message
	 * @param messageId
	 */
	public GetAck(IDcfUser user, String messageId) {
		super(user, NAMESPACE);
		this.messageId = messageId;
	}
	
	/**
	 * Get the ack of the message
	 * @return
	 * @throws SOAPException
	 */
	public DcfAck getAck() throws MySOAPException {
		
		Config config = new Config();
		
		Object response = makeRequest(config.isProductionEnvironment() ? URL : TEST_URL);
		
		if (response == null)
			return null;
		
		return (DcfAck) response;
	}

	/**
	 * Get the attachment of the ack response
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws SOAPException 
	 */
	public Document getAttachment() throws SOAPException, 
		ParserConfigurationException, SAXException, IOException {
		
		if (response == null) {
			System.err.println("Cannot get attachment. Make request first!");
			return null;
		}
		
		return getFirstXmlAttachment(response);
	}
	
	/**
	 * Get raw attachment of the response
	 * @return
	 * @throws SOAPException
	 */
	public InputStream getRawAttachment() throws MySOAPException {
		
		if (response == null) {
			System.err.println("Cannot get attachment. Make request first!");
			return null;
		}
		
		return getFirstRawAttachment(response);
	}
	
	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {
		
		// create the standard structure and get the message
		SOAPMessage request = createTemplateSOAPMessage("dcf");

		SOAPBody soapBody = request.getSOAPPart().getEnvelope().getBody();
		
		SOAPElement soapElem = soapBody.addChildElement("GetAck", "dcf");

		SOAPElement arg = soapElem.addChildElement("messageId");
		arg.setTextContent(this.messageId);

		// save the changes in the message and return it
		request.saveChanges();
		
		return request;
	}

	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {
		
		// get the state from the response
		FileState state = extractState(soapResponse);
		
		if (state == null) {
			System.err.println("No state found for message: " + messageId);
			return null;
		}
		
		DcfAckLog log = null;
		
		// no attachment in these cases
		if (state == FileState.READY) {
			
			log = extractAcklog(soapResponse);
			
			if (log == null) {
				System.err.println("No log found for message: " + messageId);
				return null;
			}
		}
		
		// create the ack object
		DcfAck ack = new DcfAck(state, log);
		
		return ack;
	}
	
	/**
	 * Extract the ack state from the response
	 * @param soapResponse
	 * @return
	 * @throws SOAPException
	 */
	private FileState extractState(SOAPMessage soapResponse) throws SOAPException {
		
		// get state
		NodeList children = soapResponse.getSOAPPart()
				.getEnvelope().getBody().getElementsByTagName("fileState");

		if (children.getLength() == 0)
			return null;

		Node stateNode = children.item(0);

		String stateText = stateNode.getTextContent();

		if (stateText.isEmpty())
			return null;

		// get the state from the response
		FileState state = FileState.fromString(stateText);
		
		return state;
	}
	
	/**
	 * Extract the ack log from the attachment
	 * @param soapResponse
	 * @return
	 * @throws SOAPException
	 */
	private DcfAckLog extractAcklog(SOAPMessage soapResponse) throws SOAPException {
		
		this.response = soapResponse;
		
		Document attachment;
		try {
			
			attachment = getFirstXmlAttachment(soapResponse);
			
			if (attachment == null)
				return null;
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			return null;
		}
		
		// get the ack from the attachment
		DcfAckLog log = new DcfAckLog(attachment);
		log.setRawLog(getFirstRawAttachment(soapResponse));
		
		return log;
	}
}
