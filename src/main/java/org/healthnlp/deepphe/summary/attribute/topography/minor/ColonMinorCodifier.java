package org.healthnlp.deepphe.summary.attribute.topography.minor;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @since {3/30/2022}
 */
final public class ColonMinorCodifier {

   private ColonMinorCodifier() {}

//   C18.0	Cecum
//C18.1	Appendix
//C18.2	Ascending colon; Right colon
//C18.3	Hepatic flexure of colon
//C18.4	Transverse colon
//C18.5	Splenic flexure of colon
//C18.6	Descending colon; Left colon
//C18.7	Sigmoid colon
//C18.8	Overlapping lesion of colon
//C18.9	Colon, NOS
//ICD-O-2/3	Term
//Rectosigmoid junction
//C19.9	Rectosigmoid junction
//ICD-O-2/3	Term
//Rectum
//C20.9	Rectum, NOS
//ICD-O-2/3	Term
//Anus and Anal canal
//C21.0	Anus, NOS (excludes Skin of anus and Perianal skin C44.5)
//C21.1	Anal canal
//C21.2	Cloacogenic zone
//C21.8	Overlapping lesion of rectum, anus and anal canal


   // TODO  Handle Rectosigmoid_Colon to be C19 instead of C18 - which it gets as a refinement of Colon
   // TODO  Handle Rectal_polyp or Rectal to be C20 instead of C18 - which it gets because Colon is frequently present


   static String getBestColon( final Map<String,Integer> uriStrengths, final String topographyMajor ) {
      if ( uriStrengths.isEmpty() ) {
         return topographyMajor.equals( "C18" ) ? "9" : "0";
      }
      final Map<Integer, List<String>> hitCounts = new HashMap<>();
      uriStrengths.forEach( (k,v) -> hitCounts.computeIfAbsent( v, l -> new ArrayList<>() )
                                              .add( k ) );
      if ( topographyMajor.equals( "C18" ) ) {
         if ( uriStrengths.containsKey( "Polyp_Of_Ascending_Colon" ) ) {
            // Kludge
            return "2";
         } else if ( uriStrengths.containsKey( "Polyp_Of_Descending_Colon" ) ) {
            return "6";
         }
         final int minor = hitCounts.keySet()
                         .stream()
                         .sorted( Comparator.comparingInt( Integer::intValue )
                                            .reversed() )
                         .map( hitCounts::get )
                         .map( ColonMinorCodifier::getBest18Code )
                         .filter( n -> n >= 0 )
                         .findFirst()
                         .orElse( -1 );
         if ( minor >= 0 ) {
            return "" + minor;
         }
         if ( uriStrengths.containsKey( "Colon" ) ) {
            if ( uriStrengths.containsKey( "Right" ) ) {
               // Have right laterality, since there is no uri for ascending colon assume 2.
               return "2";
            } else if ( uriStrengths.containsKey( "Left" ) ) {
               // Have left laterality, since there is no uri for descending colon assume 6.
               return "6";
            }
         }
         return "9";
      }
      return "" + hitCounts.keySet()
                      .stream()
                      .sorted( Comparator.comparingInt( Integer::intValue )
                                         .reversed() )
                      .map( hitCounts::get )
                           .map( ColonMinorCodifier::getBest21Code )
                           .filter( n -> n >= 0 )
                      .findFirst()
                      .orElse( 0 );
   }


   static public int getBest18Code( final Collection<String> uris ) {
      if ( uris.isEmpty() ) {
         return -1;
      }
      if ( uris.contains( "Cecum" ) ) {
         return 0;
      }
      if ( uris.contains( "Appendix" ) ) {
         return 1;
      }
      if ( uris.contains( "Transverse_Colon" ) ) {
         return 4;
      }
      if ( uris.contains( "Splenic_Flexure" ) ) {
         return 5;
      }
      if ( uris.contains( "Sigmoid_Colon" ) ) {
         return 7;
      }
      return -1;
   }

   static public int getBest21Code( final Collection<String> uris ) {
      if ( uris.isEmpty() ) {
         return -1;
      }
      if ( uris.contains( "Anal_Canal" ) ) {
         return 1;
      }
      if ( uris.contains( "Anus" ) && uris.contains( "Rectal" ) ) {
         return 8;
      }
      return -1;
   }


}
