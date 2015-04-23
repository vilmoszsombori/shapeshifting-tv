package tv.shapeshifting.nsl.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XSLTransform {
	
	public static void transform(String source, String xslt, String result) throws TransformerException, FileNotFoundException {
		StreamSource streamSource = new StreamSource(source);
		StringWriter sw = new StringWriter();
		StreamResult streamResult = new StreamResult(sw);	

		//TransformerFactory instance is used to create Transformer objects. 
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer(new StreamSource(xslt));
		//Transformer transformer = factory.newTransformer(new StreamSource("./WebContent/rs/xsl/idiap2finalcut_separatefiles.xsl"));
		//Transformer transformer = factory.newTransformer(new StreamSource("./WebContent/rs/xsl/idiapfilter.xsl"));
		transformer.transform(streamSource, streamResult);
		
		String xmlString = sw.toString();
		PrintWriter out = new PrintWriter(result);
		out.write(xmlString);
		out.flush();
		out.close();		
	}

	public static void main(String args[]) {
		try {
			//transform("./WebContent/rs/xml/MyVideos2MaterSongNames.xml", "./WebContent/rs/xsl/markershift.xsl", "./WebContent/rs/xml/markers.xml");
			//transform("./WebContent/rs/xml/MyVideosAlignment.xml", "./WebContent/rs/xsl/idiap2finalcut.xsl", "./WebContent/rs/xml/result.xml");
			//transform("./WebContent/MyVideosAlignment.xml", "./WebContent/rs/xsl/idiap2finalcut_separatefiles.xsl", "./WebContent/result.xml");
			//transform("./WebContent/MyVideosAlignment.xml", "./WebContent/rs/xsl/idiapfilter.xsl", "./WebContent/result.xml");
			transform("./WebContent/rs/xml/MyVideos2.FinalCut.master.xml", "./WebContent/rs/xsl/fc2mediacontent.xsl", "./WebContent/rs/xml/result.ttl");			
		} catch (TransformerConfigurationException e2) {
			e2.printStackTrace();
		} catch (TransformerException e3) {
			e3.printStackTrace();
		} catch (IOException e4) {
			e4.printStackTrace();
		}						
	}
}
