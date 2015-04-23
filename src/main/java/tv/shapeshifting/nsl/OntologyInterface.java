package tv.shapeshifting.nsl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Vector;

import tv.shapeshifting.nsl.exceptions.TimecodeFormatException;
import tv.shapeshifting.nsl.exceptions.UnexpectedNarrativeObjectException;
import tv.shapeshifting.nsl.exceptions.UnrecognizedMediatypeException;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;

public interface OntologyInterface {

	public abstract void addRules(String url, boolean transitive)
			throws MalformedURLException;

	public abstract Individual[] getNarrativeRoot() throws IOException;

	public abstract void preprocessSparqlExpressions() throws IOException;

	public abstract void preprocessContextVariables() throws IOException;

	public abstract int applyDynamicRules(boolean transitive)
			throws MalformedURLException;

	public abstract boolean isAtomic(Individual individual);

	public abstract boolean isMediaObject(Individual individual);

	public abstract boolean isImplicitObject(Individual individual);

	public abstract boolean isLinkStructure(Individual individual);

	public abstract boolean isBinStructure(Individual individual);

	public abstract boolean isLayerStructure(Individual individual);

	public abstract boolean isDecisionPoint(Individual individual);

	public abstract boolean isLink(Individual individual);

	public abstract void setBeingProcessed(Individual individual)
			throws IOException;

	public abstract HashMap<String, Object> getPlaylistEntry(
			Individual individual) throws FileNotFoundException, IOException,
			TimecodeFormatException, UnrecognizedMediatypeException;

	public abstract Vector<HashMap<String, Object>> getInteractions(
			Individual individual) throws FileNotFoundException, IOException,
			TimecodeFormatException;

	public abstract void updateStructuredObjectTiming(Individual parent,
			Individual individual) throws IOException;

	public abstract Individual getMediaObject(Individual individual)
			throws UnexpectedNarrativeObjectException;

	public abstract Individual getStartItemOf(Individual individual)
			throws UnexpectedNarrativeObjectException;

	public abstract Individual[] getNextLinkStructureItem(Individual individual)
			throws IOException;

	public abstract Individual[] getBinItems(Individual individual)
			throws IOException;

	public abstract boolean evaluateTerminationCondition(Individual individual)
			throws IOException;

	public abstract Individual getLeadingLayerOf(Individual individual)
			throws UnexpectedNarrativeObjectException;

	public abstract Individual[] getNarrativeItemsOf(Individual individual)
			throws UnexpectedNarrativeObjectException, IOException;

	public abstract void updateDuration(Individual individual, long duration)
			throws IOException;

	public abstract boolean hasPlaylistBarrier(Individual individual)
			throws IOException;

	public abstract void setHasBeenProcessed(Individual individual)
			throws IOException;

	public abstract void closeModels();

	public abstract InfModel getInfModel();

	public abstract OntModel getOntModel();

	public abstract OntModel getRawModel();

	public abstract void update(String updateString, Model model);

	public abstract long construct(String constructString, Model model);

	public abstract boolean ask(String askString, Model model);

	public abstract String logQuery(String queryString, Model model);

	public abstract boolean isContextVariableDefined(String label)
			throws IOException;

	public abstract long defineUntypedContextVariable(String label, Object value)
			throws IOException;

	public abstract void setUntypedContextVariable(String label, Object value)
			throws IOException;
	
	public static enum SelectType { 
		SELECT_ONE("SelectOne"), SELECT_SEQUENCE("SelectSequence"), SELECT_ALTERNATIVES("SelectAlternatives") ;
		private String value;
		SelectType(String value) {
			this.value = value;
		}
		public String toString() {
			return value;
		}		
	} ;

	public abstract SelectType getSelectType(Individual individual)
			throws UnexpectedNarrativeObjectException;


	public static final String NSL_NAMESPACE = "http://shapeshifting.tv/ontology/nsl#";

}