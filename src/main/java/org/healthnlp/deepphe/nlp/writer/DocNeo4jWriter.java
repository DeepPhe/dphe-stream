package org.healthnlp.deepphe.nlp.writer;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.core.json.JsonNoteWriter;
import org.healthnlp.deepphe.neo4j.driver.DriverConnection;
import org.healthnlp.deepphe.neo4j.util.JsonUtil;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/10/2019
 */
@PipeBitInfo(
      name = "DocNeo4jWriter",
      description = "Write dPhe note data to neo4j.",
      role = PipeBitInfo.Role.WRITER
)
public class DocNeo4jWriter extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "DocNeo4jWriter" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      LOGGER.info( "Initializing Naaccr Neo4j Writer ..." );
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( uimaContext );

      final Driver driver = DriverConnection.getInstance().getDriver();
      if ( driver == null ) {
         LOGGER.info( "Empty Driver.  Writing to Neo4j will be skipped." );
         return;
      }
      try ( Session session = driver.session() ) {
         try ( Transaction tx = session.beginTransaction() ) {
            tx.run( "CALL deepphe.initializeDphe()" );
            tx.commit();
         }
      } catch ( Exception e ) {
         LOGGER.error( e.getMessage() );
         throw new ResourceInitializationException( e );
      }
   }


   /**
    * Call necessary processing for patient
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );

      LOGGER.info( "Writing " + patientId + " " + DocIdUtil.getDocumentID( jCas )
                   + " to Neo4j Server " + DriverConnection.getInstance().getUrl() );
      final Driver driver = DriverConnection.getInstance().getDriver();
      if ( driver == null ) {
         LOGGER.info( "Empty Driver.  Writing to Neo4j will be skipped." );
         return;
      }
      final String noteJson = JsonNoteWriter.createNoteJson( jCas );
      final String neo4jOkJson = JsonUtil.packForNeo4j( noteJson );
      try ( Session session = driver.session() ) {
         try ( Transaction tx = session.beginTransaction() ) {
//            tx.run( "CALL deepphe.addNoteInfo('" + patientId + "','" + noteJson + "')" );
            tx.run( "CALL deepphe.addNoteInfo(\"" + patientId + "\",\"" + neo4jOkJson + "\")" );
            tx.commit();
         }
      } catch ( Exception e ) {
         LOGGER.error( e.getMessage() );
         throw new AnalysisEngineProcessException( e );
      }
   }



}
