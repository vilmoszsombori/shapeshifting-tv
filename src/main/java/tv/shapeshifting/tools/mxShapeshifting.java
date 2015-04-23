package tv.shapeshifting.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import tv.shapeshifting.nsl.ontology.SparqlFileRepository;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.Lock;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxDomUtils;
import com.mxgraph.view.mxGraph;

public class mxShapeshifting {
	private static Logger LOG = Logger.getLogger(mxShapeshifting.class);
	
	private OntModel ontModel = null;	
	private mxGraphModel mxModel;
	private mxGraph graph;
	private mxIGraphLayout layout;

	private Map<String, Map<String, String>> ssNarrativeObjects = new HashMap<String, Map<String, String>>();
	private Map<String, List<String>> ssHierarchy = new HashMap<String, List<String>>();
	
	public mxShapeshifting(mxGraphModel mxModel, OntModel ontModel) {
		this.mxModel = mxModel;
		this.ontModel = ontModel;
		graph = new mxGraph(mxModel);
		graph.setAutoOrigin(true);
		graph.setAutoSizeCells(false);
		layout = new mxHierarchicalLayout(graph);
	}
	
	public void convert() throws IOException {
		String narrativeRoot = null;
		String queryString = SparqlFileRepository.i().get("/tv/shapeshifting/tools/nslExport.query");
		Query query = QueryFactory.create(queryString);
		ontModel.enterCriticalSection(Lock.READ);
		LOG.debug("OntModel size: " + ontModel.size());
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, ontModel);
			ResultSet results = qexec.execSelect();
			try {
				while (results.hasNext()) {
					QuerySolution solution = results.nextSolution();
					Map<String, String> temp = new HashMap<String, String>();
					
					// Work out the type
					RDFNode uri = solution.get("uri");
					Individual individual = ontModel.getIndividual(uri.toString());
					String typeString = individual.getOntClass(true).toString();		
					typeString = typeString.substring(typeString.indexOf('#') + 1);
					temp.put("type", typeString);
						
					// Read the properties
					for ( Iterator<String> i = solution.varNames(); i.hasNext() ; ) {
						String property = i.next();
						if ( solution.contains(property) ) {
							temp.put(property, solution.get(property).toString());																					
						}
					}
					
					temp.remove("uri");
					
					if ( !temp.containsKey("parent") ) {
						narrativeRoot = temp.get("id").toString();
					} else {
						List<String> list = ( ssHierarchy.containsKey(temp.get("parent")) ) ? ssHierarchy.get(temp.get("parent")) : new Vector<String>();
						if ( temp.get("type").equals("Link") ) 
							list.add(temp.get("id")); 
						else 
							list.add(0, temp.get("id")); 
						ssHierarchy.put(temp.get("parent"), list);						
					}
					
					ssNarrativeObjects.put(temp.get("id").toString(), temp);
					
					LOG.debug(typeString + " [" + temp + "]");
				}
			} finally {
				qexec.close();
			}
		} finally {
			ontModel.leaveCriticalSection();
		}					
				
		//mxModel.setCreateIds(true);
		mxModel.setEventsEnabled(true);

		Document doc = mxDomUtils.createDocument();
		buildUi(narrativeRoot, mxModel.getCell("1"), doc.getDocumentElement(), doc);		
	}
	
	private void buildUi(String id, Object mxRoot, Element mxElement, Document doc) {
		Object newObject = null; Element element;
		mxModel.beginUpdate();		
		try {
			Map<String, String> no = ssNarrativeObjects.get(id);
			LOG.debug(id + " [" + no + "]");
			// ?x ?y ?source ?destination ?linkCondition ?startItem ?leadingLayer ?selectType ?selectionRule ?editingRule ?terminationCondition ?expression
			element = doc.createElement(no.get("type"));
			element.setAttribute("label", no.get("label"));
			element.setAttribute("id", no.get("id"));
			if ( no.get("type").equals("Link") ) {
				if ( no.containsKey("linkCondition") ) {
					Element child = doc.createElement("LinkCondition");
					Text text = doc.createTextNode("<![CDATA[" + no.get("linkCondition") + "]]>");					
					child.appendChild(text);
					element.appendChild(child);
				}				
				newObject = graph.insertEdge(mxRoot, id, element, mxModel.getCell(no.get("source")), mxModel.getCell(no.get("destination")));

			} else {
				double height = 30., width = 80.;
				String style;
				if ( no.get("type").equals("LayerStructure") ) 
				{
					style = "swimlane;horizontal=0;fillColor=#99ff99";
					if ( no.containsKey("leadingLayer") )
						element.setAttribute("hasLeadingLayer", no.get("leadingLayer"));				
				} 
				else if ( no.get("type").equals("LinkStructure") ) 
				{
					style = "swimlane;horizontal=1;fillColor=#ff9999";
					if ( no.containsKey("startItem") )
						element.setAttribute("hasStartItem", no.get("startItem"));
				}
				else if ( no.get("type").equals("BinStructure") ) 
				{
					style = "swimlane;horizontal=1;fillColor=#9999ff";
					if ( no.containsKey("selectType") )
						element.setAttribute("selectType", no.get("selectType"));
					
				} 
				else if ( no.get("type").equals("ImplicitObject") ) 
				{
					style = "fillColor=#9999ff";
				}
				else if ( no.get("type").equals("DecisionPoint") ) 
				{
					style = "ellipse"; height = width = 16.;
				} 
				else 
				{
					style = "rectangle;fillColor=#999999";
				}
				
				// ?x ?y ?source ?destination ?linkCondition ?startItem ?leadingLayer ?selectType ?selectionRule ?editingRule ?terminationCondition ?expression
				
				if ( no.containsKey("selectionRule") ) {
					Element child = doc.createElement("selectionRule");
					Text text = doc.createTextNode("<![CDATA[" + no.get("selectionRule") + "]]>");					
					child.appendChild(text);
					element.appendChild(child);
				}
				if ( no.containsKey("editingRule") ) {
					Element child = doc.createElement("editingRule");
					Text text = doc.createTextNode("<![CDATA[" + no.get("editingRule") + "]]>");					
					child.appendChild(text);
					element.appendChild(child);
				}				
				if ( no.containsKey("terminationCondition") ) {
					Element child = doc.createElement("terminationCondition");
					Text text = doc.createTextNode("<![CDATA[" + no.get("terminationCondition") + "]]>");					
					child.appendChild(text);
					element.appendChild(child);
				}				
				if ( no.containsKey("expression") ) {
					Element child = doc.createElement("expression");
					Text text = doc.createTextNode("<![CDATA[" + no.get("expression") + "]]>");					
					child.appendChild(text);
					element.appendChild(child);
				}
				
				
				double x = no.containsKey("x") ? Double.parseDouble(no.get("x")) : 20.; 
				double y = no.containsKey("y") ? Double.parseDouble(no.get("y")) : 20.; 
				
				newObject = graph.insertVertex(mxRoot, id, element, x, y, width, height, style);
				graph.updateCellSize(newObject, false);
				//mxModel.setCollapsed(mxModel.getCell(id), true);
			}
			//mxElement.appendChild(element);
		} finally {
			mxModel.endUpdate();								
		}
		
		if ( ssHierarchy.containsKey(id) ) {
			for ( Iterator<String> children = ssHierarchy.get(id).iterator() ; children.hasNext() ; ) {
				String childId = children.next();
				buildUi(childId, newObject, element, doc);

				// execute the layout
				mxModel.beginUpdate();
				try {
					layout.execute(mxModel.getCell(id));						
				} finally {
					mxModel.endUpdate();
				}
				
			}
			
		}
	}
	
}
