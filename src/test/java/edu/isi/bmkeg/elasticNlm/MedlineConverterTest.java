/** $Id: OoevvExcelEngineTest.java 2628 2011-07-21 01:01:24Z tom $
 * 
 */
package edu.isi.bmkeg.elasticNlm;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author University of Southern California
 * @date $Date: 2011-07-20 18:01:24 -0700 (Wed, 20 Jul 2011) $
 * @version $Revision: 2628 $
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ApplicationConfiguration.class)
public class MedlineConverterTest {
	
	@Autowired
	ApplicationContext ctx;
	
	private MedlineConverter converter;	
	
	File ml001, ml001Dir, uploadDataFile;

	//VPDMfKnowledgeBaseBuilder builder;

	String login, password, dbName;
	
	//JournalLookupPersistentObject jLookupPObj;
	
	@Before
	public void setUp() throws Exception {
        			
		ml001 = ctx.getResource("classpath:edu/isi/bmkeg/elasticNlm/data/medline09n0001.xml.bz2").getFile();
		ml001Dir = ml001.getParentFile();
		uploadDataFile = new File( ml001Dir.getParent() + "uploadData.xml");

		//converter = new MedlineConverter( storeUrl, login, password, 100, uploadDataFile, jLookup);
			
	}

	@After
	public void tearDown() throws Exception {
		
	}

	
	@Test
	public void testClearSolrArchive() throws Exception {
		
		int pauseHere = 0;
		
	}
	
}