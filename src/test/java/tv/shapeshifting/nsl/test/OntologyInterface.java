package tv.shapeshifting.nsl.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import tv.shapeshifting.nsl.exceptions.NonexistentVariableRequestException;
import tv.shapeshifting.nsl.exceptions.TimecodeFormatException;
import tv.shapeshifting.nsl.exceptions.UnexpectedNarrativeObjectException;
import tv.shapeshifting.nsl.exceptions.UnrecognizedMediatypeException;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;

public interface OntologyInterface {

	public abstract int applyDynamicRules(boolean transitive)
			throws MalformedURLException;

	public abstract InfModel getInfModel();

	public abstract OntModel getOntModel();

	public abstract OntModel getRawModel();

	public abstract void closeModels();

	public abstract void addRules(String url, boolean transitive)
			throws MalformedURLException;

	public abstract String logQuery(String queryString, Model model);

	public abstract void logQuery(String queryFile, Model model,
			Individual individual) throws FileNotFoundException, IOException;

	public abstract String update(String updateString, Model model);

	public abstract String construct(String constructString, Model model);

	public abstract boolean isStructured(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract boolean isAtomic(Individual individual);

	public abstract boolean isMediaObject(Individual individual);

	public abstract boolean isImplicitObject(Individual individual);

	public abstract boolean isLinkStructure(Individual individual);

	public abstract boolean isBinStructure(Individual individual);

	public abstract boolean isLayerStructure(Individual individual);

	public abstract boolean isDecisionPoint(Individual individual);

	public abstract boolean isLink(Individual individual);

	public abstract Individual[] getNarrativeRoot()
			throws FileNotFoundException, IOException;

	public abstract Individual[] getNarrativeItemsOf(Individual individual)
			throws FileNotFoundException, IOException,
			UnexpectedNarrativeObjectException;

	public abstract Individual getStartItemOf(Individual individual)
			throws UnexpectedNarrativeObjectException;

	public abstract Individual[] getNextLinkStructureItem(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract Individual getLeadingLayerOf(Individual individual)
			throws UnexpectedNarrativeObjectException;

	public abstract void getTextualAnnotations(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract void getLogicalEntityAnnotations(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract Individual getMediaObject(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract HashMap<String, Object> getPlaylistEntry(
			Individual individual) throws FileNotFoundException, IOException,
			TimecodeFormatException, UnrecognizedMediatypeException;

	public abstract Vector<HashMap<String, Object>> getInteractions(
			Individual individual) throws FileNotFoundException, IOException,
			TimecodeFormatException;

	public abstract void updateStructuredObjectTiming(Individual parent,
			Individual individual) throws IOException;

	public abstract boolean hasPlaylistBarrier(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract boolean getCodeAnnotations(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract boolean isContextVariableDefined(String label)
			throws FileNotFoundException, IOException;

	public abstract void defineTypedContextVariable(String label, Object value,
			String xsdType);

	public abstract void defineUntypedContextVariable(String label, Object value);

	public abstract void setTypedContextVariable(String label, Object value,
			String xsdType) throws FileNotFoundException, IOException;

	public abstract void setUntypedContextVariable(String label, Object value)
			throws FileNotFoundException, IOException;

	public abstract Object getContextVariableValue(String label)
			throws FileNotFoundException, IOException,
			NonexistentVariableRequestException;

	public abstract boolean preprocessContextVariables()
			throws FileNotFoundException, IOException;

	public abstract void setBeingProcessed(Individual individual, boolean on)
			throws FileNotFoundException, IOException;

	public abstract boolean isBeingProcessed(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract void setHasBeenProcessed(Individual individual, boolean on)
			throws FileNotFoundException, IOException;

	public abstract void updateDuration(Individual individual, long duration)
			throws FileNotFoundException, IOException;

	public abstract boolean hasBeenProcessed(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract boolean preprocessImplicitObjects(/*Individual individual*/)
			throws FileNotFoundException, IOException;

	public abstract boolean implicitGroupContent(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract boolean preprocessMediaObjects()
			throws FileNotFoundException, IOException;

	public abstract boolean deleteUITriplets() throws FileNotFoundException,
			IOException;

	public abstract Map<String, Double> getContextVariables(Model model)
			throws FileNotFoundException, IOException;

	public abstract boolean hasSideEffect(Individual individual);

	public abstract void evaluateSideEffect(Individual individual);

	public abstract boolean evaluateTerminationCondition(Individual individual)
			throws FileNotFoundException, IOException;

	public abstract Individual[] getBinItems(Individual individual)
			throws FileNotFoundException, IOException;
	
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

}