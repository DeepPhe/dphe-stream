package org.healthnlp.deepphe.summary.attribute.laterality;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.morphology.Morphology;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final public class LateralUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> getRelatedConcepts( final Collection<ConceptAggregate> neoplasms ) {
      return neoplasms.stream()
                      .map( c -> c.getRelated( RelationConstants.HAS_LATERALITY ) )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toSet() );
   }


   public Collection<String> getAllUris( final Collection<ConceptAggregate> neoplasms ) {
      return getRelatedConcepts( neoplasms )
            .stream()
             .map( ConceptAggregate::getAllUris )
             .flatMap( Collection::stream )
             .collect( Collectors.toSet() );
   }

   // Allows for uris that have tied quotients
   public Collection<String> getMainUris( final Collection<ConceptAggregate> neoplasms ) {
      return getRelatedConcepts( neoplasms )
            .stream()
             .map( ConceptAggregate::getUri )
             .collect( Collectors.toSet() );
   }

   public Map<String,Integer> getAllUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> lateralities = getRelatedConcepts( neoplasms );
      final List<KeyValue<String, Double>> uriQuotients
            = lateralities.stream()
                         .map( n -> UriScoreUtil.mapUriQuotients( n.getAllUris(),
                                                                  n.getUriRootsMap(),
                                                                  n.getMentions() ) )
                         .flatMap( Collection::stream )
                         .collect( Collectors.toList() );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         final int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         final int strength = (int)Math.ceil( quotients.getValue()*100 );
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      Morphology.QUOTIENT_TEXT = "L Quotients: " + uriQuotients.stream()
                                                             .map( kv -> kv.getKey() +" " + kv.getValue() )
                                                             .collect(  Collectors.joining(",") ) + "\n";
      Morphology.QUOTIENT_TEXT += "L Strengths: " + uriStrengths.entrySet().stream()
                                                              .map( e -> e.getKey() +" " + e.getValue() )
                                                              .collect(  Collectors.joining(",") ) + "\n";
      return uriStrengths;
   }

}
