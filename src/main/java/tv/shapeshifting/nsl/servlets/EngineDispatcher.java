package tv.shapeshifting.nsl.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.NslInterpreter;
import tv.shapeshifting.nsl.OntologyInterface;
import tv.shapeshifting.nsl.Settings;
import tv.shapeshifting.nsl.ontology.Wrapper;

import com.google.gson.Gson;

/**
 * @author Vilmos
 * 
 */
@WebServlet(displayName="Engine dispatcher", urlPatterns = {"/engine"})
public class EngineDispatcher extends HttpServlet {

	private static final long serialVersionUID = -5952397228430071401L;
	private static Logger LOG = Logger.getLogger(EngineDispatcher.class);
	private static final String ERROR = "/error";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		// create JSON response map
		Map<String, Object> jsonResp = new HashMap<String, Object>();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		try {
			HttpSession session = request.getSession(true);			
			String command = request.getParameter("command");			
			if(command != null && command.toLowerCase().contains("start")) {
				if(command.toLowerCase().equals("restart") && session != null) {
					LOG.debug("Invalidating session [" + session.getId() + "] ...");
					session.invalidate();
					session = request.getSession(true);
				}
				String[] owls = request.getParameterValues("owl");
				String[] rules = request.getParameterValues("rules");				
				OntologyInterface ow = new Wrapper(owls, rules);								
				NslInterpreter i = new NslInterpreter(ow);
				session.setAttribute(Settings.INTERPRETER, i);
				session.setAttribute(Settings.ONTOLOGY, ow);
				session.setAttribute(Settings.SESSIONID, session.getId());
				LOG.debug("Engine started for session [" + session.getId() + "].");
			} else if(command != null && command.toLowerCase().contains("stop")) {
				LOG.debug("Shutting down session [" + session.getId() +"] ...");
				session.invalidate();
				session = request.getSession(true);
			}
									
			// put the session ID in the response			
			LOG.debug("Session [" + session.getId() + "]");
			jsonResp.put("sessionid", session.getId());

			// put the interpreter status in the response
			if ( session.getAttribute(Settings.INTERPRETER) != null ) {
				NslInterpreter i = (NslInterpreter)session.getAttribute(Settings.INTERPRETER);
				jsonResp.put("interpreter", i.getState());								
				jsonResp.put("version", i.version());								
			}			
			// put the OWL status in the response
			if ( session.getAttribute(Settings.ONTOLOGY) != null ) {
				OntologyInterface ow = (OntologyInterface)session.getAttribute(Settings.ONTOLOGY);
				jsonResp.put("owl", ow.getInfModel().size());								
			}			
		} catch ( Exception e ) {
			jsonResp.put("error", e.getMessage());
			request.setAttribute ("javax.servlet.jsp.jspException", e);
			e.printStackTrace();
		} finally {
			// return the JSON 
			response.getWriter().write(new Gson().toJson(jsonResp));			
		}
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}
	
	private void error(HttpServletRequest request, HttpServletResponse response) {
		RequestDispatcher rd = getServletContext().getRequestDispatcher(ERROR);
		try {
			rd.forward(request, response);
		} catch (Exception e) {
			try {
				e.printStackTrace(response.getWriter());
			} catch (IOException e1) {
				e1.printStackTrace();
			}									
			e.printStackTrace();
		}
	}
	
	public void error(HttpServletRequest request, HttpServletResponse response, Throwable exception) {
		request.setAttribute ("javax.servlet.jsp.jspException", exception);
		error(request, response);
	}	
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	/*
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		response.setContentType("text/html");
		HttpSession session = request.getSession(false);	
		String id = (session != null) ? session.getId() : "no session initiated.";
		String owl = request.getParameter("owl");
		LOG.debug("Session ID: " + id);
		LOG.debug("OWL: " + (owl != null ? owl : "no narrative set."));		
		String command = request.getParameter("command");
		response.getWriter().write("Starting engine...");
		if(command != null && command.toLowerCase().contains("start")) {
			if(command.toLowerCase().equals("restart") && session != null) {
				LOG.debug("Invalidating session [" + id + "]");
				session.invalidate();				
			}
			if(owl != null) {
				session = request.getSession(true);	
				id = session.getId();
				LOG.debug("Starting engine [" + id + "]...");
				try {
					OntologyWrapper ow = new OntologyWrapper(owl);								
					NslInterpreter i = new NslInterpreter(ow);
					session.setAttribute(Settings.INTERPRETER, i);
					session.setAttribute(Settings.ONTOLOGY, ow);
					session.setAttribute(Settings.SESSIONID, id);
					LOG.debug("Engine started [" + id + "]");
				} catch (Exception e) {
					error(request, response, e);
				}
			}
		} else if(command != null && command.toLowerCase().equals("stop")) {
			LOG.debug("Shutting down session " + id);
			session.invalidate();
		}
		RequestDispatcher rd = getServletContext().getRequestDispatcher(JSP);
		rd.forward(request, response);			
	}
	*/	
}
