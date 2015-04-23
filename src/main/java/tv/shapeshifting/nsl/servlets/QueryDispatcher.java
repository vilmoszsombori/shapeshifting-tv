package tv.shapeshifting.nsl.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

import tv.shapeshifting.nsl.OntologyInterface;
import tv.shapeshifting.nsl.Settings;

public class QueryDispatcher extends HttpServlet {

	private static final long serialVersionUID = -6068662058303784715L;
	private static Logger LOG = Logger.getLogger(QueryDispatcher.class);
	private static final String JSP = "/query.jsp";
	
	public static enum Command { 
		QUERY("Query"), CONSTRUCT("Construct"), UPDATE("Update") ;
		private String value;
		Command(String value) {
			this.value = value;
		}
		public String toString() {
			return value;
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession();
		String id = session.getId();
		LOG.debug("Session id: " + id);		

		// GET parameters
		String query = request.getParameter("query");
		String model = request.getParameter("model");
		String command = request.getParameter("command");			
		
		if(session.getAttribute(Settings.ONTOLOGY) != null && query != null) {
			request.setAttribute("query", query);

			OntologyInterface ow = (OntologyInterface)session.getAttribute(Settings.ONTOLOGY);			
			Model _model = "Raw".equals(model) ? ow.getRawModel() : ("Ontology".equals(model) ? ow.getOntModel() : ("Inference".equals(model) ? ow.getInfModel() : ow.getRawModel()));
			
			String res = query;
			
			if ( command.equals(Command.QUERY.toString()) ) {
				res = ow.logQuery(query, _model);
			} else if ( command.equals(Command.CONSTRUCT.toString()) ) {
				res = "" + ow.construct(query, _model);
			} else if ( command.equals(Command.UPDATE.toString()) ) {
				ow.update(query, _model);
			}

			request.setAttribute("result", res);
		} else {
			LOG.debug("Ontology not initialized for session id " + id);
		}
		
		RequestDispatcher rd = request.getRequestDispatcher(JSP);
		rd.forward(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		doGet(request, response);
	}

}

