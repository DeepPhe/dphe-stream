package org.healthnlp.deepphe.summary.attribute;

import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

final public class MinorTopography implements SpecificAttribute {

   final private NeoplasmAttribute _neoplasmAttribute;

   public MinorTopography( final Collection<String> topographyCodes ) {
      _neoplasmAttribute = createMinorTopoAttribute( topographyCodes );
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _neoplasmAttribute;
   }


   private NeoplasmAttribute createMinorTopoAttribute( final Collection<String> topographyCodes ) {
      return SpecificAttribute.createAttribute( "topography_minor",
                                                getMinorTopoCode( topographyCodes ) );
   }

   static private String getMinorTopoCode( final Collection<String> topographyCodes ) {
      if ( topographyCodes.isEmpty() ) {
         return "9";
      }
      final Function<String, String> getMinor = t -> {
         final int dot = t.indexOf( '.' );
         return dot > 0 ? t.substring( dot + 1 ) : "";
      };
      final Collection<String> allMinors = topographyCodes.stream()
                                                .map( getMinor )
                                                .filter( t -> !t.isEmpty() )
                                                .distinct()
                                                .sorted()
                                                .collect( Collectors.toList() );
      if ( allMinors.size() > 1 ) {
         allMinors.remove( "9" );
      }
      String minors = String.join( ";", allMinors );
      if ( minors.isEmpty() ) {
//         LOGGER.info( "No specific site codes, using 9." );
         return "9";
      }
      return minors;
   }


}
