package edu.isi.bmkeg.elasticNlm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.aliasi.medline.MedlineParser;
import com.aliasi.util.Streams;
import com.aliasi.util.Strings;

public class LoadMedlineDump
{
	public static void main(String[] args) throws IOException, SAXException {
		boolean saveXML = false;
		System.out.println(args[0]+" "+args[1]);

		String citationStoreURL = args[0];
		String inputArchiveFolder = args[1];
		Boolean filterJournals = false;
		
		String mgiJournalList=null;
		if (args.length==3) {
			mgiJournalList = args[2];
		}
		System.out.println(citationStoreURL);
		System.out.println(inputArchiveFolder);
		if(mgiJournalList!=null){
			System.out.print(mgiJournalList);
		}
		
		int batchModeStep = 30000;
		int debugCtr = 10;
		int i=0;
		
		FileFilter archiveFilesFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.getAbsolutePath().endsWith(".xml.gz")&&file.getName().contains("medline");
			}
		};
		File[] list = new File(inputArchiveFolder).listFiles(archiveFilesFilter);
		MedlineParser parser = new MedlineParser(saveXML);
						
		for (int k = 0; k<list.length; k++) {
			File f = list[k];
			if (true)
			{
				i++;
				System.out.println("Processing file=" + f);
				if (f.getAbsolutePath().endsWith(".xml"))
				{
					InputSource inputSource = new InputSource(f.getAbsolutePath());
					System.out.println(inputSource.getPublicId());
					System.out.println(inputSource.getSystemId());
					parser.parse(inputSource);
				} else if (f.getAbsolutePath().endsWith(".gz"))
				{
					FileInputStream fileIn = null;
					GZIPInputStream gzipIn = null;
					InputStreamReader inReader = null;
					BufferedReader bufReader = null;
					InputSource inSource = null;
					try
					{
						fileIn = new FileInputStream(f);
						gzipIn = new GZIPInputStream(fileIn);
						inReader = new InputStreamReader(gzipIn, Strings.UTF8);
						bufReader = new BufferedReader(inReader);
						inSource = new InputSource(bufReader);
						parser.parse(inSource);
					} catch (Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally
					{
						Streams.closeReader(bufReader);
						Streams.closeReader(inReader);
						Streams.closeInputStream(gzipIn);
						Streams.closeInputStream(fileIn);
					}
				} else if (f.getAbsolutePath().endsWith(".bz2"))
				{
					FileInputStream fileIn = null;
					BZip2CompressorInputStream bzipIn = null;
					InputStreamReader inReader = null;
					BufferedReader bufReader = null;
					InputSource inSource = null;
					try
					{
						fileIn = new FileInputStream(f);
						////////////HACK found online to make the CBZip2InputStream read the bz2 file correctly////////////
						fileIn.read();
						fileIn.read();
						////////////HACK found online to make the CBZip2InputStream read the bz2 file correctly////////////
						bzipIn = new BZip2CompressorInputStream(fileIn);
						inReader = new InputStreamReader(bzipIn, Strings.UTF8);
						bufReader = new BufferedReader(inReader);
						inSource = new InputSource(bufReader);
						inSource.setSystemId(f.toURI().toURL().toString());
						parser.parse(inSource);
					} catch (Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally
					{
						Streams.closeReader(bufReader);
						Streams.closeReader(inReader);
						Streams.closeInputStream(bzipIn);
						Streams.closeInputStream(fileIn);
					}
				} else
				{
					throw new IllegalArgumentException("arguments must end with .xml or .gz or bz.");
				}
				
			}
		}
				
	}
	
}