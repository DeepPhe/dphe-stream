package org.healthnlp.deepphe.summary.attribute.cr.laterality;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class LateralityNormalizer extends AbstractAttributeNormalizer {

   //  https://seer.cancer.gov/manuals/primsite.laterality.pdf
   //  Only the following major topographic sites can have an associated laterality.
   static private final Collection<String> LATERALITIES = new HashSet<>();
   static private final int[] FACILITY0 = new int[]{ 7, 8, 9 };
   static private final int[] FACILITY = new int[]{
         30, 31, 34, 38, 40, 41, 44, 47, 49, 50, 56, 57, 62, 63, 64, 65, 66, 69, 70, 71, 72, 74, 75
   };
   static {
      Arrays.stream( FACILITY0 ).forEach( n -> LATERALITIES.add( "C0" + n ) );
      Arrays.stream( FACILITY ).forEach( n -> LATERALITIES.add( "C" + n ) );
   }

   static private final double CONFIDENCE_CUTOFF = 0.2;


   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      String topographyMajor = dependencies.getOrDefault( "topography_major", "" )
                                           .toUpperCase();
      if ( topographyMajor.length() > 3 ) {
         topographyMajor = topographyMajor.substring( 0, 3 );
      }
      final boolean hasLaterality = LATERALITIES.contains( topographyMajor );
      if ( !hasLaterality ) {
         // 0 is "Not a paired Site"
         setBestCode( "0" );
         fillEvidenceMap( infoCollector, dependencies );
      } else if ( infoCollector.getConfidence() < CONFIDENCE_CUTOFF ) {
         // TODO - Math.abs( left confidence - right confidence )
         // A "Paired Site, but no confident information on laterality"
         setBestCode( "9" );
         fillEvidenceMap( infoCollector, dependencies );
      } else {
         super.init( infoCollector, dependencies );
      }
      NeoplasmSummaryCreator.addDebug( "Laterality best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }


   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      if ( aggregates.isEmpty() ) {
         return "9";
      }
      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
      int bestCode = -1;
      long bestCodesCount = 0;
      long bilateralCount = 0;
      long unilateralCount = 0;
      long unspecifiedCount = 0;
      for ( Map.Entry<Integer,Long> codeCount : intCountMap.entrySet() ) {
         final int code = codeCount.getKey();
         final long count = codeCount.getValue();
         if ( code == 4 ) {
            bilateralCount = count;
         }
         if ( count > bestCodesCount ) {
            if ( code == 3 ) {
               unilateralCount = count;
            } else if ( code == 9 ) {
               unspecifiedCount = count;
            } else {
               bestCode = code;
               bestCodesCount = count;
            }
         } else if ( count == bestCodesCount && code + bestCode == 3 ) {
            // Right and Left are equal, use Bilateral.
            bestCode = 4;
            bilateralCount += count;
         }
      }
      if ( bilateralCount > 0 ) {
         bestCode = 4;
         bestCodesCount = bilateralCount;
      } else if ( bestCode == 0 ) {
         if ( unilateralCount > 0 ) {
            bestCode = 3;
            bestCodesCount = unilateralCount;
         } else if ( unspecifiedCount > 0 ) {
            bestCode = 9;
            bestCodesCount = unspecifiedCount;
         }
      }
      setBestCodesCount( (int)bestCodesCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( intCountMap.size() );
      NeoplasmSummaryCreator.addDebug( "LateralityNormalizer "
                                       + intCountMap.entrySet().stream()
                                                 .map( e -> e.getKey() + ":" + e.getValue() )
                                                 .collect( Collectors.joining(",") ) + " = "
                                       + bestCode +"\n");
      return bestCode <= 0 ? "9" : bestCode+"";
   }

   public String getCode( final String uri ) {
      final int code = getIntCode( uri );
      return code <= 0 ? "" : code+"";
   }

   // https://seer.cancer.gov/archive/manuals/2021/SPCSM_2021_MainDoc.pdf
   protected int getIntCode( final String uri ) {
       if ( uri.equals( UriConstants.BILATERAL ) ) {
         return 4;
      }
      if ( uri.equals( UriConstants.RIGHT ) || uri.equals( "Unilateral_Right" ) ) {
         return 1;
      }
      if ( uri.equals( UriConstants.LEFT ) || uri.equals( "Unilateral_Left" ) ) {
         return 2;
      }
      if ( uri.equals( "Unilateral" )) {
         return 3;
      }
      if ( uri.equals( "Unspecified_Laterality" ) ) {
         return 9;
      }
      return -1;
   }


}
