package edu.isi.bmkeg.elasticNlm.bin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import edu.isi.bmkeg.digitalLibrary.Journal;
import edu.isi.bmkeg.digitalLibrary.esViews.Journal.Journal__Journal;
import edu.isi.bmkeg.elasticNlm.repos.JournalRepository;
import edu.isi.bmkeg.utils.ViewConverter;

@Component
public class BuildJournalIndex {

	private static final Logger logger = LoggerFactory.getLogger(BuildJournalIndex.class);
	
	public static String JOURNAL_LIST = "ftp://ftp.nlm.nih.gov/online/journals/lsi2015.xml";
	
	@Autowired
	JournalRepository jRepo;
	
	@Autowired
	ElasticNlmAppConfiguration config;
	
	public static class Options {

		@Option(name = "-localDirectory", usage = "Directory for local files", required = true, metaVar = "BIOC_DIRECTORY")
		public String localDirectory = "";

	}

	public static void main(String[] args) 
			throws Exception {

		Options options = new Options();

		CmdLineParser parser = new CmdLineParser(options);

		try {

			parser.parseArgument(args);

		} catch (CmdLineException e) {
			
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			throw e;

		} 			
		
		// Use annotated beans from the specified package
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
				"edu.isi.bmkeg.elasticNlm.bin"
				); 

		logger.info("Local File Location: " + options.localDirectory);

		BuildJournalIndex main = ctx.getBean(BuildJournalIndex.class);

		File f = new File(options.localDirectory + "/" + main.config.afterLastSlash(JOURNAL_LIST));
		if (!f.exists()) {
			logger.info("Getting " + JOURNAL_LIST);
			main.config.urlToTextFile(new URL(JOURNAL_LIST), f);
		}

		Long nJournals = main.jRepo.count();
		if (nJournals == 0) {
			logger.info("Building Journal index");
			List<Journal> journals = main.config.parseJournalXml(f);
			for (Journal j : journals) {
				Journal__Journal jView = 
						new Journal__Journal();
				ViewConverter vc = new ViewConverter(jView);
				jView = vc.baseObjectToView(j, jView);
				
				main.jRepo.index(jView);
			}
		}
				
	}
	
	
}
