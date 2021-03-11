package org.healthnlp.deepphe.summary.attribute.laterality;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.CodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;

import java.util.*;

public class LateralityCodeInfoStore implements CodeInfoStore {

   public String _bestCode;


   public void init( final UriInfoStore uriInfoStore ) {
      _bestCode = getBestLateralityCode( uriInfoStore._uriStrengths );
   }

   public String getBestCode() {
      return _bestCode;
   }

   static private String getBestLateralityCode( final Map<String,Integer> uriStrengths ) {
      if ( uriStrengths.isEmpty() ) {
         return "0";
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
      if ( topUris.contains( UriConstants.BILATERAL ) ) {
         return "4";
      }
      if ( topUris.contains( UriConstants.RIGHT ) ) {
         if ( topUris.contains( UriConstants.LEFT ) ) {
            return "4";
         }
         return "1";
      }
      if ( topUris.contains( UriConstants.LEFT ) ) {
         return "2";
      }
      // What else could it be?
      return "0";
   }


}
