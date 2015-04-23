/**
 * 
 */
package tv.shapeshifting.nsl.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

/**
 * @author Vilmos
 * 
 */
public class Tfl extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2437299946416316422L;
	private static Logger LOG = Logger.getLogger(Tfl.class);
	private static String TFL_FEED_URL = "http://www.tfl.gov.uk/tfl/syndication/feeds/cycle-hire/livecyclehireupdates.xml";

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		LOG.debug("init...");
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("text/xml");
		try {		  
			StreamSource source = new StreamSource(TFL_FEED_URL);
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);	

			//TransformerFactory instance is used to create Transformer objects. 
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(/* new StreamSource("stocks.xsl") */);
			transformer.transform(source, result);
			
			String xmlString = sw.toString();
			PrintWriter out = response.getWriter();
			out.write(xmlString);
			
		} catch (TransformerConfigurationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (TransformerException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		} catch (IOException e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}				
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
}
