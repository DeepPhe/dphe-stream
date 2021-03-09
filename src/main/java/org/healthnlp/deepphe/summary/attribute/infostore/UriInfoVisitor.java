package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.*;
import java.util.stream.Collectors;

public interface UriInfoVisitor {

   Collection<ConceptAggregate> getAttributeConcepts( Collection<ConceptAggregate> neoplasms );

   default Collection<String> getAttributeAllUris( final Collection<ConceptAggregate> neoplasms ) {
      return getAttributeConcepts( neoplasms )
            .stream()
            .map( ConceptAggregate::getAllUris )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
   }

   // Allows for uris that have tied quotients
   default Collection<String> getAttributeMainUris( final Collection<ConceptAggregate> neoplasms ) {
      return getAttributeConcepts( neoplasms )
            .stream()
            .map( ConceptAggregate::getUri )
            .collect( Collectors.toSet() );
   }

   default Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
      if ( concepts.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Collection<String> allUris = concepts.stream()
                                                 .map( ConceptAggregate::getAllUris )
                                                 .flatMap( Collection::stream )
                                                 .collect( Collectors.toSet() );
      final Map<String,Collection<String>> allUriRoots = concepts.stream()
                                                                 .map( ConceptAggregate::getUriRootsMap )
                                                                 .map( Map::entrySet )
                                                                 .flatMap( Collection::stream )
                                                                 .distinct()
                                                                 .collect( Collectors.toMap( Map.Entry::getKey,
                                                                                             Map.Entry::getValue ) );
      final Collection<Mention> allMentions = concepts.stream()
                                                      .map( ConceptAggregate::getMentions )
                                                      .flatMap( Collection::stream )
                                                      .collect( Collectors.toSet() );
      final List<KeyValue<String, Double>> uriQuotients = UriScoreUtil.mapUriQuotients( allUris,
                                                                                        allUriRoots,
                                                                                        allMentions );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         final int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         final int strength = (int)Math.ceil( quotients.getValue() * 100 );
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      return uriStrengths;
   }

   default Map<String,Integer> getAttributeUriMaxStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
      if ( concepts.isEmpty() ) {
         return Collections.emptyMap();
      }
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
         final int strength = (int)Math.ceil( quotients.getValue() * 100 );
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      return uriStrengths;
   }

   default Map<String,Integer> getAttributeUriAveStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
      if ( concepts.isEmpty() ) {
         return Collections.emptyMap();
      }
      final List<KeyValue<String, Double>> uriQuotients
            = concepts.stream()
                      .map( c -> UriScoreUtil.mapUriQuotients( c.getAllUris(),
                                                               c.getUriRootsMap(),
                                                               c.getMentions() ) )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toList() );
      final Map<String,Collection<Double>> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         uriStrengths.computeIfAbsent( quotients.getKey(), s -> new ArrayList<>() )
                     .add( quotients.getValue() );
      }
      return uriStrengths.entrySet()
                  .stream()
                  .collect( Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (int)Math.ceil( e.getValue()
                              .stream()
                              .mapToDouble( d -> d )
                              .average()
                              .orElse( 0 ) ) * 100 ) );
   }


//   final class UriStrength {
//      final public String _uri;
//      final public int _sumStrength;
//      final public int _maxStrength;
//      final public int _aveStrength;
//      private UriStrength( final String uri,
//                           final double sumQuotient,
//                           final List<KeyValue<String, Double>> uriQuotients ) {
//         _uri = uri;
//         _sumStrength = (int)Math.ceil( sumQuotient * 100 );
//         _maxStrength = (int)Math.ceil( uriQuotients.stream()
//                                                    .filter( k -> k.getKey().equals( uri ) )
//                                                    .mapToDouble( KeyValue::getValue )
//                                                    .max()
//                                                    .orElse( 0 ) ) * 100;
//         _aveStrength = (int)Math.ceil( uriQuotients.stream()
//                     .filter( k -> k.getKey().equals( uri ) )
//                     .mapToDouble( KeyValue::getValue )
//                                               .average()
//                                               .orElse( 0 ) ) * 100;
//      }
//   }


}
