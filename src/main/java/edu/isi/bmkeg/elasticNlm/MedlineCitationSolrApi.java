package edu.isi.bmkeg.elasticNlm;

import com.aliasi.medline.MedlineCitation;

public class MedlineCitationSolrApi {
	
	private static long sequenceCounter = 0;
	
	private String solrUrl;
	
	protected boolean filterOutNonJournalListRecords;
	
	//private StreamingUpdateSolrServer server;
	
	//protected Collection<SolrInputDocument> batch;
	
	protected int counter;
	
	protected int batchStep;
	
	//protected SolrInputDocument solrOpenAccessDocument;

	private static int globalAddCount;

	private int outofScopeCount;

	public MedlineCitationSolrApi(String url, int batchStep) throws Exception {

		/*outofScopeCount = 0;
		this.filterOutNonJournalListRecords = true;
		globalAddCount = 0;
		this.solrUrl = url;
		this.batchStep = batchStep;
		
		server = new StreamingUpdateSolrServer(url, 10000, 1);
		server.setSoTimeout(10000000); // socket read timeout
		server.setConnectionTimeout(1000000);
		server.setDefaultMaxConnectionsPerHost(100);
		server.setMaxTotalConnections(100);
		
		// defaults to false
		server.setFollowRedirects(false); 
		
		// allowCompression defaults to false.
		// Server side must support gzip or deflate for this to have any
		// effect.
		server.setAllowCompression(true);
		
		// defaults to 0. > 1 not recommended.
		server.setMaxRetries(1);
		
		// binary parser is used by default
		server.setParser(new XMLResponseParser()); 

		batch = new ArrayList<SolrInputDocument>(batchStep);
		counter = 0;*/
		

		
	
	}

	public void add(MedlineCitation doc) {
		
/*		ArticleCitation articleObject = LingpipeMedlineToDigitalLibraryConverter
				.getSimpleArticleModel(doc);
		
		solrOpenAccessDocument = new SolrInputDocument();
		
		//articleObject.setBmkegUniqueId(new Long(++sequenceCounter));
		
		solrOpenAccessDocument = this.toSolrRecord(articleObject);

		if (counter < batchStep) {
			batch.add(solrOpenAccessDocument);
			counter++;
		} else {
			commit();
			batch.add(solrOpenAccessDocument);
			counter = 0;
		}*/
	}

	public void commit()  {
/*		System.out.println("Commiting ..." + batch.size());
		globalAddCount += batch.size();
		server.add(batch.iterator());
		server.commit(true, true);
		batch.clear();*/
	}

	public void close() {
		if (!isBatchEmpty()) {
			commit();
		}
		System.out.println(globalAddCount + " documents added to " + solrUrl);
		System.out.println(outofScopeCount + " skipped");
	}

	private boolean isBatchEmpty() {
		//if (batch.isEmpty())
			return true;
		//return false;
	}

/*	public SolrInputDocument toSolrRecord(ArticleCitation a){
		
		SolrInputDocument solrDoc = new SolrInputDocument();
		
		if (a.getPmid().pmid>0)
		{
			solrDoc.addField("articlePmid", pmid);
		}
		if (startPage>0)
		{
			solrDoc.addField("startPage", startPage);
		}
		if (endPage>0)
		{
			solrDoc.addField("endPage", endPage);
		}
		if (pages!=null)
		{
			solrDoc.addField("pages", pages);
		}

		if (abstractText!=null)
		{
			solrDoc.addField("articleAbstract", abstractText);
		}

		if (issue!=null)
		{
			solrDoc.addField("issue", issue);
		}
		if (volume!=null)
		{
			solrDoc.addField("volume", volume);
		}
		solrDoc = this.journal.toSolrRecord(solrDoc);
		solrDoc = super.toSolrRecord(solrDoc);
		
		return  solrDoc;
	
	}*/
	
}
