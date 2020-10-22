package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.SemanticGroup;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.nlp.uri.UriAnnotationFactory;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/21/2019
 */
@PipeBitInfo(
      name = "ClockFaceFinder",
      description = "For deepphe.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class ClockFaceFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "ClockFaceFinder" );

   static private final Pattern CLOCK_PATTERN = Pattern.compile( "1?[0-9](?:\\s|(?::[03]0))" );
//   static private final Pattern RANGE_PATTERN = Pattern.compile( "1?[0-9]-1?[0-9](?:\\s|(?::[03]0))" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding simply stated clock faces ..." );
      findSimpleClocks( jCas );
   }

   static public void findSimpleClocks( final JCas jCas ) {
      final Collection<IdentifiedAnnotation> quadrants
            = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.QUADRANT );
      if ( quadrants.isEmpty() ) {
         return;
      }
      final String docText = jCas.getDocumentText();
      final int maxClockIndex = docText.length() - 16;
      for ( IdentifiedAnnotation quadrant : quadrants ) {
         if ( quadrant.getEnd() > maxClockIndex ) {
            // doc ends with a quadrant mention.
            continue;
         }
         final String nextText = docText.substring( quadrant.getEnd(), quadrant.getEnd() + 16 );
         if ( nextText.toLowerCase().contains( "clock" ) ) {
            // dictionary lookup probably found it.
            continue;
         }
         final Matcher matcher = CLOCK_PATTERN.matcher( nextText );
         if ( matcher.find() ) {
            final String matchText = nextText.substring( matcher.start(), matcher.end() );
            int hourEnd = 1;
            if ( Character.isDigit( matchText.charAt( 1 ) ) ) {
               hourEnd = 2;
            }
            final String hour = matchText.substring( 0, hourEnd );
            String minute = "";
            if ( hourEnd < matchText.length() ) {
               minute = '_' + matchText.substring( hourEnd + 1 ).trim();
               if ( minute.equals( "_00" ) ) {
                  minute = "";
               }
            }
            final String uri = hour + minute + "_o_clock_position";
            UriAnnotationFactory.createIdentifiedAnnotations( jCas,
                  quadrant.getEnd() + matcher.start(),
                  quadrant.getEnd() + matcher.end(), uri, SemanticGroup.MODIFIER, "T082" );
//            LOGGER.info( nextText + " -> " + matchText + " hourStart 0 hourEnd " + hourEnd + " hour = " + hour + " minute = " + minute + " uri = " + uri );
         }
      }
   }


}
