package tv.shapeshifting.nsl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;

import tv.shapeshifting.nsl.exceptions.TimecodeFormatException;
import tv.shapeshifting.nsl.exceptions.UnexpectedNarrativeObjectException;
import tv.shapeshifting.nsl.exceptions.UnrecognizedMediatypeException;

import com.hp.hpl.jena.ontology.Individual;

public class NslInterpreterThread implements Runnable {
	private static Logger LOG = Logger.getLogger(NslInterpreterThread.class);
	private String name;
	private OntologyInterface ontology;
	private Individual individual;
	private Playlist playlist;
	private Barriers barriers;
	
	// structured narrative objects' housekeeping
	private Vector<Map<String, Object>> options = new Vector<Map<String, Object>>();
	private Stack<Individual> structuredObjects = new Stack<Individual>();
	
	public NslInterpreterThread(String name, OntologyInterface ontology, Playlist playlist, Barriers barriers) {
		this.name = name;
		this.ontology = ontology;
		this.playlist = playlist;
		this.barriers = barriers;
		LOG.debug("New interpreter thread: [" + this.name + "]");
	}
	
	public NslInterpreterThread(Individual individual, OntologyInterface ontology, Playlist playlist, Barriers barriers) throws FileNotFoundException, IOException {
		this(individual.getLabel(""), ontology, playlist, barriers);
		this.individual = individual;
	}
	
	@Override
	public void run() {
		try {
			interpret(individual);
			LOG.debug(name + " interpreter thread finished. Parent is being notified...");		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnexpectedNarrativeObjectException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}	
	
	private long interpret(Individual individual) throws FileNotFoundException, IOException, UnexpectedNarrativeObjectException, InterruptedException {
		/*
		ExtendedIterator<?> iterator = individual.listOntClasses(false);
		while ( iterator.hasNext() ) {
			LOG.debug(iterator.next());
		}
		LOG.debug(individual.getURI());
		*/
		
		long start = System.currentTimeMillis();		
		
		long duration = 0;
		
		String typeString = individual.getOntClass(true).toString();		
		typeString = typeString.substring(typeString.indexOf('#') + 1);
		
		// TODO move the ID conversion from here to somewhere more sensible
		String id = individual.getURI();
		id = id.substring(id.indexOf('#') + 1);	
		
		String displayName = (individual.getLabel("") != null && !individual.getLabel("").isEmpty()) ? individual.getLabel("") : id ;
		
		LOG.debug(typeString + " [" + displayName + "] is being processed.");
		ontology.setBeingProcessed(individual);
				
		if ( ontology.isMediaObject(individual) ) {		
			// get the playlist entry
			try {
				HashMap<String, Object> pe = ontology.getPlaylistEntry(individual);
				if (pe != null) {				
					//String[] value = new String[] {pe.get("file").toString(), pe.get("in").toString(), pe.get("out").toString()};
					String key = pe.remove("type").toString();
					if ( !options.isEmpty() ) {
						pe.put("options", options);
						//options.clear();
					}
					long offset = playlist.duration(key).longValue();
					duration = playlist.add(key, pe);
					//LOG.debug("Duration before: " + offset + "; Duration after: " + duration);
					duration -= offset;
					//LOG.debug("Actual duration: " + duration);
					
					// update the begins of parent structured objects
					//LOG.debug("Stack: " + structuredObjects.size());
					while( !structuredObjects.isEmpty() ) {
						Individual parent = structuredObjects.pop();
						LOG.debug(parent.getLabel("") + " startsWith " + individual.getLabel(""));
						ontology.updateStructuredObjectTiming(parent, individual);
					}					
				}
				// TODO get code annotations and queue them up somewhere clever...
			} catch (TimecodeFormatException e) {
				LOG.warn("Media object [" + individual.getLabel("") + "] skipped. " + e.getMessage());
			} catch (UnrecognizedMediatypeException e) {
				LOG.warn("Media object [" + individual.getLabel("") + "] skipped. " + e.getMessage());
			}
		} else if(ontology.isAtomic(individual)) {
			// needed for the startsWith update once a MediaObject is reached recursively
			structuredObjects.push(individual);
			
			Individual mediaobject = ontology.getMediaObject(individual);
			if(mediaobject != null)
				duration = interpret(mediaobject);

		} else if(ontology.isLinkStructure(individual)) {
			// needed for the startsWith update once a MediaObject is reached recursively
			structuredObjects.push(individual);
			
			Individual startItem = ontology.getStartItemOf(individual);
			if(startItem != null) {
				duration += interpret(startItem);
				Individual[] nextItems = ontology.getNextLinkStructureItem(startItem);
				while ( nextItems.length > 0 ) {
					int index = nextItems.length > 1 ? (new Random()).nextInt(nextItems.length) : 0;
					if ( nextItems.length > 1 )
						LOG.warn("Multiple enabled outgoing links from [" + individual.getLabel("") + "]. Index [" + index + "] has been followed.");
					duration += interpret(nextItems[index]);
					nextItems = ontology.getNextLinkStructureItem(nextItems[index]);
				}
			}
		} else if(ontology.isImplicitObject(individual) || ontology.isBinStructure(individual)) {	
			// needed for the startsWith update once a MediaObject is reached recursively
			structuredObjects.push(individual);
			
			OntologyInterface.SelectType selectType = ontology.getSelectType(individual);
			LOG.debug("Select type: " + selectType);
			Individual[] binItems;
			do {
				binItems = ontology.getBinItems(individual);
				if ( binItems.length == 0 )
					break;
				if ( selectType == OntologyInterface.SelectType.SELECT_SEQUENCE ) {
					for ( int index = 0; index < binItems.length ; index++ )
						duration += interpret(binItems[index]);
				} else if  ( selectType == OntologyInterface.SelectType.SELECT_ONE ) {
					int index = 0;
					if ( binItems.length > 1 ) {
						Random random = new Random();
						index = random.nextInt(binItems.length);
						LOG.warn("Selection group [" + individual.getLabel("") + "] returned " + binItems.length + " results. Item at index [" + index + "] has been selected.");
					}
					duration = interpret(binItems[index]);
				} else if ( selectType == OntologyInterface.SelectType.SELECT_ALTERNATIVES && ontology.isImplicitObject(individual) ) {
					// TODO this, unfortunately, is a BIG hack, added to cater for the MyVideos2 "authoring" use-case
					try {
						options = new Vector<Map<String, Object>>();
						for ( int index = 1; index < binItems.length ; index++ ) {
							HashMap<String, Object> pe = ontology.getPlaylistEntry(binItems[index]);
		 					pe.remove("type").toString();
							options.add(pe);
						}
					} catch (TimecodeFormatException e) {
						LOG.warn("Media object [" + individual.getLabel("") + "] skipped. " + e.getMessage());
					} catch (UnrecognizedMediatypeException e) {
						LOG.warn("Media object [" + individual.getLabel("") + "] skipped. " + e.getMessage());
					}
					duration = interpret(binItems[0]);					
				}
			} while (binItems.length > 0 && // TODO Bins stop looping if at any point the result of the selection is empty set. Is this how it should be? 
					ontology.evaluateTerminationCondition(individual));
			
		} else if(ontology.isLayerStructure(individual)) {
			// needed for the startsWith update once a MediaObject is reached recursively
			structuredObjects.push(individual);

			// processing recursively the leading layer exclusively first
			Individual leadingLayer = ontology.getLeadingLayerOf(individual);
			if ( leadingLayer != null ) {
				LOG.debug("Interpreting the leading layer: " + leadingLayer.getLabel(""));
				duration = interpret(leadingLayer);
			}
			
			// then all other layers by launching concurrent threads
			Individual[] kids = ontology.getNarrativeItemsOf(individual);
			String threadName = Thread.currentThread().getName();
			ThreadGroup threadGroup = new ThreadGroup(threadName);
			for ( int i = 0; i < kids.length; i++ ) {
				if ( ! kids[i].equals(leadingLayer) ) {
					if ( kids[i].getLabel("").isEmpty() )
						(new Thread(threadGroup, new NslInterpreterThread(kids[i], ontology, playlist, barriers))).start();
					else
						(new Thread(threadGroup, new NslInterpreterThread(kids[i], ontology, playlist, barriers), kids[i].getLabel(""))).start();
				}
			}

			LOG.debug(kids.length + " threads started for [" + threadGroup.getName() + "]. Wating for them to join ...");

			Thread[] threads = new Thread[kids.length];
			int n = threadGroup.enumerate(threads);
			for(int i = 0; i < n; i++) {
				threads[i].join();
			}

			LOG.debug(n + " threads of [" + threadGroup.getName() + "] joined.");
		} else {
			LOG.warn("Unrecognized item: " + individual.getLabel("") + " [" + typeString + "]" );			
		}
		

		// update the duration of narrative objects
		ontology.updateDuration(individual, duration);			

		// process the interactions
		try {
			Vector<HashMap<String, Object>> interactions = ontology.getInteractions(individual);
			for ( int i = 0; i < interactions.size(); i++ )
				playlist.add("interaction", interactions.get(i));
		} catch (TimecodeFormatException e) {
			LOG.warn("Media object [" + individual.getLabel("") + "] skipped. " + e.getMessage());
		}
		
		//ow.setBeingProcessed(individual, false);		
		// wait on barrier
		if(ontology.hasPlaylistBarrier(individual)) {			
			LOG.debug("Playlist barrier reached. Flushing the playlist fragment...");
			playlist.flushFragment();
			LOG.debug("Waiting for playlist barrier to be lifted...");
			barriers.await();
		}

		ontology.setHasBeenProcessed(individual);

		long finish = System.currentTimeMillis();		
		LOG.debug(typeString + " [" + displayName + "] has been processed in " + (finish - start) + "ms. Duration = " + duration + "ms.");

		return duration;
	}	
}
