package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.summary.attribute.morphology.Morphology;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AllUriInfoStore extends UriInfoStore {

   public AllUriInfoStore( final Collection<ConceptAggregate> neoplasms ) {
      super( getAllUris( neoplasms ) );
      final Map<String, Integer> uriStrengths = getAllUriStrengths( neoplasms );
      setUriStrengths( uriStrengths );
      setClassLevelMap( UriScoreUtil.createUriClassLevelMap( _uris ) );
   }

   // TODO - switch this for a visitor !  Visitor handles the uri fetch - be it grades, lateralities, locations, etc.
   static private Collection<String> getAllUris( final Collection<ConceptAggregate> neoplasms ) {
      return neoplasms.stream()
                      .map( ConceptAggregate::getAllUris )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toSet() );
   }

   // TODO - switch this for a visitor !  Visitor handles the uri strengths - be it grades, lateralities, locations,
   //  etc.
   static private Map<String,Integer> getAllUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final List<KeyValue<String, Double>> uriQuotients = neoplasms.stream()
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
      Morphology.QUOTIENT_TEXT = "Quotients: " + uriQuotients.stream()
                                                  .map( kv -> kv.getKey() +" " + kv.getValue() )
                                                  .collect(  Collectors.joining(",") ) + "\n";
      Morphology.QUOTIENT_TEXT += "Strengths: " + uriStrengths.entrySet().stream()
                                                   .map( e -> e.getKey() +" " + e.getValue() )
                                                   .collect(  Collectors.joining(",") ) + "\n";
      return uriStrengths;
   }

}
