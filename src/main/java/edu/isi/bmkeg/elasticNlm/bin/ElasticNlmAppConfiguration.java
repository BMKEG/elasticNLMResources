/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.isi.bmkeg.elasticNlm.bin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.xml.sax.SAXException;

import edu.isi.bmkeg.digitalLibrary.Journal;
import edu.isi.bmkeg.elasticNlm.xml.ListOfSerialsHandler;

/**
 * @author Gully Burns
 * 
 * To run with different applicaiton properites, call 
 * set the following flag when running the JVM for this application:
 *  
 *     -DappConfig=file:/Users/Gully/Desktop/application.properties
 *     
 * In the absence of this flag, we'll default to the contained properties file 
 * in the classpath. 
 * 
 */
@Configuration
@EnableElasticsearchRepositories("edu.isi.bmkeg.elasticNlm.repos")
@PropertySource("${appConfig:application.properties}")
@EnableAutoConfiguration
public class ElasticNlmAppConfiguration {

	private static Logger logger = Logger.getLogger(ElasticNlmAppConfiguration.class);
	
	
	@PreDestroy
	public void deleteIndex() {
		
	}

	@PostConstruct
	public void buildAllFilesAndIndices() throws Exception {
		
	}

	 List<Journal> parseJournalXml(File f) 
			 throws ParserConfigurationException, SAXException, 
			 FileNotFoundException, IOException {

		ListOfSerialsHandler handler = new ListOfSerialsHandler();

		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		saxFactory.setValidating(false);
		SAXParser parser = saxFactory.newSAXParser();
		parser.parse(new FileInputStream(f), handler);

		return handler.getJournals();
	}

	void urlToTextFile(URL url, File f) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
		BufferedReader in2 = new BufferedReader(new InputStreamReader(url.openStream()));
		String inputLine2;
		while ((inputLine2 = in2.readLine()) != null)
			out.println(inputLine2);
		in2.close();
		out.close();
	}

	String afterLastSlash(String s) {
		return s.substring(s.lastIndexOf("/") + 1, s.length());
	}

}
