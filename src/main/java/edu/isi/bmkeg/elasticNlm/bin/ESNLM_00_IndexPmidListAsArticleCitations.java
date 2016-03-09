package edu.isi.bmkeg.elasticNlm.bin;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import edu.isi.bmkeg.digitalLibrary.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.Journal;
import edu.isi.bmkeg.digitalLibrary.esViews.ArticleCitation.ArticleCitation__ArticleCitation;
import edu.isi.bmkeg.elasticNlm.repos.ArticleCitationRepository;
import edu.isi.bmkeg.elasticNlm.repos.JournalRepository;
import edu.isi.bmkeg.utils.ViewConverter;
import edu.isi.bmkeg.utils.pubmed.EFetcher;

/**
 * This script loads a file of PMC or PMID values and permits you to convert to
 * the other. This requires that Elastic Search be running on the server.
 * 
 * @author Gully
 * 
 */

@Component
public class ESNLM_00_IndexPmidListAsArticleCitations {

	public static class Options {

		@Option(name = "-inFile", usage = "Input", required = true, metaVar = "INPUT")
		public File inFile;

	}

	@Autowired
	JournalRepository jRepo;

	@Autowired
	ArticleCitationRepository acRepo;

	private static final Logger logger = LoggerFactory.getLogger(ESNLM_00_IndexPmidListAsArticleCitations.class);
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();

		CmdLineParser parser = new CmdLineParser(options);

		try {

			parser.parseArgument(args);

		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			System.exit(-1);

		}
		
		// Use annotated beans from the specified package
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
				"edu.isi.bmkeg.elasticNlm.bin"
				); 

		ESNLM_00_IndexPmidListAsArticleCitations main = ctx.getBean(ESNLM_00_IndexPmidListAsArticleCitations.class);

		List<String> pmids = Files.readAllLines(Paths.get(options.inFile.getPath()), StandardCharsets.UTF_8);
		List<ArticleCitation> l = new ArrayList<ArticleCitation>();
		Set<Integer> toAdd = new HashSet<Integer>();
		for (String pmidStr : pmids) {
			toAdd.add(new Integer(pmidStr));
		}

		EFetcher f = new EFetcher(toAdd);
		while (f.hasNext()) {
			ArticleCitation a = f.next();

			if (a == null)
				continue;

			if (a.getAuthorList() == null || a.getAuthorList().size() == 0)
				continue;

			if (a.getVolume() == null) {
				a.setVolume("-");
				a.setVolValue(-1);
			}

			if (a.getIssue() == null)
				a.setIssue("-");

			if (a.getPages() == null)
				a.setPages("-");

			ArticleCitation__ArticleCitation aView = 
					new ArticleCitation__ArticleCitation();
			ViewConverter vc = new ViewConverter(aView);
			aView = vc.baseObjectToView(a, aView);
			
			main.acRepo.index(aView);
			
		}

	}

}
