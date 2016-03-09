package edu.isi.bmkeg.elasticNlm.uima.reader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.uimafit.component.JCasCollectionReader_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.ConfigurationParameterFactory;

import com.google.common.collect.ImmutableMap;

import bioc.type.UimaBioCAnnotation;
import bioc.type.UimaBioCDocument;
import bioc.type.UimaBioCLocation;
import bioc.type.UimaBioCPassage;
import edu.isi.bmkeg.digitalLibrary.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.esViews.ArticleCitation.ArticleCitation__ArticleCitation;
import edu.isi.bmkeg.elasticNlm.repos.ArticleCitationRepository;
import edu.isi.bmkeg.uimaBioC.UimaBioCUtils;
import edu.isi.bmkeg.utils.ViewConverter;

public class LiteratureCitationElasticSearchReader extends JCasCollectionReader_ImplBase {
	
	private static Logger logger = Logger.getLogger(LiteratureCitationElasticSearchReader.class);
	
	public static final String PARAM_PMID_FILE = ConfigurationParameterFactory
			.createConfigurationParameterName(
					LiteratureCitationElasticSearchReader.class, "pmidFilePath");
	@ConfigurationParameter(mandatory = false, description = "Path to list to of PMID entries")
	protected String pmidFilePath;
	private List<String> pmids = new ArrayList<String>();
	
	public static final String PARAM_TXT_DIR = ConfigurationParameterFactory
			.createConfigurationParameterName(
					LiteratureCitationElasticSearchReader.class, "txtDirStr");
	@ConfigurationParameter(mandatory = false, description = "Path to directory for local copy of abstracts")
	protected String txtDirStr;
	private File txtDir;
	
	public final static String LITCIT_ES_REPO = "articleCitationRepository";
	
	@org.uimafit.descriptor.ExternalResource(key = LITCIT_ES_REPO)
	ArticleCitationRepository litCitRepo;
	
	private Iterator<ArticleCitation__ArticleCitation> biocDocIt;
	private Iterator<String> pmidIt;
	
	private long pos = 0;
	private long count = 0;
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {

		try {
			
			if( pmidFilePath != null ) { 
				File pmidFile = new File(this.pmidFilePath);
				if( pmidFile.exists() ) {
					for(String pmid : FileUtils.readFileToString(pmidFile).split("\\n") ) {
						pmids.add(pmid);
					}
					this.count = this.pmids.size();
					this.pmidIt = this.pmids.iterator();
				} else {
					throw new Exception(this.pmidFilePath + " not found");
				}
				
			} else {
				this.count = this.litCitRepo.count();
				biocDocIt = this.litCitRepo.findAll().iterator();
			}
			
			if(txtDirStr != null && txtDirStr.length() > 0) {
				txtDir = new File(this.txtDirStr);
				if( !txtDir.exists() )
					txtDir.mkdirs();
			}
			
		} catch (Exception e) {

			e.printStackTrace();
			throw new ResourceInitializationException(e);

		}

	}

	/**
	 * @see com.ibm.uima.collection.CollectionReader#getNext(com.ibm.uima.cas.CAS)
	 */
	public void getNext(JCas jcas) throws IOException, CollectionException {

		try {
			
			ArticleCitation__ArticleCitation acView = null;
			if( pmidFilePath == null ) {

				if(!biocDocIt.hasNext()) 
					return;
				acView = biocDocIt.next();
			
			} else {
			
				String pmid = pmidIt.next();
				List<ArticleCitation__ArticleCitation> acViews = this.litCitRepo.findByPmid(pmid);
				if( acViews.size() == 1 ) 
					acView = acViews.get(0);
				else 
					return;
				
			}
			
 			ArticleCitation ac = new ArticleCitation();
			ViewConverter vc = new ViewConverter(acView);
			ac = (ArticleCitation) vc.viewObjectToBase(acView, ac);

			//
			// For these documents, we only want to consider the titles and abstracts. 
			//
			String txt = ac.getTitle() + "\n";
			txt += ac.getAbstractText();
				
			jcas.setDocumentText( txt );

			if( txtDir != null ) {
				File txtFile = new File( this.txtDir + "/" + ac.getPmid() + "_abstract.txt");
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new FileWriter(txtFile, true)));
				out.write(txt);
				out.close();
			}
			
			//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			UimaBioCDocument uiD = new UimaBioCDocument(jcas);
			
			uiD.setId(ac.getPmid() + "");
			Map<String,String> infons = new HashMap<String,String>();
			uiD.setBegin(0);
			uiD.setEnd(txt.length());
						
			int passageCount = 0;
			int nSkip = 0;
			
			//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Note that in these systems, we will create a single passage of the
			// entire document and then create general annotations for formatting 
			// on top of that, other sections such as introduction, abstract, etc.
			// will be placed into other passages but no annotations directly 
			// placed on them except for purposes of delineating the sections. 
			//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			UimaBioCPassage uiP = new UimaBioCPassage(jcas);
			int annotationCount = 0;
			uiP.setBegin(0);
			uiP.setEnd(txt.length());
			uiP.setOffset(0);
			passageCount++;
			
			infons = new HashMap<String, String>();
			infons.put("type", "document");
			uiP.setInfons(UimaBioCUtils.convertInfons(infons, jcas));
			uiP.addToIndexes();

			Map<String, String> infons2 = ImmutableMap.of(
			        "type", "formatting",
			        "value", "body"
			);
			UimaBioCAnnotation uiA = this.createBioCAnnotation(
					jcas, ac.getTitle().length() + 1, txt.length(), infons2);
			uiA.addToIndexes();
			
			Map<String, String> infons3 = ImmutableMap.of(
			        "type", "formatting",
			        "value", "title"
			);
			uiA = this.createBioCAnnotation(
					jcas, 0, ac.getTitle().length(), infons3 );
			uiA.addToIndexes();
		
			uiD.addToIndexes();
			
			pos++;
		    if( (pos % 1000) == 0) {
		    	System.out.println("Processing " + pos + "th document.");
		    }
		    
		} catch (Exception e) {
			
			System.err.print(this.pos + "/" + this.count);
			throw new CollectionException(e);

		}
			
	}
	
	private UimaBioCAnnotation createBioCAnnotation(JCas jcas, int begin, int end, Map<String,String> infons) {

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		UimaBioCAnnotation uiA = new UimaBioCAnnotation(jcas);
		uiA.setBegin(begin);
		uiA.setEnd(end);
		uiA.setInfons(UimaBioCUtils.convertInfons(infons, jcas));
		uiA.addToIndexes();
				
		FSArray locations = new FSArray(jcas, 1);
		uiA.setLocations(locations);
		UimaBioCLocation uiL = new UimaBioCLocation(jcas);
		locations.set(0, uiL);
		uiL.setOffset(begin);
		uiL.setLength(end - begin);
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		return uiA;
	}

		
	protected void error(String message) {
		logger.error(message);
	}

	@SuppressWarnings("unused")
	protected void warn(String message) {
		logger.warn(message);
	}

	@SuppressWarnings("unused")
	protected void debug(String message) {
		logger.error(message);
	}

	public Progress[] getProgress() {		
		Progress progress = new ProgressImpl(
				(int) this.pos, 
				(int) this.count, 
				Progress.ENTITIES);
		
        return new Progress[] { progress };
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		
		if( pmidFilePath == null  ) 
			return biocDocIt.hasNext(); 
		else 
			return pmidIt.hasNext();
	}

}
