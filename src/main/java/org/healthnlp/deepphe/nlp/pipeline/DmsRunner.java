package org.healthnlp.deepphe.nlp.pipeline;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.JCasPool;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.node.NoteNodeCreator;
import org.healthnlp.deepphe.node.PatientCreator;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.summary.engine.SummaryEngine;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/14/2020
 */
public enum DmsRunner implements Closeable {
   INSTANCE;

   static public DmsRunner getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "DmsRunner" );


   // Use a constant piper name.
   static private final String NLP_PIPER_PATH = "pipeline/DmsNoteNlp.piper";

   private final AnalysisEngine _engine;
   private final JCasPool _pool;
   private final TextRunner _textRunner;


   DmsRunner() throws ExceptionInInitializerError {
      synchronized ( NLP_PIPER_PATH ) {
         try {
            final PiperFileReader reader = new PiperFileReader( NLP_PIPER_PATH );
            final PipelineBuilder builder = reader.getBuilder();

            final AnalysisEngineDescription pipeline = builder.getAnalysisEngineDesc();
            _engine = UIMAFramework.produceAnalysisEngine( pipeline );
            _pool = new JCasPool( 2, _engine );
         } catch ( IOException | UIMAException multE ) {
            Logger.getLogger( "DmsRunner" ).error( multE.getMessage() );
            throw new ExceptionInInitializerError( multE );
         }

         _textRunner = new TextRunner();
         final ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.execute( _textRunner );
      }
   }

   /**
    * stop the run queue.
    */
   @Override
   public void close() {
      _textRunner.close();
      PatientNodeStore.getInstance().close();
   }


   ////////////////////////////////////////////////////////////////////////
   //
   //                            REST Calls
   //
   ////////////////////////////////////////////////////////////////////////


   /**
    * Summarize the note and return a patient summary in json as if the note represents the entire patient.
    * This is not the preferred call.  It is preferred that a patient Id is provided, even if fake.
    *
    * @param docId -
    * @param text  -
    * @return -
    */
   public String summarizeDoc( final String docId, final String text ) {
      return summarizeDoc( docId, docId, text );
   }


   /**
    * Summarize the note and return a patient summary in json as if the note represents the entire patient.
    * This is the preferred call.
    *
    * @param patientId -
    * @param docId     -
    * @param text      -
    * @return -
    */
   public String summarizeDoc( final String patientId, final String docId, final String text ) {
      synchronized ( NLP_PIPER_PATH ) {
         try {
            final Note note = runNlp( patientId, docId, text );
            if ( note == null ) {
               return "{}";
            }

            return summarizeNoteAsPatient( patientId, note );
         } catch ( CASRuntimeException multE ) {
//            LOGGER.error( "Error processing text." );
//            throw new AnalysisEngineProcessException( multE );
            return multE.getMessage();
         }
      }
   }

   /**
    * Summarize and return a patient summary in json as if the note represents the entire patient.
    * Extracted NLP Information will be stored for future full- patient summary.
    *
    * @param patientId -
    * @param docId     -
    * @param text      -
    * @return -
    */
   public String summarizeAndStoreDoc( final String patientId, final String docId, final String text ) {
      synchronized ( NLP_PIPER_PATH ) {
         try {
            final Note note = runNlp( patientId, docId, text );
            if ( note == null ) {
               return "{}";
            }

            storeDoc( patientId, note );

            return summarizeNoteAsPatient( patientId, note );
         } catch ( CASRuntimeException multE ) {
            return multE.getMessage();
         }
      }
   }

   /**
    * Queue the document, running it at some later time and storing the result.
    * Extracted NLP Information will be stored for future full- patient summary.
    *
    * @param patientId -
    * @param docId     -
    * @param text      -
    * @return -
    */
   public String queueAndStoreDoc( final String patientId, final String docId, final String text ) {
      return _textRunner.addText( patientId, docId, text );
   }


   /**
    * Summarize the patient from previously stored NLP-extracted information and return a patient summary in json.
    *
    * @param patientId -
    * @return -
    */
   public String summarizePatient( final String patientId ) {
      final Patient patient = PatientNodeStore.getInstance().getOrCreate( patientId );
      final PatientSummary patientSummary = SummaryEngine.createPatientSummary( patient );

      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson( patientSummary );
   }


   ////////////////////////////////////////////////////////////////////////
   //
   //                            NLP
   //
   ////////////////////////////////////////////////////////////////////////


   /**
    * runs a cas through the pipeline.
    *
    * @param patientId -
    * @param docId     -
    * @param text      -
    */
   private Note runNlp( final String patientId, final String docId, final String text ) {
      synchronized ( NLP_PIPER_PATH ) {
         try {
            final JCas jCas = _pool.getJCas( -1 );
            if ( jCas == null ) {
               throw new AnalysisEngineProcessException( new Throwable( "Could not acquire JCas from pool." ) );
            }
            new JCasBuilder().setPatientId( patientId )
                             .setDocId( docId )
                             .setDocText( text )
                             .rebuild( jCas );
            runNlp( jCas );

            final Note note = NoteNodeCreator.createNote( jCas );
            _pool.releaseJCas( jCas );

            return note;
         } catch ( UIMAException | CASRuntimeException multE ) {
            LOGGER.error( multE.getMessage() );
         }
      }
      return null;
   }


   /**
    * runs a cas through the pipeline.
    *
    * @param jCas The cas should already be populated with text, patientId, docId, etc.
    */
   private void runNlp( final JCas jCas ) {
      synchronized ( NLP_PIPER_PATH ) {
         LOGGER.info( "Processing " + DocIdUtil.getDocumentID( jCas ) );
         try {
            _engine.process( jCas );
         } catch ( UIMAException | CASRuntimeException multE ) {
            LOGGER.error( multE.getMessage() );
         }
      }
   }


   ////////////////////////////////////////////////////////////////////////
   //
   //                            Note Storage
   //
   ////////////////////////////////////////////////////////////////////////


   private void storeDoc( final String patientId, final Note note ) {
      final Patient storedPatient = PatientNodeStore.getInstance().getOrCreate( patientId );
      PatientCreator.addNote( storedPatient, note );
   }


   ////////////////////////////////////////////////////////////////////////
   //
   //                            Note Summarization
   //
   ////////////////////////////////////////////////////////////////////////


   static private String summarizeNoteAsPatient( final String patientId, final Note note ) {
      final Patient patient = new Patient();
      patient.setId( patientId );
      patient.setNotes( Collections.singletonList( note ) );

      final PatientSummary patientSummary = SummaryEngine.createPatientSummary( patient );

      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson( patientSummary );
   }


   ////////////////////////////////////////////////////////////////////////
   //
   //                            Queue Runner
   //
   ////////////////////////////////////////////////////////////////////////


   private class TextRunner implements Runnable, Closeable {

      static private final String STOP_TEXT = "STOP_PROCESSING_NOW";
      private final SimpleDoc STOP_DOC = new SimpleDoc( STOP_TEXT, STOP_TEXT, STOP_TEXT );
      private final BlockingQueue<SimpleDoc> _textQueue = new ArrayBlockingQueue<>( 100 );
      private boolean _stop;

      private String addText( final String patientId, final String docId, final String text ) {
         if ( _textQueue.offer( new SimpleDoc( patientId, docId, text ) ) ) {
            return "Added " + patientId + " " + docId + " to the Text Processing Queue.";
         }
         return "Problem adding " + patientId + " " + docId + "to the Text Processing Queue.";
      }

      public void run() {
         while ( !_stop ) {
            try {
               // Use poll instead of take just in case we need to close using _stop
               final SimpleDoc simpleDoc = _textQueue.poll( 1, TimeUnit.MINUTES );
               if ( simpleDoc == null ) {
                  continue;
               }
               if ( simpleDoc.equals( STOP_DOC ) ) {
                  break;
               }
               final Note note = runNlp( simpleDoc._patientID, simpleDoc._docID, simpleDoc._text );
               storeDoc( simpleDoc._patientID, note );
            } catch ( InterruptedException intE ) {
               //
            }
         }
      }

      public void close() {
         _stop = true;
         _textQueue.offer( STOP_DOC );
      }

      private final class SimpleDoc {

         private final String _patientID;
         private final String _docID;
         private final String _text;

         private SimpleDoc( final String patientID, final String docID, final String text ) {
            _patientID = patientID;
            _docID = docID;
            _text = text;
         }

      }

   }


}