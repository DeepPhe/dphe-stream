package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface UriInfoVisitor {

   Collection<ConceptAggregate> getConcepts( Collection<ConceptAggregate> neoplasms );

   default Collection<String> getAllUris( final Collection<ConceptAggregate> neoplasms ) {
      return getConcepts( neoplasms )
            .stream()
            .map( ConceptAggregate::getAllUris )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }

   // Allows for uris that have tied quotients
   default Collection<String> getMainUris( final Collection<ConceptAggregate> neoplasms ) {
      return getConcepts( neoplasms )
            .stream()
            .map( ConceptAggregate::getUri )
            .collect( Collectors.toSet() );
   }

   default Map<String,Integer> getUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> concepts = getConcepts( neoplasms );
      final List<KeyValue<String, Double>> uriQuotients
            = concepts.stream()
                          .map( c -> UriScoreUtil.mapUriQuotients( c.getAllUris(),
                                                                   c.getUriRootsMap(),
                                                                   c.getMentions() ) )
                          .flatMap( Collection::stream )
                          .collect( Collectors.toList() );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         final int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         final int strength = (int)Math.ceil( quotients.getValue()*100 );
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      return uriStrengths;
   }

}
