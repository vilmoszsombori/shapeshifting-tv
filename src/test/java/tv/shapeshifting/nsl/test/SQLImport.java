package tv.shapeshifting.nsl.test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

public class SQLImport {
	private static Logger LOG = Logger.getLogger(SQLImport.class);
	private static final String CONNECTION_STRING = "jdbc:mysql://localhost:3306/myvideos_production";
	private static final String USER_NAME = "ta2";
	private static final String PASSWD = "ta2";
	private static final String OUTPUT = "./WebContent/rs/owl/ta2myvideos.content.ttl";
	private static final String SPARQL_PREAMBLE = "PREFIX core: <http://www.ist-nm2.org/ontology/core#>\n"
			+ "PREFIX production: <http://www.ist-nm2.org/ontology/production#>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n";
	private static final String TTL_PREAMBLE = 
			"@prefix : <http://www.ist-nm2.org/ontology/production#> .\n" +
			"@prefix production: <http://www.ist-nm2.org/ontology/production#> .\n" +
			"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
			"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
			"@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n" +
			"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
			"@prefix core: <http://www.ist-nm2.org/ontology/core#> .\n" +
			"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" + 
			"@base <http://www.ist-nm2.org/ontology/production> .\n\n" + 
			"<http://www.ist-nm2.org/ontology/production> rdf:type owl:Ontology .\n\n";

	public static String generateId() {
		UUID uuid = UUID.randomUUID();
		return "ID" + uuid.toString().replaceAll("-", "");
	}
	public static int getMediaType(String type) {
		if (type.toLowerCase().contains("mp4"))
			return 2;
		else if (type.toLowerCase().contains("wav"))
			return 1;
		else
			return 0;
	}

	public static void main(String args[]) {
		SQLImport sqlImport;
		try {
			sqlImport = new SQLImport();
			PrintWriter out = new PrintWriter(SQLImport.OUTPUT);
			out.write(TTL_PREAMBLE);
			sqlImport.importClasses(out);
			sqlImport.importInstances(out);
			sqlImport.importMediaContent(out);
			sqlImport.importMediaItems(out);
			sqlImport.importAnnotations(out);
			// sqlImport.write(OUTPUT);
			sqlImport.cleanup();
			out.flush();
			out.close();
		} catch (ClassNotFoundException e) {
			LOG.error("Cannot load database driver [" + e.getMessage() + "]");
		} catch (SQLException e) {
			LOG.error("SQL error [" + e.getErrorCode() + "]: " + e.getMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	private OntModel model = null;
	private Connection conn = null;
	private Map<Integer, String> classSqlId2owlId = new HashMap<Integer, String>();

	private Map<Integer, String> instanceSqlId2owlId = new HashMap<Integer, String>();

	private Map<Integer, String> contentSqlId2owlId = new HashMap<Integer, String>();

	private Map<Integer, String> itemSqlId2owlId = new HashMap<Integer, String>();

	public SQLImport() throws ClassNotFoundException, SQLException {
		// Set up the MySQL connection
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(SQLImport.CONNECTION_STRING,
				SQLImport.USER_NAME, SQLImport.PASSWD);
		LOG.debug("Database connection established");

		// Set up the ontology model
		OntModelSpec spec = OntModelSpec.OWL_DL_MEM;
		model = ModelFactory.createOntologyModel(spec);
	}

	public void cleanup() {
		if (conn != null) {
			try {
				conn.close();
				LOG.debug("Database connection terminated");
			} catch (SQLException e) {
				LOG.error("Error in terminating database connection ["
						+ e.getErrorCode() + "]: " + e.getMessage());
			}
		}
		if (model != null) {
			model.close();
		}
	}

	public void construct(String constructString, boolean preamble) {
		Model temp = null;
		Query query = QueryFactory.create(preamble ? SPARQL_PREAMBLE
				+ constructString : constructString);
		model.enterCriticalSection(Lock.READ);
		try {
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			temp = qexec.execConstruct();
		} finally {
			model.leaveCriticalSection();
		}
		if (temp != null) {
			model.enterCriticalSection(Lock.WRITE);
			try {
				model.add(temp);
			} finally {
				model.leaveCriticalSection();
			}
		}
	}

	private void importAnnotations(PrintWriter out) {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			Iterator<Integer> i = itemSqlId2owlId.keySet().iterator();
			while (i.hasNext()) {
				int sqlItemId = i.next().intValue();
				// conceptual annotations
				if (stmt.execute(String.format(
						"CALL select_media_instance_annotations(%d, 0);", sqlItemId))) {
					rs = stmt.getResultSet();
					out.println("production:" + itemSqlId2owlId.get(sqlItemId) + " core:contains ");
					int sqlTagInstanceId;
					while (rs.next()) {
						sqlTagInstanceId = rs.getInt("tag_instance_id");
						if (instanceSqlId2owlId.containsKey(sqlTagInstanceId)) {
							out.println("\tproduction:" + instanceSqlId2owlId.get(sqlTagInstanceId) + ( rs.isLast() ? " ." : " ," ));
						} else {
							LOG.warn("The instance id [" + sqlItemId + "] is not recognized");
						}
					}
					rs.close();
					rs = null;
				}
				// TODO temporal annotations
			}
		} catch (SQLException e) {
			LOG.error("SQL error [" + e.getErrorCode() + "]: " + e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error("Error in closing result set [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					LOG.error("Error in closing statement [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
		}
	}

	private void importClasses(PrintWriter out) {
		Statement stmt = null;
		ResultSet rs = null;
		String updateString = "";
		try {
			stmt = conn.createStatement();
			if (stmt.execute("CALL select_tag_classes();")) {
				rs = stmt.getResultSet();
				String name, owlId, owlParent;
				int sqlId, sqlParentId;
				while (rs.next()) {
					sqlId = rs.getInt("id");
					name = rs.getString("name");
					sqlParentId = rs.getInt("parent_id");
					owlParent = (sqlParentId == 0 || sqlParentId == sqlId) ? 
							"core:LogicalEntity" : (classSqlId2owlId.containsKey(sqlParentId) ? 
									"production:" + classSqlId2owlId.get(sqlParentId) : null);
					if (owlParent != null && !name.equals("RIN") && !name.equals("ROUT")) {
						owlId = generateId();
						classSqlId2owlId.put(sqlId, owlId);
						// updateString += "INSERT DATA { production:" + owlId + " a owl:Class ; rdfs:label \"" + name + "\" ; rdfs:subClassOf " + owlParent + " . } ;";
						out.println("production:" + owlId + " rdf:type owl:Class ;\n\t " +
								"rdfs:label \"" + name + "\" ;\n\t " +
								"rdfs:subClassOf " + owlParent + " .\n");
					} else {
						LOG.warn("The parent id [" + sqlParentId + "] of concept [" + name + "] is not recognized");
					}
				}
				rs.close();
				rs = null;
				if (!updateString.isEmpty()) ;
					// update(updateString, true);
			}
		} catch (SQLException e) {
			LOG.error("SQL error [" + e.getErrorCode() + "]: " + e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error("Error in closing result set [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					LOG.error("Error in closing statement [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
		}
	}

	private void importInstances(PrintWriter out) {
		Statement stmt = null;
		ResultSet rs = null;
		String updateString = "";
		try {
			stmt = conn.createStatement();
			Iterator<Integer> i = classSqlId2owlId.keySet().iterator();
			while (i.hasNext()) {
				int sqlClassId = i.next().intValue();
				if (stmt.execute(String.format("CALL select_tag_instances(%d);", sqlClassId))) {
					rs = stmt.getResultSet();
					String name, owlId;
					int sqlId;
					while (rs.next()) {
						sqlId = rs.getInt("id");
						name = rs.getString("instance");
						if (classSqlId2owlId.containsKey(sqlClassId)) {
							owlId = generateId();
							instanceSqlId2owlId.put(sqlId, owlId);
							// updateString += "INSERT DATA { production:" + owlId + " a production:" + classSqlId2owlId.get(sqlClassId) + " ; rdfs:label \"" + name + "\" . } ;";
							out.println("production:" + owlId + " rdf:type production:" + classSqlId2owlId.get(sqlClassId) + " , owl:NamedIndividual ; \n\t" +
									"rdfs:label \"" + name + "\" .\n");
						} else {
							LOG.warn("The class id [" + sqlClassId + "] of instance [" + name + "] is not recognized");
						}
					}
					rs.close();
					rs = null;
					if (!updateString.isEmpty());
						// update(updateString, true);
				}
			}
		} catch (SQLException e) {
			LOG.error("SQL error [" + e.getErrorCode() + "]: " + e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error("Error in closing result set [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					LOG.error("Error in closing statement [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
		}
	}

	private void importMediaContent(PrintWriter out) {
		Statement stmt = null;
		ResultSet rs = null;
		String updateString = "";
		try {
			stmt = conn.createStatement();
			if (stmt.execute("CALL select_media_content();")) {
				rs = stmt.getResultSet();
				String name, owlId, type, url;
				int sqlId;
				while (rs.next()) {
					sqlId = rs.getInt("id");
					name = rs.getString("name");
					type = rs.getString("media_type");
					url = rs.getString("url");
					owlId = generateId();
					contentSqlId2owlId.put(sqlId, owlId);
					// updateString += "INSERT DATA { production:" + owlId + " a core:MediaContent ; rdfs:label \"" + name + "\" ; core:hasFile \"" + url + "\" ; core:hasType " + SQLImport.getMediaType(type) + " . } ;";
					out.println("production:" + owlId	+ " rdf:type core:MediaContent , owl:NamedIndividual ; \n\t" +
							"rdfs:label \"" + name + "\" ;\n\t " +
							"core:hasFile \"" + url + "\" ;\n\t " +
							"core:hasType \""	+ SQLImport.getMediaType(type) + "\" .\n");
				}
				rs.close();
				rs = null;
				if (!updateString.isEmpty());
					// update(updateString, true);
			}
		} catch (SQLException e) {
			LOG.error("SQL error [" + e.getErrorCode() + "]: " + e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error("Error in closing result set [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					LOG.error("Error in closing statement [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
		}
	}

	private void importMediaItems(PrintWriter out) {
		Statement stmt = null;
		ResultSet rs = null;
		String updateString = "";
		try {
			stmt = conn.createStatement();
			if (stmt.execute("CALL select_media_instances(0);")) {
				rs = stmt.getResultSet();
				String name, owlId, tempAnnot;// , type;
				int sqlId, cutIn, cutOut, contentId;
				long rIn, rOut;
				
				while (rs.next()) {
					sqlId = rs.getInt("id");
					name = rs.getString("name");
					// type = rs.getString("media_type");
					contentId = rs.getInt("media_object_id");
					cutIn = rs.getInt("cut_in_milli");
					cutOut = rs.getInt("cut_out_milli");
					rIn = rs.getInt("rin");
					rOut = rs.getInt("rout");
					owlId = generateId();
					itemSqlId2owlId.put(sqlId, owlId);
					tempAnnot = SQLImport.generateId();
					/*
					 * updateString += "INSERT DATA { production:" + tempAnnot + " a core:TemporalAnnotation ; core:type \"core.inOutTime\" ; core:beginTime " + cutIn + " ; core:endTime " + cutOut + " . } ;\n";
					 * updateString += "INSERT DATA { production:" + owlId + " a core:MediaItem ; rdfs:label \"" + name + "\" ; " + "core:hasMediaContent production:" + contentSqlId2owlId.get(contentId) + " ; " + "core:hasTemporalAnnotation production:" + tempAnnot + " . } ;\n";
					 */
					out.println("production:"	+ tempAnnot + " rdf:type core:TemporalAnnotation , owl:NamedIndividual ; \n\t" +
							"core:type \"core.inOutTime\" ;\n\t " +
							"core:beginTime \"" + cutIn + "\" ;\n\t " +
							"core:endTime \"" + cutOut + "\" ;\n\t " +
							"core:name \"\" .\n");
					out.println("production:" + owlId + " rdf:type core:MediaItem , owl:NamedIndividual ; \n\t" +
							"rdfs:label \"" + name + "\" ;\n\t " +
							"core:hasMediaContent production:" + contentSqlId2owlId.get(contentId) + " ;\n\t " +
							"core:hasTemporalAnnotation production:" + tempAnnot + " ;\n\t " +
							"core:hasRelativeIn \"" + rIn + "\"^^xsd:long ;\n\t " +
							"core:hasRelativeOut \"" + rOut + "\"^^xsd:long .\n");
					
				}
				rs.close();
				rs = null;
				if (!updateString.isEmpty());
					// update(updateString, true);
			}
		} catch (SQLException e) {
			LOG.error("SQL error [" + e.getErrorCode() + "]: " + e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					LOG.error("Error in closing result set [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					LOG.error("Error in closing statement [" + e.getErrorCode() + "]: " + e.getMessage());
				}
			}
		}
	}

	public void update(String updateString, boolean preamble) {
		UpdateRequest update = UpdateFactory.create(preamble ? SPARQL_PREAMBLE
				+ updateString : updateString);
		model.enterCriticalSection(Lock.WRITE);
		try {
			UpdateAction.execute(update, model);
		} finally {
			model.leaveCriticalSection();
		}
	}

	public void write(String outputFileName) throws FileNotFoundException {
		// create an output print writer for the results
		PrintWriter writer = new PrintWriter(outputFileName);
		LOG.debug("Writing [" + model.size() + "] RDF triples to [" + outputFileName + "] ...");
		model.write(writer, "TTL");
	}

}
