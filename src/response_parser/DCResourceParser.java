package response_parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import data_collection.IDcfCatalogueConfig;
import data_collection.IDcfDCTable;
import data_collection.IDcfDCTableLists;

/**
 * Parser used to extract all the {@link DCTable} from
 * a data collection configuration .xml.
 * @author avonva
 *
 */
public class DCResourceParser<T extends IDcfDCTable> implements AutoCloseable {

	private static final String TABLE_NODE = "dataCollectionTable";
	private static final String TABLE_NAME_NODE = "tableName";
	private static final String CONFIG_NODE = "catalogueConfiguration";
	private static final String DATA_NODE = "dataElementName";
	private static final String CAT_CODE_NODE = "catalogueCode";
	private static final String HIER_CODE_NODE = "hierarchyCode";
	
	private IDcfDCTableLists<T> output;  // output list
	
	private T table;
	private IDcfCatalogueConfig catalogueConfig;
	
	private String currentNode;  // current xml node which is parsed
	private InputStream stream;  // stream parsed
	private XMLEventReader eventReader;
	
	/**
	 * Initialize the parser for data collection resources files
	 * @param file file we want to parse
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	public DCResourceParser(IDcfDCTableLists<T> output, File file) throws FileNotFoundException, XMLStreamException {
		
		this.output = output;
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_COALESCING, true);
		
		stream = new FileInputStream(file);
		eventReader = factory.createXMLEventReader(stream);
	}
	
	/**
	 * Parse the xml resource file
	 * @return list of DCTable created
	 * @throws XMLStreamException
	 */
	public IDcfDCTableLists<T> parse() throws XMLStreamException {
		
		while (eventReader.hasNext()) {
			// read the node
			XMLEvent event = eventReader.nextEvent();

			// actions based on the node type
			switch(event.getEventType()) {

			// if starting xml node
			case XMLStreamConstants.START_ELEMENT:
				start(event);
				break;

			// if looking the xml contents
			case XMLStreamConstants.CHARACTERS:
				parseCharacters(event);
				break;

			// if ending xml node
			case XMLStreamConstants.END_ELEMENT:
				end(event);
				break;
			}
		}
		
		return output;
	}

	/**
	 * A start node was found
	 * @param event
	 */
	private void start(XMLEvent event) {

		StartElement startElement = event.asStartElement();
		
		currentNode = startElement.getName().getLocalPart();

		switch (currentNode) {
		case TABLE_NODE:
			this.table = this.output.create();
			break;
		case CONFIG_NODE:
			this.catalogueConfig = this.output.createConfig(); 
			break;
		default:
			break;
		}
	}
	
	/**
	 * Node value was found
	 * @param event
	 */
	private void parseCharacters(XMLEvent event) {
		
		if (currentNode == null)
			return;
		
		// get the xml node value
		String contents = event.asCharacters().getData();

		if (contents == null)
			return;

		switch (currentNode) {
		case TABLE_NAME_NODE:
			table.setName(contents);
			break;
		case DATA_NODE:
			catalogueConfig.setDataElementName(contents);
			break;
		case CAT_CODE_NODE:
			catalogueConfig.setCatalogueCode(contents);
			break;
		case HIER_CODE_NODE:
			catalogueConfig.setHierarchyCode(contents);
			break;
		default:
			break;
		}
	}

	/**
	 * End of node found
	 * @param event
	 */
	private void end(XMLEvent event) {
		
		// get the xml node
		EndElement endElement = event.asEndElement();
		
		// get the xml node name
		String qName = endElement.getName().getLocalPart();

		switch (qName) {
		case TABLE_NODE:

			// add table to output
			output.add(table);
			table = null;
			break;
			
		case CONFIG_NODE:
			// add the configuration to the table
			table.addConfig(catalogueConfig);
			catalogueConfig = null;
			break;
			
		default:
			break;
		}
		
		currentNode = null;
	}
	
	/**
	 * Close the parser
	 * @throws IOException
	 * @throws XMLStreamException 
	 */
	public void close() throws IOException, XMLStreamException {
		stream.close();
		eventReader.close();
	}
}
