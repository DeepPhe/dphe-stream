package org.healthnlp.deepphe.util;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/22/2020
 */
final public class UriScoreUtil {

   static private final Logger LOGGER = Logger.getLogger( "UriScoreUtil" );

   private UriScoreUtil() {}


   static public String getBestUri( final Collection<String> uris ) {
      final Map<String,Collection<String>> uriRootsMap = UriUtil.mapUriRoots( uris );
      return getBestUriScore( uris, uriRootsMap ).getKey();
   }


   static public String getBestUri( final Collection<String> uris,
                                    final Map<String,Collection<String>> uriRootsMap ) {
      return getBestUriScore( uris, uriRootsMap ).getKey();
   }

   static public KeyValue<String,Double> getBestUriScore( final Collection<String> uris,
                                                          final Map<String,Collection<String>> uriRootsMap ) {
      final List<KeyValue<String,Double>> uriQuotients = mapUriQuotients( uris, uriRootsMap );
      final Map<String,Integer> loggerClassLevelMap
            = uriQuotients.stream()
                          .map( KeyValue::getKey )
                          .collect( Collectors.toMap( Function.identity(),
                                Neo4jOntologyConceptUtil::getClassLevel ) );

      uriQuotients.stream().map( kv -> "URI " + kv.getKey()
                                       + "   quotient score " + kv.getValue()
                                       + "   class level " + loggerClassLevelMap.get( kv.getKey() )
                                       + "   root count " + uriRootsMap.get( kv.getKey() ).size()
                                       + "   quotient level score " + (kv.getValue()*loggerClassLevelMap.get( kv.getKey() ))
                                       + "   rooted " + (kv.getValue()*loggerClassLevelMap.get( kv.getKey() )*uriRootsMap.get( kv.getKey() ).size()) )
                  .forEach( LOGGER::info );

      if ( uriQuotients.size() == 1 ) {
         LOGGER.info( "Only one URI for concept " + uriQuotients.get( 0 ).getKey() + " score = 1.0"  );
         return uriQuotients.get( 0 );
      }
      final List<KeyValue<String,Double>> bestKeyValues = getBestUriScores( uriQuotients );
      if ( bestKeyValues.size() == 1 ) {
         LOGGER.info( "Only one best URI score for concept " + bestKeyValues.get( 0 ).getKey() + " score = " +  bestKeyValues.get( 0 ).getValue() );
         return bestKeyValues.get( 0 );
      }
      final Map<String,Integer> classLevelMap
            = bestKeyValues.stream()
                           .map( KeyValue::getKey )
                           .collect( Collectors.toMap( Function.identity(),
                                 Neo4jOntologyConceptUtil::getClassLevel ) );

      final ToIntFunction<KeyValue<String,Double>> getClassLevel = kv -> classLevelMap.get( kv.getKey() );
      final ToIntFunction<KeyValue<String,Double>> getRootCount = kv -> uriRootsMap.get( kv.getKey() ).size();

      LOGGER.info( "The best URI is the one with the highest quotient score and the highest class level " +
                   "(furthest from root by the shortest path) with ties broken by total number of nodes (all routes) to root.\n" +
                   "This is all about high representation and high precision.\n" +
                   "The highest quotient is a measure of fully and exactly representing the most mentions.\n" +
                   "The class level is a measure of specificity - the furthest the shortest path is from root the more specific the concept.\n" +
                   "Breaking a tie with the most nodes between a concept and root is sort of a measure of both\n" +
                   "specificity and high representation, but a much less exact measure of each." );


      return bestKeyValues.stream()
                          .max( Comparator.comparingInt( getClassLevel ).thenComparingInt( getRootCount ) )
                          .orElse( bestKeyValues.get( bestKeyValues.size()-1 ) );
   }


   static private List<KeyValue<String,Double>> getBestUriScores( final List<KeyValue<String,Double>> uriQuotients ) {
      final Double bestQuotient = uriQuotients.get( uriQuotients.size()-1 ).getValue();
      return uriQuotients.stream()
                         .filter( q -> bestQuotient.compareTo( q.getValue() ) == 0 )
                         .sorted( Comparator.comparing( KeyValue::getKey ) )
                         .collect( Collectors.toList() );
   }


   static public List<KeyValue<String,Double>> mapUriQuotients( final Collection<String> uris,
                                                                final Map<String, Collection<String>> uriRootsMap ) {
      if ( uris.size() == 1 ) {
         final String uri = new ArrayList<>( uris ).get( 0 );
         LOGGER.info( "Only one URI for concept " + uri + " quotient score = 1.0"  );
         return Collections.singletonList( new KeyValue<>( uri, 1d ) );
      }

      uris.remove( UriConstants.NEOPLASM );
      if ( uris.size() == 1 ) {
         final String uri = new ArrayList<>( uris ).get( 0 );
         LOGGER.info( "Only one URI under root for concept " + uri + " quotient score = 1.0"  );
         return Collections.singletonList( new KeyValue<>( uri, 1d ) );
      }

      final Map<String,Long> uriCountsMap = mapUriCounts( uris );


      LOGGER.info( "Determining best URI for concept." );
      uriCountsMap.keySet().stream().filter( k -> !uriRootsMap.containsKey( k ) ).forEach( k -> LOGGER.info( "No uriRoots entry for " + k ) );
      uriCountsMap.forEach( (k,v) -> LOGGER.info( "Uri Mentions: " + k + " : " + v ) );

      return mapUriQuotients( uris, uriCountsMap, uriRootsMap )
            .entrySet()
            .stream()
            .map( e -> new KeyValue<>( e.getKey(), e.getValue() ) )
            .sorted( Comparator.comparingDouble( KeyValue::getValue ) )
            .collect( Collectors.toList() );

   }

//   public List<KeyValue<String,Double>> getUriDistanceScores() {
//      final List<String> uris = createUriList();
//      final Map<String,Long> uriCountsMap = mapUriCounts( uris );
//      return mapUriDistanceQuotients( uris, uriCountsMap, getUriRootsMap() )
//            .entrySet()
//            .stream()
//            .map( e -> new KeyValue<>( e.getKey(), e.getValue() ) )
//            .sorted( Comparator.comparingDouble( KeyValue::getValue ) )
//            .collect( Collectors.toList() );
//   }

   static private Map<String,Long> mapUriCounts( final Collection<String> uris ) {
      return uris.stream()
                    .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }

   /**
    *
    * @param uris list of uris associated with some thing.  Order does not matter.
    * @return Map of Uris and their scores.
    */
   static private Map<String,Double> mapUriQuotients( final Collection<String> uris,
                                                      final Map<String,Long> uriCountsMap,
                                                      final Map<String,Collection<String>> uriRootsMap ) {
      final Map<String,Long> uriProducts = new HashMap<>();
      final Collection<String> uriSet = new HashSet<>( uris );
      for ( String uri : uriSet ) {
         final Collection<String> uriRoots = new HashSet<>( uriRootsMap.getOrDefault( uri, new HashSet<>() ) );
         uriRoots.add( uri );
         // How many of the roots of this uri are in the collection of uris for this concept (and how many times), multiplied
         final long product = uriRoots.stream()
                                      .filter( uriSet::contains )
                                      .mapToLong( uriCountsMap::get )
                                      .sum();
//                                     .reduce( 1, (a, b) -> a * b );
         uriProducts.put( uri, product );
         LOGGER.info( "URI " + uri + " has " + product
                      + " nodes in its path to root that are also distinct URIs for mentions in this ConceptAggregate." );
      }
      final long productSum = uriCountsMap.values().stream().mapToLong( l -> l ).sum();

      LOGGER.info( "Why we are using this number:  Each node is scored +1 for each Mention that it represents.\n" +
                   "For instance, assume BrCa has the path to root [root,Ca,BrCa].\n" +
                   "If we have mentions BrCa and Ca, node BrCa represents both of them with but with high specificity.\n" +
                   "Ca, while applicable to both mentions, is not as specific." );

      return uriProducts.keySet().stream()
                        .collect( Collectors.toMap( Function.identity(),
                              u -> computeQuotient( u, uriProducts, productSum ) ) );
//                              u -> computeQuotient( u, uriProducts ) ) );
   }

//   /**
//    *
//    * @param uris list of uris associated with some thing.  Order does not matter.
//    * @return Map of Uris and their scores.
//    */
//   static private Map<String,Double> mapUriDistanceQuotients( final List<String> uris,
//                                                              final Map<String,Long> uriCounts,
//                                                              final Map<String,Collection<String>> uriRootsMap ) {
//      final Map<String,Long> uriDistanceProducts = new HashMap<>();
//      for ( Map.Entry<String,Collection<String>> uriRoots : uriRootsMap.entrySet() ) {
//         final String uri = uriRoots.getKey();
//         final Collection<String> allUris = new HashSet<>( uriRoots.getValue() );
//         allUris.add( uri );
//         final long product = allUris.stream()
//                                     .filter( uris::contains )
//                                     .mapToLong( uriCounts::get )
//                                     .reduce( 1, (a, b) -> a * b );
//         final long distance = uriRoots.getValue().size();
//         uriDistanceProducts.put( uri, distance + product );
//      }
//      return uriDistanceProducts.keySet().stream()
//                                .collect( Collectors.toMap( Function.identity(),
//                                      u -> computeQuotient( u, uriDistanceProducts ) ) );
//   }

   static private double computeQuotient( final String uri, final Map<String,Long> uriProducts, final long productsSum ) {
      if ( uriProducts.size() == 1 ) {
         return 1d;
      }

      LOGGER.info( "Representation Quotient for " + uri + " : " + uriProducts.get( uri ) + " / " + productsSum + " = " + Double.valueOf( uriProducts.get( uri ) ) / productsSum );

      return Double.valueOf( uriProducts.get( uri ) ) / productsSum;
   }


   static private double computeQuotient( final String uri, final Map<String,Long> uriProducts ) {
      if ( uriProducts.size() == 1 ) {
         return 1d;
      }

      LOGGER.info( "Quotient for " + uri + " : " + uriProducts.get( uri ) + " / " + sumAllExcept( uri, uriProducts ) + " = " + Double.valueOf( uriProducts.get( uri ) ) / sumAllExcept( uri, uriProducts ) );

      return Double.valueOf( uriProducts.get( uri ) ) / sumAllExcept( uri, uriProducts );
   }


   static private long sumAllExcept( final String exceptUri, final Map<String,Long> uriProducts ) {
      return uriProducts.entrySet().stream()
                        .filter( e -> !e.getKey().equals( exceptUri ) )
                        .mapToLong( Map.Entry::getValue )
                        .sum();
   }

   static private long sumAll( final Map<String,Long> uriProducts ) {
      return uriProducts.values().stream()
                        .mapToLong( l -> l )
                        .sum();
   }





}
