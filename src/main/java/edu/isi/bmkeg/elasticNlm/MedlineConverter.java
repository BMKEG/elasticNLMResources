package edu.isi.bmkeg.elasticNlm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.InputSource;

import com.aliasi.medline.MedlineCitation;
import com.aliasi.medline.MedlineHandler;
import com.aliasi.medline.MedlineParser;
import com.aliasi.util.Streams;
import com.aliasi.util.Strings;

import edu.isi.bmkeg.digitalLibrary.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.Author;
import edu.isi.bmkeg.digitalLibrary.Journal;
import edu.isi.bmkeg.digitalLibrary.esViews.ArticleCitation.ArticleCitation__ArticleCitation;
import edu.isi.bmkeg.elasticNlm.repos.ArticleCitationRepository;
import edu.isi.bmkeg.elasticNlm.repos.JournalRepository;
import edu.isi.bmkeg.utils.ViewConverter;

public class MedlineConverter implements MedlineHandler {

	private static Logger logger = Logger.getLogger(MedlineConverter.class);
	
	private MedlineParser parser;

	@Autowired
	ArticleCitationRepository acRepo;

	@Autowired
	JournalRepository jRepo;

	// total count of records in the upload directory
	private long recsInDir;
	
	// total count of records in the current upload file 
	private long recsInFile;

	// total count of records committed to the solr store
	private long recsSubmitted;

	// total count of records available in the solr store 
	private long recsInStore;

	private Map<String, File> fLookup = new HashMap<String, File>();
	
	public MedlineConverter(ArticleCitationRepository acRepo) {
		super();
		this.acRepo = acRepo;
		this.parser = new MedlineParser(this, false);
	}
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Callbacks from Lingpipe's MedlineParser
	//
	@Override
	public void delete(String pmid) {
		// we do not support deletion
	}

	@Override
	public void handle(MedlineCitation mlCitation) {

		try {

			this.recsInDir++;
			this.recsInFile++;
			
			ArticleCitation a = this
					.mlCitation2ArticleCitation(mlCitation);
			
			if( a == null ) {
				return;
			}

			this.recsSubmitted++;
			
			ArticleCitation__ArticleCitation aView = 
					new ArticleCitation__ArticleCitation();
			ViewConverter vc = new ViewConverter(aView);
			aView = vc.baseObjectToView(a, aView);
			
			acRepo.index(aView);

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private ArticleCitation mlCitation2ArticleCitation(MedlineCitation citation)
			throws Exception {

		ArticleCitation article = new ArticleCitation();
		article.setAbstractText(citation.article().abstrct().text());
		List<Author> authors = new ArrayList<Author>();
		for (com.aliasi.medline.Author author : citation.article().authorList()
				.authors()) {
			Author aa = new Author();
			if (author.name() != null) {
				aa.setInitials(author.name().initials());
				aa.setSurname(author.name().lastName());
				authors.add(aa);
			}
		}
		article.setAuthorList(authors);

		article.setPmid(Integer.parseInt(citation.pmid()));

		com.aliasi.medline.Journal j = citation.article().journal();
		com.aliasi.medline.JournalInfo jInfo = citation.journalInfo();
		Journal journal = new Journal();

		article.setVolume(j.journalIssue().volume());
		if( article.getVolume() == null || article.getVolume().length() == 0 ) {
			article.setVolume("-");
		}

		article.setIssue(j.journalIssue().issue());
		if( article.getIssue() == null || article.getIssue().length() == 0 ) {
			article.setIssue("-");
		}

		journal.setJournalTitle(j.title());
		journal.setAbbr(j.isoAbbreviation());
		journal.setNlmId(jInfo.nlmUniqueID());

		article.setTitle(citation.article().articleTitle());
		article.setPages(citation.article().pagination());
		if (j.journalIssue().pubDate().year() != null) {
			if (j.journalIssue().pubDate().year().trim().length() > 0) {
				article.setPubYear(Integer.parseInt(j.journalIssue().pubDate()
						.year()));
			}
		}

		article.setJournal(journal);
		return article;
		
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Public facing function calls.
	//
	public void parseArchiveDirectory(File dir)
			throws Exception {

		FileFilter archiveFilesFilter = new FileFilter() {
			public boolean accept(File file) {
				
				if( !file.getName().contains("medline") )
					return false;
				
				String p = file.getAbsolutePath();
				if( p.endsWith(".xml") || p.endsWith(".gz") || p.endsWith(".bz2") )
					return true;
				
				return false;

			}
		};

		File[] list = dir.listFiles(archiveFilesFilter);
		this.recsInDir = 0;
		
		for (int k = 0; k < list.length; k++) {
			File f = list[k];
			
			if( !this.fLookup.containsKey( f.getName() ) ) {
				
				this.parseMedlineFileToList(f);
							
				this.fLookup.put(f.getName(), f);
				
				logger.info(f.getPath() + " - dir=" + this.recsInDir + ", file=" + 
						this.recsInFile + ", rec=" + this.recsSubmitted);

			} else {

 				logger.info(f.getPath() + " file already parsed, skipped.");

			}
			
		}

	}

	public void parseMedlineFileToList(File f) throws Exception {

		this.recsInFile = 0;
		
		if (f.getAbsolutePath().endsWith(".xml")) {

			InputSource inputSource = new InputSource(f.getAbsolutePath());
			System.out.println(inputSource.getPublicId());
			System.out.println(inputSource.getSystemId());
			parser.parse(inputSource);

		} else if (f.getAbsolutePath().endsWith(".gz")) {

			FileInputStream fileIn = null;
			GZIPInputStream gzipIn = null;
			InputStreamReader inReader = null;
			BufferedReader bufReader = null;
			InputSource inSource = null;

			try {

				fileIn = new FileInputStream(f);
				gzipIn = new GZIPInputStream(fileIn);
				inReader = new InputStreamReader(gzipIn, Strings.UTF8);
				bufReader = new BufferedReader(inReader);
				inSource = new InputSource(bufReader);

				parser.parse(inSource);

			} finally {
				Streams.closeReader(bufReader);
				Streams.closeReader(inReader);
				Streams.closeInputStream(gzipIn);
				Streams.closeInputStream(fileIn);
				
				fileIn = null;
				gzipIn = null;
				inReader = null;
				bufReader = null;
				inSource = null;
			}

		} else if (f.getAbsolutePath().endsWith(".bz2")) {

			FileInputStream fileIn = null;
			CBZip2InputStream bzipIn = null;
			InputStreamReader inReader = null;
			BufferedReader bufReader = null;
			InputSource inSource = null;

			try {

				fileIn = new FileInputStream(f);

				// //////////HACK found online to make the CBZip2InputStream
				// read the bz2 file correctly////////////
				fileIn.read();
				fileIn.read();
				// //////////HACK found online to make the CBZip2InputStream
				// read the bz2 file correctly////////////

				bzipIn = new CBZip2InputStream(fileIn);
				inReader = new InputStreamReader(bzipIn, Strings.UTF8);
				bufReader = new BufferedReader(inReader);
				inSource = new InputSource(bufReader);
				inSource.setSystemId(f.toURI().toURL().toString());
				parser.parse(inSource);

			} finally {

				Streams.closeReader(bufReader);
				Streams.closeReader(inReader);
				Streams.closeInputStream(bzipIn);
				Streams.closeInputStream(fileIn);
				
				fileIn = null;
				bzipIn = null;
				inReader = null;
				bufReader = null;
				inSource = null;
				
			}

		} else {

			throw new IllegalArgumentException(
					"file suffix must be either '.xml', '.gz' or '.bz'");

		}
		
	}
	
}
