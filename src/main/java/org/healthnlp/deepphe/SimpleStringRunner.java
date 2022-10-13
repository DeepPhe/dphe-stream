package org.healthnlp.deepphe;

import org.apache.ctakes.core.pipeline.EntityCollector;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;

import java.io.IOException;
import java.util.Collections;

/**
 * Build and run a pipeline using a {@link PiperFileReader} and a {@link PipelineBuilder}.
 * <p>
 * Example of a running a pipeline programatically w/o uima xml descriptor xml files
 * Adds the default Tokenization pipeline and adding the Example HelloWorld Annotator
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2016
 */
final public class SimpleStringRunner {

   static private final Logger LOGGER = Logger.getLogger( "HelloWorldPropsPiperRunner" );

   static private final String PIPER_FILE_PATH
//         = "org/apache/ctakes/examples/pipeline/HelloWorldProps.piper";
//         = "DefaultFastPipeline";
         = "pipeline/DpheXnEval";

   static private final String DOC_TEXT
//         = "Hello World!  I feel no pain.  My father takes aspirin.  My sister might have a headache.";
         = "7/5/17 - 9/21/17 MEADOWVIEW/DR PRABHU - RT HILUM AND MEDIASTINUM TX TO A REG DOSE OF 2,700 CGY IN 15 FX USING 6X; RT HILUM TX TO A BOOST DOSE OF 3,960 CGY IN 22 FX USING 6X";

   private SimpleStringRunner() {
   }

   /**
    * @param args an output directory for xmi files or none if xmi files are not wanted
    */
   public static void main( final String... args ) {
      try {
         // Add a simple pre-defined existing pipeline for Tokenization from file
         final PiperFileReader reader = new PiperFileReader( PIPER_FILE_PATH );
         // Add the property extraction pipeline
         PipelineBuilder builder = reader.getBuilder();
         builder.readFiles( "C:/temp/V7NMFS4R.txt" );
         builder.add( org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriterFit.class, Collections.emptyList(),
                      "OutputDirectory", "C:/temp" );
         if ( args.length > 0 ) {
            // Example to save the Aggregate descriptor to an xml file for external use such as the UIMA CVD
            builder.writeXMIs( args[ 0 ] );
         }
         // Run the pipeline with specified text
         builder.run();
         // Log the IdentifiedAnnotation objects
         LOGGER.info( "\n" + EntityCollector.getInstance()
                                            .toString() );
      } catch ( IOException | UIMAException multE ) {
         LOGGER.error( multE.getMessage() );
      }
   }

}
