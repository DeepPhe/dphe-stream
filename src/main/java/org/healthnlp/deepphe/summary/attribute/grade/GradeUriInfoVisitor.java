package org.healthnlp.deepphe.summary.attribute.grade;

import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

final public class GradeUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _gradeConcepts;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _gradeConcepts == null ) {
         _gradeConcepts = neoplasms.stream()
                                   .map( c -> c.getRelated( HAS_GLEASON_SCORE,
                                                DISEASE_IS_GRADE,
                                                DISEASE_HAS_FINDING ) )
                                   .flatMap( Collection::stream )
                                   .filter( c -> GradeCodeInfoStore.getGradeNumber( c ) >= 0  )
                                   .collect( Collectors.toSet() );
      }
      return _gradeConcepts;
   }

   /**
    * Grade uris are ranked in order of represented grade number regardless of the uri quotients.
    * @param neoplasms -
    * @return the grade score (0-5) * 20.
    */
   @Override
   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
      return concepts.stream()
                     .map( ConceptAggregate::getAllUris )
                     .flatMap( Collection::stream )
                     .distinct()
                     .collect( Collectors.toMap( Function.identity(),
                                                 GradeUriInfoVisitor::getGradeStrength ) );
   }

   static private int getGradeStrength( final String uri ) {
      return Math.min( 0, Math.max( 100, GradeCodeInfoStore.getUriGradeNumber( uri )*20 ) );
   }

}
