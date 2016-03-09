package edu.isi.bmkeg.elasticNlm.bin;

import java.io.File;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.bmkeg.elasticNlm.PubMedESIndex;

/**
 * This script loads a file of PMC or PMID values and permits you to 
 * convert to the other. This requ
 * ires that Elastic Search be running
 * on the server. 
 * 
 * @author Gully
 * 
 */
public class BuildPmidPmcidIndex {

	public static class Options {

		@Option(name = "-pubmedFiles", 
				usage = "Local directory where files from PubMed can be stored",
				required = true, metaVar = "PUBMED_FILES")
		public File pubmedFiles;

		@Option(name = "-clusterName", 
				usage = "Name used for the Elastic Search Cluster",
				required = true, metaVar = "CLUSTER_NAME")
		public String clusterName;
		
	}

	private static final Logger logger = LoggerFactory.getLogger(BuildPmidPmcidIndex.class);
	

	/**0
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

		PubMedESIndex pmES = new PubMedESIndex(options.pubmedFiles, options.clusterName);
				
	}

}
