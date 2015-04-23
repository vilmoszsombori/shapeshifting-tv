package tv.shapeshifting.nsl.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.OntologyInterface;
import tv.shapeshifting.nsl.Settings;
import tv.shapeshifting.nsl.ontology.Wrapper;

import com.google.gson.Gson;

/**
 * @author Vilmos
 * 
 */
@WebServlet(displayName="Interaction dispatcher", urlPatterns = {"/interaction"})
public class InteractionDispatcher extends HttpServlet {

	private static final long serialVersionUID = 6530561858169464493L;
	private static Logger LOG = Logger.getLogger(InteractionDispatcher.class);
	private static final String ERROR = "/error";

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
	
	private void error(HttpServletRequest request, HttpServletResponse response, Throwable exception) {
		request.setAttribute ("javax.servlet.jsp.jspException", exception);
		error(request, response);
	}		

	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		HttpSession session = request.getSession(false);
		String id = (session != null) ? session.getId() : "no session initiated.";
		LOG.debug("Session id: " + id);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		if(session != null) {
			// check if the OWL is initialized and insert the value in the
			Map<String, String[]> parameterMap = request.getParameterMap();
			
			if(!parameterMap.isEmpty() && session.getAttribute(Settings.ONTOLOGY) != null) {
				try {
					OntologyInterface ow = (OntologyInterface) session.getAttribute(Settings.ONTOLOGY);
					boolean flag = false;
					for (Iterator<Entry<String, String[]>> it = parameterMap.entrySet().iterator(); it.hasNext(); ) {
						Map.Entry<String, String[]> pair = it.next();
						if(pair.getValue().length == 1) {
							flag = true;
							String canonicalValue = Wrapper.toCanonicalValue(pair.getValue()[0]);
							// Assign as typed context variable 
							if(ow.isContextVariableDefined(pair.getKey()))
								// Check whether it has already been defined
								ow.setUntypedContextVariable(pair.getKey(), canonicalValue);
							else
								ow.defineUntypedContextVariable(pair.getKey(), canonicalValue);
						} else if(pair.getValue().length == 0) {
							LOG.warn("Varaible [" + pair.getKey() + "] not assigned to empty value.");
						} else {
							LOG.warn("Variable [" + pair.getKey() + "] not assigned to list as lists are not supported yet.");						
						}
					}
					//TODO this call may be non-blocking
					if ( flag )
						ow.applyDynamicRules(false);
				} catch ( Exception e ) {
					error(request, response, e);
				}
			
			}
			
			// Keep a local copy in a hash map
			
			HashMap<String, Object> interactions = null;
			
			if(session.getAttribute(Settings.INTERACTION) == null) {
				interactions = new HashMap<String, Object>();
			} else {
				Object sesssionVar = session.getAttribute(Settings.INTERACTION); 
				interactions = (HashMap<String, Object>) sesssionVar;									
			}
			if(interactions != null) {
				interactions.putAll(parameterMap);
				interactions.put("session", id);
				session.setAttribute(Settings.INTERACTION, interactions);
				response.getWriter().write(new Gson().toJson(interactions));
			}
		}		
				    
		/*
		RequestDispatcher rd = getServletContext().getRequestDispatcher(JSP);
		rd.forward(request, response);
		*/			
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
