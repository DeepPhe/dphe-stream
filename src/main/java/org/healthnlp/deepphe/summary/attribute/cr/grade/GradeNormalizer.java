package org.healthnlp.deepphe.summary.attribute.cr.grade;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class GradeNormalizer extends AbstractAttributeNormalizer {

   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "Grade best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      if ( aggregates.isEmpty() ) {
         // The Cancer Registry default is 9.
         return "9";
      }
      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
      int bestCode = -1;
      long bestCodesCount = 0;
      for ( Map.Entry<Integer,Long> codeCount : intCountMap.entrySet() ) {
         final long count = codeCount.getValue();
         if ( codeCount.getKey() > bestCode ) {
            bestCode = codeCount.getKey();
            bestCodesCount = count;
         }
      }
      setBestCodesCount( (int)bestCodesCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( intCountMap.size() );
      NeoplasmSummaryCreator.addDebug( "GradeNormalizer "
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

   protected int getIntCode( final String uri ) {
      if ( uri.startsWith( "Gleason_Score_" ) ) {
         if ( uri.endsWith( "6" ) ) {
            // well differentiated
            return 1;
         } else if ( uri.endsWith( "7" ) ) {
            // moderately differentiated
            return 2;
         } else if ( uri.endsWith( "8" )
                     || uri.endsWith( "9" )
                     || uri.endsWith( "10" ) ) {
            // poorly differentiated
            return 3;
         } else {
            return -1;
         }
         // There is a Tumor_Grade_G0
      } else if ( uri.equals( "Grade_1" ) || uri.equals( "Tumor_Grade_G1" )
                  || uri.equals( "Low_Grade" )
                  || uri.equals( "Low_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Well_Differentiated" ) ) {
         return 1;
      } else if ( uri.equals( "Grade_2" ) || uri.equals( "G2_Grade" )
                  || uri.equals( "Intermediate_Grade" )
                  || uri.equals( "Intermediate_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Moderately_Differentiated" ) ) {
         return 2;
      } else if ( uri.equals( "Grade_3" ) || uri.equals( "Tumor_Grade_G3" )
                  || uri.equals( "High_Grade" )
                  || uri.equals( "High_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Poorly_Differentiated" ) ) {
         return 3;
      } else if ( uri.equals( "Grade_4" ) || uri.equals( "G4_Grade" )
                  || uri.equals( "Undifferentiated" ) ) {
//                  || uri.equals( "Anaplastic" ))
         return 4;
      } else if ( uri.equals( "Grade_5" ) ) {
         return 5;
      }
      return -1;
   }


   protected void fillEvidenceMap( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies) {
      useAllEvidenceMap( infoCollector, dependencies );
   }

}
