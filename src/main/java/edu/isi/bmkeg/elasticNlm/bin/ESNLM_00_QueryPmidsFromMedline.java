package edu.isi.bmkeg.elasticNlm.bin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.bmkeg.utils.pubmed.ESearcher;

public class ESNLM_00_QueryPmidsFromMedline {

	private static final Logger logger = LoggerFactory.getLogger(ESNLM_00_QueryPmidsFromMedline.class);
	
	public static class Options {

		@Option(name = "-query", usage = "Medline query", required = true, metaVar = "QUERY")
		public String queryString;
		
		@Option(name = "-outFile", usage = "Corpus name", required = false, metaVar = "OUTPUT-FILE")
		public File outFile;
			
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();
		
		CmdLineParser parser = new CmdLineParser(options);
	
		try {

			parser.parseArgument(args);

			String queryString = options.queryString;
			
			ESearcher eSearcher = new ESearcher(queryString);
			int maxCount = eSearcher.getMaxCount();
			
			logger.info("esearch max entries: " + maxCount);
			
			List<Integer> esearchIds = new ArrayList<Integer>();
			for(int i=0; i<maxCount; i=i+1000) {
	
				long t = System.currentTimeMillis();
				
				esearchIds.addAll( eSearcher.executeESearch(i, 1000) );
				
				long deltaT = System.currentTimeMillis() - t;
				logger.info("esearch 1000 entries: " + deltaT / 1000.0
						+ " s\n");
				
				logger.info("wait 1 sec");
				Thread.sleep(1000);
			}
	
			PrintWriter out = new PrintWriter(
					new BufferedWriter(
							new FileWriter(
					options.outFile, true)));
			for(Integer pmid : esearchIds) {
				out.println(pmid);
			}
			out.close();
			logger.info("Wrote data to " + options.outFile.getPath());
			
		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			System.exit(-1);
		
		} 
		
	}

}
