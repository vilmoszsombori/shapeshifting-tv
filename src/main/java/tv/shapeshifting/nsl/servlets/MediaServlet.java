package tv.shapeshifting.nsl.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.OntologyInterface;
import tv.shapeshifting.nsl.Settings;
import tv.shapeshifting.nsl.ontology.SparqlFileRepository;

import com.google.gson.Gson;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.shared.Lock;

/**
 * Servlet implementation class MediaServlet
 */
public class MediaServlet extends HttpServlet {
	private static final long serialVersionUID = -5336099261280076363L;
	private static Logger LOG = Logger.getLogger(MediaServlet.class);

	/**
     * @see HttpServlet#HttpServlet()
     */
    public MediaServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, Object> jsonResp = new HashMap<String, Object>();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");	
		
		// GET parameters
		/*
		String command = request.getParameter("command");			
		String id = request.getParameter("id");
		*/
				
		
		try {
			HttpSession session = request.getSession(false);
			jsonResp.put("sessionid", session.getId());
			OntologyInterface ow = (OntologyInterface)session.getAttribute(Settings.ONTOLOGY);

			String queryString = SparqlFileRepository.i().get("queries/getMediaObjects.query");
			Query query = QueryFactory.create(queryString);
			
			// TODO re-think which model to use --> it should be the media model
			ow.getRawModel().enterCriticalSection(Lock.READ);			
			try {
				QueryExecution qexec = QueryExecutionFactory.create(query, ow.getRawModel());
				ResultSet results = qexec.execSelect();
				try {
					List<Map<String, String>> mediaObjects = new Vector<Map<String, String>>();
					Map<String, String> item;
					String key, value;
					while (results.hasNext()) {
						QuerySolution solution = results.nextSolution();
						item = new HashMap<String, String>();
						for ( Iterator<String> i = solution.varNames() ; i.hasNext() ; ) {							
							key = i.next();
							value = solution.get(key).toString();
							item.put(key, value);
						}
						if ( !item.isEmpty() )
							mediaObjects.add(item);						
					}
					if ( !mediaObjects.isEmpty() )
						jsonResp.put("mediaObjects", mediaObjects);
				} finally {
					qexec.close();
				}
			} finally {
				ow.getRawModel().leaveCriticalSection();
			}		
							
		} catch ( Exception e ) {
			jsonResp.put("error", e.getMessage());
			request.setAttribute ("javax.servlet.jsp.jspException", e);
			LOG.error(e.getMessage());
			e.printStackTrace();
		} finally {
			// return the JSON 
			response.getWriter().write(new Gson().toJson(jsonResp));			
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}		

}
