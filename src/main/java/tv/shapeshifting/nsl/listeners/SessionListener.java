package tv.shapeshifting.nsl.listeners;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.NslInterpreter;
import tv.shapeshifting.nsl.OntologyInterface;
import tv.shapeshifting.nsl.Settings;

public class SessionListener implements HttpSessionListener {
	private static Logger LOG = Logger.getLogger(SessionListener.class);
	private int sessionCount = 0;

	@Override
	public void sessionCreated(HttpSessionEvent event) {
        synchronized (this) {
            sessionCount++;
        } 
        LOG.debug("Session [" + event.getSession().getId() + "] has been created. Total number of sessions: " + sessionCount + ".");
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
        synchronized (this) {
            sessionCount--;
        }
        HttpSession session = event.getSession();
		if ( session.getAttribute(Settings.INTERPRETER) != null ) {
			NslInterpreter i = (NslInterpreter)session.getAttribute(Settings.INTERPRETER);
			i.close();
		}
		if ( session.getAttribute(Settings.ONTOLOGY) != null ) {
			OntologyInterface ow = (OntologyInterface)session.getAttribute(Settings.ONTOLOGY);
			ow.closeModels();
		}
		session.removeAttribute(Settings.INTERPRETER);
		session.removeAttribute(Settings.ONTOLOGY);
		session.removeAttribute(Settings.SESSIONID);
		session.removeAttribute(Settings.INTERACTION);
        LOG.debug("Session [" + event.getSession().getId() + "] has been destroyed. Total number of sessions: " + sessionCount + ".");		
	}

}
