package tv.shapeshifting.nsl.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class InferenceExample {
	/**
	 * This program takes 4 parameters an input file name an output file name an
	 * input file format a reasoning level {RDFS, OWL-DL}
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// validate the program arguments
		if (args.length != 4) {
			System.err.println("Usage: java InferenceExample "
					+ "<input file> <input format> <output file> "
					+ "<none|rdfs|owl>");
			return;
		}

		String inputFileName = args[0];
		String inputFileFormat = args[1];
		String outputFileName = args[2];
		String reasoningLevel = args[3];

		// create an input stream for the input file
		FileInputStream inputStream = null;
		PrintWriter writer = null;
		try {
			inputStream = new FileInputStream(inputFileName);
		} catch (FileNotFoundException e) {
			System.err.println("'" + inputFileName
					+ "' is an invalid input file.");
			return;
		}

		// create an output print writer for the results
		try {
			writer = new PrintWriter(outputFileName);
		} catch (FileNotFoundException e) {
			System.err.println("'" + outputFileName
					+ "' is an invalid output file.");
			return;
		} finally {
			inputStream.close();
		}

		// create the appropriate jena model
		OntModel model = null;
		if ("none".equals(reasoningLevel.toLowerCase())) {
			/*
			 * "none" is jena model with OWL_DL ontologies loaded and no
			 * inference enabled
			 */
			model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		} else if ("rdfs".equals(reasoningLevel.toLowerCase())) {
			/*
			 * "rdfs" is jena model with OWL_DL ontologies loaded and RDFS
			 * inference enabled
			 */
			model = ModelFactory
					.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF);
		} else if ("owl".equals(reasoningLevel.toLowerCase())) {
			/*
			 * "owl" is jena model with OWL_DL ontologies wrapped around a
			 * pellet-based inference model
			 */
			model = ModelFactory
					.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		} else {
			// invalid inference setting
			System.err.println("Invalid inference setting, "
					+ "choose one of <none|rdfs|owl>.");
			writer.close();
			inputStream.close();
			return;
		}

		// load the facts into the model
		model.read(inputStream, null, inputFileFormat);

		// validate the file
		ValidityReport validityReport = model.validate();
		if (validityReport != null && !validityReport.isValid()) {
			Iterator<ValidityReport.Report> i = validityReport.getReports();
			while (i.hasNext()) {
				System.err.println(((ValidityReport.Report) i.next())
						.getDescription());
			}
			writer.close();
			inputStream.close();
			return;
		}
		System.err.println("gotya");

		// Iterate over the individuals, print statements about them
		ExtendedIterator<Individual> iIndividuals = model.listIndividuals();
		while (iIndividuals.hasNext()) {
			System.err.println("gotya - 1");
			Individual i = iIndividuals.next();
			printIndividual(i, writer);
		}
		iIndividuals.close();

		writer.close();
		model.close();
	}

	/**
	 * Print information about the individual
	 * 
	 * @param i
	 *            The individual to output
	 * @param writer
	 *            The writer to which to output
	 */
	public static void printIndividual(Individual i, PrintWriter writer) {
		// print the local name of the individual (to keep it terse)
		writer.println("Individual: " + i.getLocalName());

		// print the statements about this individual
		StmtIterator iProperties = i.listProperties();
		while (iProperties.hasNext()) {
			Statement s = (Statement) iProperties.next();
			writer.println("  " + s.getPredicate().getLocalName() + " : "
					+ s.getObject().toString());
		}
		iProperties.close();
		writer.println();
	}
}
