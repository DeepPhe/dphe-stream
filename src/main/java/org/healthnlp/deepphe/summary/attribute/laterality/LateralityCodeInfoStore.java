package org.healthnlp.deepphe.summary.attribute.laterality;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;

public class LateralityCodeInfoStore {

   public String _bestLateralityCode;


   protected void init( final Collection<ConceptAggregate> neoplasms,
                        final UriInfoStore uriInfoStore ) {
      _bestLateralityCode = getBestLateralityCode( uriInfoStore._uriStrengths );
   }

   static private String getBestLateralityCode( final Map<String,Integer> uriStrengths ) {
      if ( uriStrengths.isEmpty() ) {
         return "";
      }
      final Map<Integer,List<String>> hitCounts = new HashMap<>();
      for ( Map.Entry<String,Integer> uriStrength : uriStrengths.entrySet() ) {
         hitCounts.computeIfAbsent( uriStrength.getValue(), u -> new ArrayList<>() )
                  .add( uriStrength.getKey() );
      }
      final List<String> topUris = hitCounts.entrySet()
                                            .stream()
                                            .max( Comparator.comparingInt( Map.Entry::getKey ) )
                                            .map( Map.Entry::getValue )
                                            .orElse( Collections.emptyList() );
      if ( topUris.contains( UriConstants.RIGHT ) ) {
         return "1";
      }
      if ( topUris.contains( UriConstants.LEFT ) ) {
         return "2";
      }
      if ( topUris.contains( UriConstants.BILATERAL ) ) {
         return "4";
      }
      // What else could it be?
      return "";
   }


}
