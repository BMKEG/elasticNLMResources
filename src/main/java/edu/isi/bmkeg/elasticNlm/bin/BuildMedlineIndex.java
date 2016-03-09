package edu.isi.bmkeg.elasticNlm.bin;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import edu.isi.bmkeg.elasticNlm.MedlineConverter;
import edu.isi.bmkeg.elasticNlm.repos.ArticleCitationRepository;

@Component
public class BuildMedlineIndex {

	private static final Logger logger = LoggerFactory.getLogger(BuildMedlineIndex.class);
	
	@Autowired
	ArticleCitationRepository acRepo;
	
	public static class Options {

		@Option(name = "-medlineDir", usage = "Medline Directory", required = true, metaVar = "BIOC_DIRECTORY")
		public String medlineDir = "";
		
	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();

		CmdLineParser parser = new CmdLineParser(options);

		try {

			parser.parseArgument(args);

		} catch (CmdLineException e) {
			
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);

		} 			
		
		// Use annotated beans from the specified package
		ApplicationContext ctx = new AnnotationConfigApplicationContext(
				"edu.isi.bmkeg.elasticNlm.bin"
				); 
		
		BuildMedlineIndex main = ctx.getBean(BuildMedlineIndex.class);
		
		logger.info("Medline Location: " + options.medlineDir);
		
		Long nCitations = main.acRepo.count();
		
		if (nCitations == 0) {
			MedlineConverter mConverter = new MedlineConverter(main.acRepo);
			mConverter.parseArchiveDirectory(new File(options.medlineDir));
		}
		
	}
	
}
