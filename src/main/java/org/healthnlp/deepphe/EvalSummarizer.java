package org.healthnlp.deepphe;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.nlp.pipeline.DmsRunner;
import org.healthnlp.deepphe.summary.attribute.morphology.Morphology;
import org.healthnlp.deepphe.util.eval.FeatureFilesAppender;
import org.healthnlp.deepphe.util.eval.ForEvalLineCreator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Run multiple docs and write output for scoring with the eval tool and output for features.
 */
final public class EvalSummarizer {

   static private final Logger LOGGER = Logger.getLogger( "EvalSummarizer" );

   static public final String PATIENT_ID = "Patient_ID";

   static private final List<String> ATTRIBUTE_NAMES
         = Arrays.asList( "topography_major",
                         "topography_minor",
                         "histology",
                         "behavior",
                         "grade" );
   static private final List<String> EVAL_ATTRIBUTE_NAMES
         = Arrays.asList( "*patient ID",
                          "-Summary_ID",
                          "topography: ICD-O code (MAJOR SITE); code is only the digits before decimal point",
                          "topography: ICD-O code (SUBSITE); code is only digits after decimal point",
                          "histology: ICD-O code",
                          "behavior: ICD-O code",
                          "grade: ICD-O code" );

//    "*patient ID|topography: ICD-O code (MAJOR SITE); code is only the digits before decimal point|-primary site: text in note|topography: ICD-O code (SUBSITE); code is only digits after decimal point|-site: text in note|histology: ICD-O code|-histology: text in note|behavior: ICD-O code|-behavior: text in note|laterality: ICD-O code|-laterality: text in note|grade: ICD-O code|-grade: text in note|-AJCC Clinical TNM|-AJCC Pathological TNM: text in note|AJCC Pathological T value|AJCC Pathological N value|AJCC Pathological M value|-3 digits: Tumor size|-Text: Tumor size|-2 digits: Extension|-Text: Extension|-1 digit: Lymph nodes|-Text: Lymph nodes|-2 digits: # of lymph nodes pathologically positive|-Text: # of lymph nodes pathologically positive|-2 digits: # of lymph nodes pathologically examined|-Text: # of lymph nodes pathologically examined|-ER: DeepPhe value set|ER: SSDI value set|-text for ER|-PR: DeepPhe value set|PR: SSDI value set|-text for PR|-HER2: DeepPhe Value Set|HER2: SSDI Value Set|-text for HER2|-ki67: DeepPhe value set|-ki67: DeepPhe value set; if % enter value|-ki67: SSDI value set|ki67: SSDI value set; if % positive enter value|-text for ki67|-BRCA1: DeepPhe value set|-BRCA1: SSDI value set|-text for BRCA1|-BRCA2: DeepPhe value set|-BRCA2: SSDI value set|-text for BRCA2|-ALK: DeepPhe value set|-ALK: SSDI value set|-text for ALK|-EGFR: DeepPhe value set|-EGFR: SSDI value set|-text for EGFR|-BRAF: DeepPhe value set|-BRAF: SSDI value set|-text for BRAF|-ROS1: DeepPhe value set|-ROS1: SSDI value set|-text for ROS1|-pd1: DeepPhe value set|-pd1: SSDI value set|-text for pd1|-pdl1: DeepPhe value set|-pdl1: SSDI value set|-text for pdl1|-msi: DeepPhe value set|msi: SSDI value set|-text for msi|-KRAS: DeepPhe value set|KRAS: SSDI value set|-text for KRAS|-PSA: DeepPhe value set|-PSA: DeepPhe value set -- if numerical enter the value|-PSA: SSDI value set|PSA: SSDI value set - if numerical enter the value|-text for PSA\n"

//   C:\Spiffy\data\dphe_data\xlated\kcr\text_1_90
//   C:\Spiffy\data\dphe_data\xlated\kcr\text_91_300
//   C:\Spiffy\data\dphe_data\xlated\kcr\text_301_600
//   C:\Spiffy\data\dphe_data\datasets\KCR\preexisting_gold_annotations\corpus_for_3_cancers

//   C:\Spiffy\data\dphe_data\combined
   public static void main( final String... args ) {
      LOGGER.info( "Initializing ..." );
      DmsRunner.getInstance();
      LOGGER.info( "Reading docs in " + args[ 0 ] + " writing Output for Eval to " + args[ 1 ] );
      final File evalFile = new File( args[ 1 ] );
      final File featuresDir = new File( evalFile.getParentFile(), "features" );
      try {
         FeatureFilesAppender.initFeatureFiles( featuresDir, ATTRIBUTE_NAMES );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE );
      }
      LOGGER.info( "Writing Header in " + evalFile.getPath() );
      try ( Writer evalWriter = new FileWriter( evalFile ) ) {
         // Our gold standard files use horrifically long "human-readable" names.
         for ( String attribute : EVAL_ATTRIBUTE_NAMES ) {
            evalWriter.write( attribute + "|" );
         }
         evalWriter.write( "\n" );
         evalWriter.flush();
         processDir( new File( args[ 0 ] ), evalWriter, featuresDir );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      DmsRunner.getInstance()
               .close();

      final Map<String,Map<String,Long>> confusion = new HashMap<>();
      for ( Map.Entry<String,List<String>> confused : Morphology.CONFUSION.entrySet() ) {
         final Map<String,Long> counts = confused.getValue().stream().collect( Collectors.groupingBy(
               Function.identity(), Collectors.counting() ) );
         confusion.put( confused.getKey(), counts );
      }
      System.out.println("System : Gold");
      confusion.forEach( (k,v) -> System.out.println( k + " : " + v ) );

      final Map<String,Map<String,Long>> realConfusion = new HashMap<>();
      for ( Map.Entry<String,List<String>> confused : Morphology.REAL_CONFUSION.entrySet() ) {
         final Map<String,Long> counts = confused.getValue().stream().collect( Collectors.groupingBy(
               Function.identity(), Collectors.counting() ) );
         realConfusion.put( confused.getKey(), counts );
      }
      System.out.println("System : Available Gold");
      realConfusion.forEach( (k,v) -> System.out.println( k + " : " + v ) );

      System.out.println("System Counts");
      Morphology.SYS_COUNTS.forEach( (k,v) -> System.out.println( k + " = " + v ) );
      System.out.println("Gold Counts");
      Morphology.GOLD_COUNTS.forEach( (k,v) -> System.out.println( k + " = " + v ) );

      System.exit( 0 );
   }

   static private void processDir( final File dir,
                                   final Writer evalWriter,
                                   final File featureDir )
         throws IOException {
      LOGGER.info( "Processing directory " + dir.getPath() );
      final File[] files = dir.listFiles();
      if ( files == null ) {
         return;
      }
      for ( File file : files ) {
         if ( file.isDirectory() ) {
            processDir( file, evalWriter, featureDir );
         } else {
            processDoc( file, evalWriter, featureDir );
         }
      }
   }

   static private void processDoc( final File file,
                                   final Writer evalWriter,
                                   final File featureDir )
         throws IOException {
      String docId = file.getName();
      final int dotIndex = docId.lastIndexOf( '.' );
      if ( dotIndex > 0 ) {
         docId = docId.substring( 0, dotIndex );
      }
      String text = "";
      try {
         text = new String( Files.readAllBytes( Paths.get( file.getPath() ) ) );
      } catch ( IOException ioE ) {
         LOGGER.error( "Processing Failed:\n" + ioE.getMessage() );
         System.exit( -1 );
      }
      // Process doc text
      final PatientSummary summary = DmsRunner.getInstance()
                                      .createPatientSummary( docId, text );
      if ( summary == null ) {
         LOGGER.error( "Could not process doc " + docId );
         return;
      }
      final String patientId = summary.getPatient()
                                      .getId();
      for ( NeoplasmSummary neoplasm : summary.getNeoplasms() ) {
         writeEval( patientId, neoplasm, evalWriter );
         writeFeatures( patientId, neoplasm, featureDir );
      }
   }

   static private void writeEval( final String patientId,
                                  final NeoplasmSummary summary,
                                  final Writer writer ) throws IOException {
      writer.write( ForEvalLineCreator.createBsv( patientId, summary, ATTRIBUTE_NAMES ) );
   }

   static private void writeFeatures( final String patientId,
                                      final NeoplasmSummary summary,
                                      final File featureDir ) {
      FeatureFilesAppender.appendFeatureFiles( patientId, summary, featureDir, ATTRIBUTE_NAMES );
   }


}
