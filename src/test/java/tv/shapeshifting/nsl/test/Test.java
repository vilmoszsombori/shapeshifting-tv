package tv.shapeshifting.nsl.test;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.ontology.SparqlFileRepository;

@WebServlet(displayName="Test", urlPatterns = {"/test"})
public class Test extends HttpServlet {
	private static final long serialVersionUID = -6276643929503834621L;
	private static Logger LOG = Logger.getLogger(Error.class);	
	private static final String JSP = "/error.jsp";
	

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		if ( request.getAttribute("error") != null && !request.getAttribute("error").toString().isEmpty() )
			LOG.error(request.getAttribute("error"));
		String s = SparqlFileRepository.i().get("tv/ShapeShift/nsl/ontology/queries/narrativeRoot.query");
		request.setAttribute("error", s);
		RequestDispatcher rd = getServletContext().getRequestDispatcher(JSP);
		
		rd.forward(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
