package org.healthnlp.deepphe.summary.writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.node.NoteNodeCreator;
import org.healthnlp.deepphe.node.PatientCreator;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.node.PatientSummaryXnNodeStore;
import org.healthnlp.deepphe.summary.engine.FactRelationUtil;
import org.healthnlp.deepphe.summary.engine.MultiSummaryEngine;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/30/2021}
 */
public class EvalFileWriterXn extends AbstractFileWriter<Patient> {

   static private final Logger LOGGER = Logger.getLogger( "EvalFileWriterXn" );

   // TODO: Create Cancer and Tumor Node.  Similar to NeoplasmSummary, but contain Fact instead of Mention etc.
   // Refactor Neo4j writer and reader to handle Cancer, Tumor, Fact instead of NeoplasmSummary.  dphe-xn will use.
   // Refactor Neo4j plugin to use Cancer, Tumor, Fact.  dphe-xn will use.  dphe-cr will NOT.
   // Refactor MultiSummaryEngine to create Cancer, Tumor, Fact instead of NeoplasmSummary.  dphe-xn will use.

   // This can keep -cr and -xn separate without too much bother.




   @ConfigurationParameter(
         name = "CancerEvalFile",
         description = "The Path to the File for Cancer Evaluation output.",
         mandatory = true
   )
   private String _cancerEvalPath;

   private Patient _patient;

   private boolean _needHeader;


   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _needHeader = true;
   }


   /**
    * Sets data to be written to the jcas.
    *
    * @param jCas ye olde
    */
   @Override
   protected void createData( final JCas jCas ) {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      _patient = PatientNodeStore.getInstance().getOrCreate( patientId );
      final Note note = NoteNodeCreator.createNote( jCas );
      PatientCreator.addNote( _patient, note );
   }

   /**
    * @return the JCas.
    */
   @Override
   protected Patient getData() {
      return _patient;
   }

   /**
    * called after writing is complete
    *
    * @param data -
    */
   @Override
   protected void writeComplete( final Patient data ) {
   }

   /**
    * Don't use the document / patient for subdirectories
    * {@inheritDoc}
    */
   @Override
   protected String getSubdirectory(JCas jCas, String documentId) {
      return this.getSimpleSubDirectory();
   }

   /**
    * Write information into a file named based upon the document id and located based upon the document id prefix.
    *
    * This will write one file per patient, named after the patient, with each row containing columns of cuis.
    *
    * @param patient       data to be written
    * @param outputDir  output directory
    * @param documentId -- not used --
    * @param fileName   -- not used --
    * @throws IOException if anything goes wrong
    */
   public void writeFile( final Patient patient,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final File cancerFile = new File( outputDir, _cancerEvalPath + "_cancer.bsv" );
      final File tumorFile = new File( outputDir, _cancerEvalPath + "_tumor.bsv" );
      if ( _needHeader ) {
         try ( Writer cancerWriter = new BufferedWriter( new FileWriter( cancerFile ) ) ) {
            cancerWriter.write( CANCER_HEADER );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
         try ( Writer tumorWriter = new BufferedWriter( new FileWriter( tumorFile ) ) ) {
            tumorWriter.write( TUMOR_HEADER );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
         _needHeader = false;
      }
      final String patientId = patient.getId();
      // Even though the ctakes PatientNoteStore isn't being used to store any patient jcas, it still has note counts.
      final int patientDocCount = PatientNoteStore.getInstance()
                                                  .getWantedDocCount( patientId );
      if ( patient.getNotes().size() < patientDocCount ) {
         LOGGER.info( patientId + " " + patient.getNotes().size() + " of " + patientDocCount );
         return;
      }
      // Somebody else may have already created the patient summary.
      PatientSummaryXn patientSummary = PatientSummaryXnNodeStore.getInstance().get( patientId );
      if ( patientSummary == null ) {
         // Create PatientSummary
         patientSummary = MultiSummaryEngine.createPatientSummaryXn( patient );
         if ( patientSummary == null ) {
            LOGGER.warn( "EvalFileWriterXn #143, null PatientSummary from MultiSummaryEngine.  Need to finish." );
            return;
         }
         // Add the summary just in case some other consumer can utilize it.  e.g. eval file writer.
         PatientSummaryXnNodeStore.getInstance().add( patientId, patientSummary );
      }

      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      final String summaryJson = gson.toJson( patientSummary );
      final File file = new File( outputDir, patientId + ".json" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( summaryJson );
      }

      final Map<String,Fact> idFactMap =
            patientSummary.getFacts()
                          .stream()
                          .collect( Collectors.toMap( Fact::getId, Function.identity() ) );
      final List<Cancer> cancers = patientSummary.getCancers();
      try ( Writer cancerWriter = new BufferedWriter( new FileWriter( cancerFile, true ) );
            Writer tumorWriter = new BufferedWriter( new FileWriter( tumorFile, true ) ) ) {
         for ( Cancer cancer : cancers ) {
            cancerWriter.write( createCancerLine( patientId, cancer, idFactMap ) );
            final String cancerId = cancer.getId();
            for ( Tumor tumor : cancer.getTumors() ) {
               tumorWriter.write( createTumorLine( patientId, cancerId, tumor, idFactMap ) );
            }
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private final String CANCER_HEADER = "Patient_ID|"
                                               + "Summary_Type|"
                                               + "Summary_ID|"
                                               + "Summary_URI|"
                                               + "hasCancerType|"
                                               + "hasHistologicType|"
                                               + "hasHistoricity|"
                                               + "hasBodySite|"
                                               + "hasLaterality|"
                                               + "hasCancerStage|"
                                               + "has_Clinical_T|"
                                               + "has_Clinical_N|"
                                               + "has_Clinical_M|"
                                               + "has_Pathologic_T|"
                                               + "has_Pathologic_N|"
                                               + "has_Pathologic_M|"
                                               + "hasMethod|"
                                               + "hasTreatment|"
                                               + "Regimen_Has_Accepted_Use_For_Disease|"
                                               + "Disease_Has_Normal_Tissue_Origin|"
                                               + "Disease_Has_Normal_Cell_Origin|"
                                               + "Disease_Has_Finding|"
                                               + "Disease_May_Have_Finding|"
                                               + "has_PSA_Level|"
                                               + "has_Gleason_Score|\n";



   static private String createCancerLine( final String patientId,
                                           final Cancer cancer,
                                           final Map<String,Fact> idFactMap ) {
      final Collection<NeoplasmAttribute> attributes = cancer.getAttributes();
      final Map<String,List<String>> idRelatedIdMap = cancer.getRelatedFactIds();
      final Map<String,List<Fact>> related = FactRelationUtil.getRelatedFactsMap( idRelatedIdMap, idFactMap );
      return patientId + "|" + "Cancer|" + cancer.getId() + "|" + cancer.getClassUri() + "|"
            + getRelatedUri( related, "hasCancerType" ) + "|"
             + getAttributeValue( attributes, "histology" ) + "|"
             + getRelatedUri( related, "hasHistoricity" ) + "|"
             + getAttributeUri( attributes, "topography_major" ) + "|"
             + getAttributeUri( attributes, "laterality" ) + "|"
             + getAttributeValue( attributes, "stage" ) + "|"
             + getAttributeValue( attributes, "t" ) + "|"
             + getAttributeValue( attributes, "n" ) + "|"
             + getAttributeValue( attributes, "m" ) + "|"
             + getAttributeValue( attributes, "t" ) + "|"
             + getAttributeValue( attributes, "n" ) + "|"
             + getAttributeValue( attributes, "m" ) + "|"
             + getRelatedUri( related, "hasMethod" ) + "|"
             + getRelatedUri( related, "hasTreatment" ) + "|"
             + getRelatedUri( related, "Regimen_Has_Accepted_Use_For_Disease" ) + "|"
             + getRelatedUri( related, "Disease_Has_Normal_Tissue_Origin" ) + "|"
             + getRelatedUri( related, "Disease_Has_Normal_Cell_Origin" ) + "|"
             + getRelatedUri( related, "Disease_Has_Finding" ) + "|"
             + getRelatedUri( related, "Disease_May_Have_Finding" ) + "|"
             + getAttributeValue( attributes, "PSA" ) + "|"
             + getRelatedUri( related, "hasGleasonScore" ) + "|\n";
   }


   static private final String TUMOR_HEADER = "Cancer_ID|"
                                              + "Patient_ID|"
                                              + "Summary_Type|"
                                              + "Summary_ID|"
                                              + "Summary_URI|"
                                              + "hasCancerType|"
                                              + "hasHistologicType|"
                                              + "hasHistoricity|"
                                              + "hasBodySite|"
                                              + "hasLaterality|"
                                              + "hasDiagnosis|"
                                              + "isMetastasisOf|"
                                              + "hasTumorType|"
                                              + "hasTumorExtent|"
                                              + "hasMethod|"
                                              + "hasTreatment|"
                                              + "Disease_Has_Normal_Tissue_Origin|"
                                              + "Disease_Has_Normal_Cell_Origin|"
                                              + "hasSize|"
                                              + "hasCalcification|"
                                              + "has_Ulceration|"
                                              + "has_Breslow_Depth|"
                                              + "hasQuadrant|"
                                              + "hasClockface|"
                                              + "has_ER_Status|"
                                              + "has_PR_Status|"
                                              + "has_HER2_Status|\n";

   static private String createTumorLine( final String patientId,
                                          final String cancerId,
                                          final Tumor tumor,
                                          final Map<String,Fact> idFactMap ) {
      final Collection<NeoplasmAttribute> attributes = tumor.getAttributes();
      final Map<String,List<String>> idRelatedIdMap = tumor.getRelatedFactIds();
      final Map<String,List<Fact>> related = FactRelationUtil.getRelatedFactsMap( idRelatedIdMap, idFactMap );
      return cancerId + "|" + patientId + "|" + "Tumor|" + tumor.getId() + "|" + tumor.getClassUri() + "|"
             + getRelatedUri( related, "hasCancerType" ) + "|"
             + getAttributeValue( attributes, "histology" ) + "|"
             + getRelatedUri( related, "hasHistoricity" ) + "|"
             + getAttributeUri( attributes, "topography_major" ) + "|"
             + getAttributeUri( attributes, "laterality" ) + "|"
             + getRelatedUri( related, "hasDiagnosis" ) + "|"
             + getRelatedUri( related, "isMetastasisOf" ) + "|"
             + getRelatedUri( related, "hasTumorType" ) + "|"
             + getRelatedUri( related, "hasTumorExtent" ) + "|"
             + getRelatedUri( related, "hasMethod" ) + "|"
             + getRelatedUri( related, "hasTreatment" ) + "|"
             + getRelatedUri( related, "Disease_Has_Normal_Tissue_Origin" ) + "|"
             + getRelatedUri( related, "Disease_Has_Normal_Cell_Origin" ) + "|"
             + getRelatedValue( related, "hasSize" ) + "|"
             + getRelatedUri( related, "hasCalcification" ) + "|"
             + getRelatedUri( related, "has_Ulceration" ) + "|"
             + getRelatedUri( related, "has_Breslow_Depth" ) + "|"
             + getRelatedUri( related, "hasQuadrant" ) + "|"
             + getRelatedUri( related, "hasClockface" ) + "|"
             + getAttributeValue( attributes, "ER_" ) + "|"
             + getAttributeValue( attributes, "PR_" ) + "|"
             + getAttributeValue( attributes, "HER2" ) + "|\n";
   }


   static private String getAttributeValue( final Collection<NeoplasmAttribute> attributes,
                                            final String attributeName ) {
      return attributes.stream()
                .filter( a -> a.getName().equalsIgnoreCase( attributeName ) )
                .map( NeoplasmAttribute::getValue )
                       .distinct()
                .collect( Collectors.joining( ";" ) );
   }

   static private String getAttributeUri( final Collection<NeoplasmAttribute> attributes,
                                          final String attributeName ) {
      return attributes.stream()
                       .filter( a -> a.getName().equalsIgnoreCase( attributeName ) )
                       .map( NeoplasmAttribute::getClassUri )
                       .distinct()
                       .collect( Collectors.joining( ";" ) );
   }

   static private String getRelatedUri( final Map<String,List<Fact>> relatedFacts,
                                        final String relationName ) {
      return relatedFacts.getOrDefault( relationName, Collections.emptyList() )
                         .stream()
                       .map( Fact::getClassUri )
                       .distinct()
                       .collect( Collectors.joining( ";" ) );
   }


   static private String getRelatedValue( final Map<String,List<Fact>> relatedFacts,
                                          final String relationName ) {
      return relatedFacts.getOrDefault( relationName, Collections.emptyList() )
                         .stream()
                         .map( Fact::getValue )
                         .distinct()
                         .collect( Collectors.joining( ";" ) );
   }


}
