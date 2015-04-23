package tv.shapeshifting.nsl.servlets;

import java.io.File;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author Vilmos
 * 
 */
public class Logger extends HttpServlet {

	private static final long serialVersionUID = 4285421051314938311L;
	//private static Logger LOG = Logger.getLogger(Logger.class);	

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String prefix =  getServletContext().getRealPath("/");
		System.setProperty("shapeshifting-tv.rootPath", prefix);
		
    	String logsFolder = System.getProperty("shapeshifting-tv.rootPath") + "logs";
    	
    	//create "download" folder if necessary
    	File directory = new File(logsFolder);
    	if (directory.isDirectory() == false) {
    		directory.mkdir();
    	}		
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("text/html");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
}
