package edu.isi.bmkeg.elasticNlm;

import java.util.ArrayList;
import java.util.List;

import com.aliasi.medline.MedlineCitation;

import edu.isi.bmkeg.digitalLibrary.ArticleCitation;
import edu.isi.bmkeg.digitalLibrary.Author;
import edu.isi.bmkeg.digitalLibrary.Journal;

public class MedlineBatchHandler {
	
	static ArticleCitation mlCitation2ArticleCitation(MedlineCitation citation) {

		ArticleCitation article = new ArticleCitation();
		article.setAbstractText(citation.article().abstrct().text());
		List<Author> authors = new ArrayList<Author>();
		for (com.aliasi.medline.Author author : citation.article().authorList()
				.authors()) {
			Author p = new Author();
			if (author.name() != null) {
				p.setInitials(author.name().initials());
				p.setSurname(author.name().lastName());
				authors.add(p);
				article.getAuthorList().add(p);
			}
		}
		article.setAuthorList(authors);
		
		article.setPmid(Integer.parseInt(citation.pmid()));
	
		com.aliasi.medline.Journal j = citation.article().journal();
		com.aliasi.medline.JournalInfo jInfo = citation.journalInfo();
		Journal journal = new Journal();
		// if(j!=null && jInfo!=null){
		article.setVolume(j.journalIssue().volume());
		article.setIssue(j.journalIssue().issue());

		journal.setJournalTitle(j.title());
		journal.setAbbr(j.isoAbbreviation());
		article.setJournal(journal);

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
	

}