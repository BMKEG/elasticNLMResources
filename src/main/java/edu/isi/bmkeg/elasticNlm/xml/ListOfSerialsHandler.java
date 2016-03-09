package edu.isi.bmkeg.elasticNlm.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import edu.isi.bmkeg.digitalLibrary.Journal;

public class ListOfSerialsHandler extends DefaultHandler {

	Journal journal;

	boolean error = false;

	private List<Journal> journals = new ArrayList<Journal>();
	String currentMatch = "";
	String currentAttribute = "";
	int onePercent;

	int globalPosition = 0;
	int lastPosition = 0;

	ArrayList<Exception> exceptions = new ArrayList<Exception>();

	public void startDocument() {
		journals = new ArrayList<Journal>();
	}

	public void endDocument() {
	}
	
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) {

		this.currentMatch += "." + qName;
		this.currentAttribute = attributes.getValue("IdType");

		if (currentMatch.endsWith(".Serial")) {
			journal = new Journal();
			journals.add(journal);
		}
		
	}

	public void endElement(String uri, String localName, String qName) {
		String c = this.currentMatch;
		this.currentMatch = c.substring(0, c.lastIndexOf("." + qName));
	}

	public void characters(char[] ch, int start, int length) {
		String value = new String(ch, start, length);

		this.lastPosition = start;

		try {

			if (currentMatch.endsWith(".ISSN")) {
				journal.setISSN(value);
			} 
			else if (currentMatch.endsWith(".Title")) {
				journal.setJournalTitle(value);
			}
			else if (currentMatch.endsWith(".ISOAbbreviation")) {
				journal.setAbbr(value);
			}	
			else if (currentMatch.endsWith(".NlmUniqueID")) {
				journal.setNlmId(value);
				journal.setId(value);
			}	

			
		} catch (Exception e) {

			this.exceptions.add(e);

		}

	}
	
	public List<Journal> getJournals() {
		return this.journals;
	}

}
