/**
 * 
 */
package tv.shapeshifting.nsl.servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import tv.shapeshifting.nsl.NslInterpreter;
import tv.shapeshifting.nsl.Settings;

import com.google.gson.Gson;

/**
 * @author Vilmos
 * 
 */
public class PlaylistDispatcher extends HttpServlet {
	private static final long serialVersionUID = 8063912246766278109L;
	private static Logger LOG = Logger.getLogger(PlaylistDispatcher.class);
	private static final String ERROR = "/error";
		
	private void doJson(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, Object> jsonResp = new HashMap<String, Object>();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");		
		try {
			HttpSession session = request.getSession(false);
			jsonResp.put("sessionid", session.getId());
			if ( session.getAttribute(Settings.INTERPRETER) != null ) {
				NslInterpreter i = (NslInterpreter) session.getAttribute(Settings.INTERPRETER);				
				String command = request.getParameter("command");			
				command = (command != null && command.toLowerCase().contains("new")) ? "new" : "old";
				Map<String, Vector<Object>> playlistFragment = null;
				if ( command.equals("old") ) {
					Vector<Map<String, Vector<Object>>> history = i.getPlaylistHistory();
					playlistFragment = history.lastElement();
				} else {				
					i.interpret();
					playlistFragment = i.getPlaylistFragment();
				}
				if ( playlistFragment != null ) {
					jsonResp.putAll(playlistFragment);
					if ( Settings.i().getBoolean("INTERACTION_URL_REWRITE") ) {
						if ( jsonResp.containsKey("interaction") ) {
							String[] keys = Settings.i().getStringArray("INTERACTION_URL_REWRITE_KEY");
							@SuppressWarnings("unchecked")
							Vector<HashMap<String, Object>> inteaction = (Vector<HashMap<String, Object>>)jsonResp.get("interaction");
							String prefix = "http://" + request.getHeader("host") + request.getContextPath();
							for ( int j = 0; j < inteaction.size(); j++ ) 
								for ( int k = 0; k < keys.length; k++ )
									if ( inteaction.get(j).containsKey(keys[k]) )
										inteaction.get(j).put(keys[k], prefix + "/" + inteaction.get(j).get(keys[k]).toString());																				
						}
					}
				}
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
	
	private void doXml(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		HttpSession session = request.getSession(false);
		if ( session == null ) {
			request.setAttribute("error", "No session is initiated!");
			error(request, response);
			return;
		}		
		if ( session.getAttribute(Settings.INTERPRETER) == null ) {
			request.setAttribute("error", "Engine has not been started or the session has expired!");
			error(request, response);
			return;						
		}

		try {
			NslInterpreter i = (NslInterpreter) session.getAttribute(Settings.INTERPRETER);
			String command = request.getParameter("command");			
			command = (command != null && command.toLowerCase().contains("new")) ? "new" : "old";
			String xslt = request.getParameter("xslt");
			Map<String, Vector<Object>> playlistFragment = null;
			if ( command.equals("old") ) {
				Vector<Map<String, Vector<Object>>> history = i.getPlaylistHistory();
				playlistFragment = history.lastElement();
			} else {
				i.interpret();
				playlistFragment = i.getPlaylistFragment();
			}
			if ( playlistFragment != null ) {
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
				Document xml = docBuilder.newDocument();
				
				Element root = xml.createElement("playlist");
				xml.appendChild(root);
										
				for ( Iterator<String> it = playlistFragment.keySet().iterator(); it.hasNext(); ) {
					String tagName = it.next(); 
					Element e = xml.createElement(tagName + "s");
					for ( Iterator<?> j = playlistFragment.get(tagName).iterator(); j.hasNext(); ) {
						Element e1 = xml.createElement(tagName);
						@SuppressWarnings("unchecked")
						Map<String, Object> map = ((Map<String, Object>)j.next());
						for ( Iterator<String> jj = map.keySet().iterator(); jj.hasNext(); ) {
							String name = jj.next();
							Object value = map.get(name);
							if ( value instanceof String )
								e1.setAttribute(name, value.toString());
							else if ( value instanceof Vector ) {
								e1.setAttribute(name, value.toString());								
							}
						}
						e.appendChild(e1);								
					}							
					root.appendChild(e);
				}
					
				StringWriter sw = new StringWriter();
				StreamResult result = new StreamResult(sw);
				DOMSource source = new DOMSource(xml);

				TransformerFactory factory = TransformerFactory.newInstance();
				Transformer transformer = xslt == null ? factory.newTransformer() : factory.newTransformer( new StreamSource(xslt) ) ;
				transformer.transform(source, result);

				String xmlString = sw.toString();
				response.setContentType("text/xml");				
				PrintWriter out = response.getWriter();
				out.write(xmlString);
			}
		} catch ( Exception e ) {
			request.setAttribute("error", e.getMessage());
			error(request, response, e);
		}
	}
		
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// update the server url in the settings file
		try {
			String s = Settings.i().get("SERVER_URL");
			if ( s == null || ( s != null && s.isEmpty() ) ) {
				String url = request.getRequestURL().toString();
				url = url.substring(0, url.lastIndexOf("/"));
				Settings.i().setProperty("SERVER_URL", url);
				LOG.debug("SERVER_URL = " + url);
			}
		} catch (FileNotFoundException e1) {
			LOG.warn("SERVER_URL not set!");			
			e1.printStackTrace();
		} catch (IOException e1) {
			LOG.warn("SERVER_URL not set!");			
			e1.printStackTrace();
		}
		
		String format = (request.getParameter("format") != null) ? request.getParameter("format") : "JSON";
		format = format.toUpperCase();
		
		if (format.equals("XML")) {
			doXml(request, response);
		} else {
			doJson(request, response);
		}
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
		LOG.error(exception.getMessage());
		error(request, response);
		exception.printStackTrace();
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}		
}
