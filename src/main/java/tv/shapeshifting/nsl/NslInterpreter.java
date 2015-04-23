package tv.shapeshifting.nsl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.exceptions.InterpreterStateException;
import tv.shapeshifting.nsl.exceptions.UnexpectedNarrativeObjectException;

import com.hp.hpl.jena.ontology.Individual;

public class NslInterpreter {
	public static enum State {INITIALIZED, INTERPRETING, WAITING, FINISHED, UNKNOWN}
	
	private static Logger LOG = Logger.getLogger(NslInterpreter.class);	
	private OntologyInterface ow;
	private State state = State.UNKNOWN;
	private Playlist playlist = new Playlist();
	private Barriers barriers = new Barriers();
	private final ThreadGroup interpreterThreads = new ThreadGroup("RootInterpreterThreadGroup");
		
	public NslInterpreter(OntologyInterface ow) throws FileNotFoundException, IOException {
		LOG.debug(version());
		setState(State.UNKNOWN);
		this.ow = ow;
		preprocess();
		setState(State.INITIALIZED);
	}
		
	public int interpret() throws FileNotFoundException, IOException, UnexpectedNarrativeObjectException, InterruptedException {
		if(getState().equals(State.INITIALIZED)) {
			setState(State.INTERPRETING);			
			//preprocess();

			ow.getOntModel().prepare();
			
			Individual[] root = ow.getNarrativeRoot();
			if(root.length == 0) {
				LOG.warn("Narrative doesn't have a root object defined by the hasNarrativeRoot property!");
			} else if(root.length > 1) {
				LOG.warn("Narrative has multiple root objects defined by the hasNarrativeRoot property!");
			}
			LOG.debug("Narrative root: " + root[0].getURI());
									
			for ( int i = 0; i < root.length; i++ ) 
				if ( root[i].getLabel("").isEmpty() )
					(new Thread(interpreterThreads, new NslInterpreterThread(root[i], ow, playlist, barriers))).start();
				else
					(new Thread(interpreterThreads, new NslInterpreterThread(root[i], ow, playlist, barriers), root[i].getLabel(""))).start();
						
			(new Thread("Monitor thread") {
				@Override
				public void run() {
					Thread[] threads = new Thread[interpreterThreads.activeCount()];
					int n = interpreterThreads.enumerate(threads);
					for(int i = 0; i < n; i++) {
						try {
							threads[i].join();
						} catch (InterruptedException e) {
							LOG.warn("Interpreter threads interrupted: " + e.getMessage());
							//e.printStackTrace();
						}
					}
					LOG.debug(n + " threads of [" + interpreterThreads.getName() + "] joined.");
					try {
						playlist.flushFragment();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						setState(NslInterpreter.State.FINISHED);						
					}
				}					
			}).start();
		} else if(getState().equals(State.INTERPRETING)) {
			setState(State.INTERPRETING);
			LOG.debug("Playlist barrier is being lifted...");
			barriers.signal();
		}
		return 0;
	}
	
	public synchronized State getState() {
		return this.state;		
	}
	
	private synchronized void setState(State state) {
		this.state = state;
		LOG.debug("Interperter state [" + state +"]");
	}
	
	public Map<String, Vector<Object>> getPlaylistFragment() throws InterpreterStateException, InterruptedException {
		if(state.equals(State.INTERPRETING))
			return playlist.getFragment();
		else
			throw new InterpreterStateException("Interpreter is not running.");
	}
	
	public Vector<Map<String, Vector<Object>>> getPlaylistHistory() {
		return playlist.getHistory();
	}
	
	public String version() {		
		try {
			return Settings.i().get("VERSION");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "NSL Inference Engine [no version]";
		} catch (IOException e) {
			e.printStackTrace();
			return "NSL Inference Engine [no version]";
		}
	}
	
	public synchronized void close() {
		LOG.debug("Close called. There are " + interpreterThreads.activeCount() + " interpreter threads active. Calling interrupt...");		
		interpreterThreads.interrupt();
		state.equals(State.FINISHED);
	}
			
	private void preprocess() throws FileNotFoundException, IOException {
		//ow.deleteUITriplets();
		//LOG.debug("Preprocess media objects...");
		//ow.preprocessMediaObjects();
		LOG.debug("Preprocess SPARQL expressions in implicit objects...");
		ow.preprocessSparqlExpressions();
		LOG.debug("Preprocess context variables...");		
		ow.preprocessContextVariables();
		LOG.debug("Apply the dynamic rules...");		
		ow.applyDynamicRules(false);				
	}
}






/*

			// set up the SMIL document
			Element smilRoot = null;		
			if(smil.getDocumentElement() != null) {
				LOG.debug("SMIL not empty!");
				smilRoot = smil.getDocumentElement();
				// TODO smilRoot.removeChild...
			} else {
				LOG.debug("SMIL empty!");
				smilRoot = smil.createElement("smil");
				smil.appendChild(smilRoot);					
			}


	// for the Interpreter Threads
	private Element smilParent;
	private Individual individual;
	private String name = "MainInterpreter";
	private SynchTable synchTable;
	private Document smil;
	private int offset = 0;
	
	private NslInterpreter(Individual individual, Element smilParent, NslInterpreter mainThread) throws FileNotFoundException, IOException {
		this.name = individual.getLabel("");
		LOG.info(name + " initializing...");
		this.ow = mainThread.ow;
		this.playlist = mainThread.playlist;
		this.smil = mainThread.smil;
		this.offset = mainThread.offset;
		this.synchTable = mainThread.synchTable;
		this.smilParent = smilParent;
		this.individual = individual;
	}

	public int interpret(Individual individual, Element smilParent) throws FileNotFoundException, IOException, UnexpectedNarrativeObjectException, InterruptedException {
		String s = individual.getOntClass(true).toString();		
		s = s.substring(s.indexOf('#') + 1);
		ow.setBeingProcessed(individual, true);
		LOG.info(individual.getLabel("") + " [" + s + " (struct = " + ow.isStructured(individual) + ", isBeingProcessed = " + ow.isBeingProcessed(individual) + ")]");
		if(ow.isMediaObject(individual)) {
			HashMap<String, Object> pe = ow.getPlaylistEntry(individual);
			ow.getCodeAnnotations(individual);
			// TODO move the following context call to OntologyWrapper
			//Context.i().hasBeenPlayed.put(individual.getURI(), true);
			//LOG.debug(individual.getURI() + " has been played.");
			if(pe != null && pe.containsKey("file") && pe.containsKey("type") && pe.containsKey("in") && pe.containsKey("out")) {
				String nodeName = "1".equals(pe.get("type")) ? "audio" : ( "2".equals(pe.get("type")) ? "video" : "unknown" );				
				Element node = smil.createElement(nodeName);
				node.setAttribute("src", pe.get("file").toString());
				node.setAttribute("clipBegin", pe.get("in").toString());
				node.setAttribute("clipEnd", pe.get("out").toString());
				smilParent.appendChild(node);
				offset += TimeCode.toMilliseconds(pe.get("out").toString()) - TimeCode.toMilliseconds(pe.get("in").toString());
			}			
		} else if(ow.isAtomic(individual)) {
			Individual mediaobject = ow.getMediaObject(individual);
			if(mediaobject != null) {
				interpret(mediaobject, smilParent);
				//ow.getPlaylistEntry(mediaobject);
				//ow.getCodeAnnotations(mediaobject);
				//ow.getLogicalEntityAnnotations(individual);
			}
		} else if(ow.isLinkStructure(individual)) {
			Element seq = smil.createElement("seq");
			smilParent.appendChild(seq);		
			
			Individual startItem = ow.getStartItemOf(individual);
			if(startItem != null) {
				interpret(startItem, seq);				
				Individual nextItem = ow.getNextLinkStructureItem(startItem);
				while(nextItem != null) {
					interpret(nextItem, seq);
					nextItem = ow.getNextLinkStructureItem(nextItem);
				}
			}
		} else if(ow.isImplicitObject(individual) || ow.isBinStructure(individual)) {
			Element seq = smil.createElement("seq");
			smilParent.appendChild(seq);		
			
			LOG.debug("Group textual annotations:");
			ow.getTextualAnnotations(individual);
			LOG.debug("Implicit group content:");
			ow.implicitGroupContent(individual);
			Individual[] binItems;
			do {
				binItems = ow.getBinItems(individual);
				for(int i=0; i<binItems.length; i++) {
					interpret(binItems[i], seq);
				}
			} while (binItems.length > 0 && 
					ow.evaluateTerminationCondition(individual));
			
		} else if(ow.isLayerStructure(individual)){
			Element par = smil.createElement("par");
			smilParent.appendChild(par);		

			Individual[] kids = ow.getNarrativeItemsOf(individual);
			ThreadGroup threadGroup = new ThreadGroup(individual.getLabel(""));
			for(int i = 0; i < kids.length; i++) {
				(new Thread(threadGroup, new NslInterpreter(kids[i], par, this))).start();
				//interpret(kids[i], par);				
			}

			LOG.debug(kids.length + " threads started for [" + individual.getLabel("") + "]. Wating for them to join ...");

			Thread[] threads = new Thread[kids.length];
			int n = threadGroup.enumerate(threads);
			for(int i = 0; i < n; i++) {
				threads[i].join();
			}

			LOG.debug("All threads joined.");

			while(threadGroup.activeCount() > 0) {
				LOG.debug(threadGroup.activeCount() + " threads still active...");
				lock.wait();
			}				
		} else {
			LOG.warn("Unrecognized item: " + individual.getLabel("") + " [" + s + "]" );			
		}
		ow.setBeingProcessed(individual, false);
		LOG.info(individual.getLabel("") + " [" + s + " (struct = " + ow.isStructured(individual) + ", isBeingProcessed = " + ow.isBeingProcessed(individual) + ")]");
		ow.setHasBeenProcessed(individual, true);
		return 0;
	}
	
	public Document getPlaylist() {
		return smil;
	}

	@Override
	public void run() {
		try {
			interpret(individual, smilParent);
			LOG.debug(name + " interpreter thread finished. Parent is being notified...");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnexpectedNarrativeObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
*/