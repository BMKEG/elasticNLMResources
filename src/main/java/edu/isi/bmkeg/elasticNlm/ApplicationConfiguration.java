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
package edu.isi.bmkeg.elasticNlm;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.xml.sax.SAXException;

import edu.isi.bmkeg.digitalLibrary.Journal;
import edu.isi.bmkeg.digitalLibrary.esViews.Journal.Journal__Journal;
import edu.isi.bmkeg.elasticNlm.repos.ArticleCitationRepository;
import edu.isi.bmkeg.elasticNlm.repos.JournalRepository;
import edu.isi.bmkeg.elasticNlm.xml.ListOfSerialsHandler;
import edu.isi.bmkeg.utils.ViewConverter;

/**
 * @author Gully Burns
 */
@Configuration
@EnableAutoConfiguration
public class ApplicationConfiguration {

	private static Logger logger = Logger.getLogger(ApplicationConfiguration.class);
	
	@Autowired
	Environment env;
	
	@Autowired
	ElasticsearchOperations operations;

	@Autowired
	JournalRepository jRepo;

	@Autowired
	ArticleCitationRepository acRepo;

	public static String JOURNAL_LIST = "ftp://ftp.nlm.nih.gov/online/journals/lsi2015.xml";

	@PreDestroy
	public void deleteIndex() {
	}

	@PostConstruct
	public void buildAllFilesAndIndices() throws Exception {

		if (env.containsProperty("journal.dir")) {
			
			String jDir = env.getProperty("journal.dir");
			logger.info("Journal Location: " + jDir);
			
			File f = new File(jDir + "/" + afterLastSlash(JOURNAL_LIST));
			if (!f.exists()) {
				logger.info("Getting " + JOURNAL_LIST);
				this.urlToTextFile(new URL(JOURNAL_LIST), f);
			}
	
			Long nJournals = jRepo.count();
			if (nJournals == 0) {
				logger.info("Building Journal index");
				List<Journal> journals = parseJournalXml(f);
				for (Journal j : journals) {
					
					Journal__Journal jView = 
							new Journal__Journal();
					ViewConverter vc = new ViewConverter(jView);
					jView = vc.baseObjectToView(j, jView);
				
					jRepo.index(jView);
				}
				operations.refresh(Journal.class, true);
			}
		}
		
		if (env.containsProperty("medline.dir")) {
			
			String medlineDir = env.getProperty("medline.dir");
			logger.info("Medline Location: " + medlineDir);
			
			Long nCitations = acRepo.count();
			
			if (nCitations == 0) {
				MedlineConverter mConverter = new MedlineConverter(this.acRepo);
				mConverter.parseArchiveDirectory(new File(medlineDir));
			}
			
		}

		if (env.containsProperty("pmc.xml.dir")) {

			String pmcDir = env.getProperty("pmc.xml.dir");
			logger.info("PMC OA Location: " + pmcDir);
			
			Long nCitations = acRepo.count();
		
		}

	
	}

	private List<Journal> parseJournalXml(File f) throws FileNotFoundException, IOException, ClassNotFoundException,
			SAXException, ParserConfigurationException {

		ListOfSerialsHandler handler = new ListOfSerialsHandler();

		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		saxFactory.setValidating(false);
		SAXParser parser = saxFactory.newSAXParser();
		parser.parse(new FileInputStream(f), handler);

		return handler.getJournals();
	}

	private void urlToTextFile(URL url, File f) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
		BufferedReader in2 = new BufferedReader(new InputStreamReader(url.openStream()));
		String inputLine2;
		while ((inputLine2 = in2.readLine()) != null)
			out.println(inputLine2);
		in2.close();
		out.close();
	}

	private String afterLastSlash(String s) {
		return s.substring(s.lastIndexOf("/") + 1, s.length());
	}

}
