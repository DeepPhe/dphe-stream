package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/6/2020
 */
@PipeBitInfo(
      name = "AllConceptsWriter",
      description = "For dphe-nlp.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class AllConceptsWriter extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "AllConceptsWriter" );

//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void initialize( final UimaContext context ) throws ResourceInitializationException {
//      // Always call the super first
//      super.initialize( context );
//
//      // place AE initialization code here
//
//   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Writing all Concepts ..." );
//      JCasUtil.select( jCas, IdentifiedAnnotation.class )
//              .forEach( a -> LOGGER.info( toInfo( a ) ) );

      JCasUtil.select( jCas, BinaryTextRelation.class )
              .forEach( r -> LOGGER.info( toRelInfo( r ) ) );

      LOGGER.info( "Writing Essential Concepts ..." );
      Map<IdentifiedAnnotation, Collection<Integer>> corefs = EssentialAnnotationUtil.createMarkableCorefs(jCas);
      Collection<IdentifiedAnnotation> requiredAnnotations = (Collection)EssentialAnnotationUtil.getRequiredAnnotations(jCas, corefs).stream().filter((a) -> {
         return !Neo4jOntologyConceptUtil.getUri(a).isEmpty();
      }).collect( Collectors.toList());
      requiredAnnotations.forEach( a -> LOGGER.info( toInfo( a ) ) );
   }

   static private String toInfo( final IdentifiedAnnotation annotation ) {
      return Neo4jOntologyConceptUtil.getUri( annotation )
             + " " + Neo4jOntologyConceptUtil.getCui( Neo4jOntologyConceptUtil.getUri( annotation ) )
             + " " + annotation.getCoveredText();
   }

   static private String toRelInfo( final BinaryTextRelation relation ) {
      return relation.getArg1().getArgument().getCoveredText()
             + " " + relation.getCategory()
             + " " + relation.getArg2().getArgument().getCoveredText();
   }

}
