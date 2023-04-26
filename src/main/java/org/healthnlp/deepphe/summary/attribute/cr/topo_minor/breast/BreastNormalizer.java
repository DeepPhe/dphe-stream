package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.breast;

import org.healthnlp.deepphe.nlp.uri.CustomUriRelations;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {3/26/2023}
 */
public class BreastNormalizer extends AbstractAttributeNormalizer {


   static private final Collection<String> CENTERS
         = Arrays.asList( "_12_O_clock", "_3_O_clock", "_6_O_clock", "_9_O_clock" );
   static private final Collection<String> C_12_3
         = Arrays.asList( "_12_30_O_clock", "_1_O_clock", "_1_30_O_clock", "_2_O_clock", "_2_30_O_clock" );
   static private final Collection<String> C_3_6
         = Arrays.asList( "_3_30_O_clock", "_4_O_clock", "_4_30_O_clock", "_5_O_clock", "_5_30_O_clock" );
   static private final Collection<String> C_6_9
         = Arrays.asList( "_6_30_O_clock", "_7_O_clock", "_7_30_O_clock", "_8_O_clock", "_8_30_O_clock" );
   static private final Collection<String> C_9_12
         = Arrays.asList( "_9_30_O_clock", "_10_O_clock", "_10_30_O_clock", "_11_O_clock", "_11_30_O_clock" );

   static private Collection<String> QUADRANT_URIS;


   private String _lateralityCode = "";

   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      if ( QUADRANT_URIS == null ) {
         QUADRANT_URIS = CustomUriRelations.getInstance()
                                           .getQuadrantUris();
      }
      _lateralityCode = dependencies.getOrDefault( "laterality", "" );
      super.init( infoCollector, dependencies );
//      NeoplasmSummaryCreator.addDebug( "Breast best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getBestCode( final AttributeInfoCollector infoCollector ) {
      return getBestIntCode( infoCollector.getAllRelations() );
   }



//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // The Cancer Registry default is 9.
//         return "9";
//      }
//      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
//      final List<Integer> bestCodes = getBestIntCodes( intCountMap );
//      bestCodes.sort( Comparator.reverseOrder() );
//      final int bestIntCode = bestCodes.get( 0 );
//      long bestCount = intCountMap.get( bestIntCode );
//      setBestCodesCount( (int)bestCount );
//      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( intCountMap.size() );
//      NeoplasmSummaryCreator.addDebug( "BreastNormalizer "
//                                       + intCountMap.entrySet().stream()
//                                                    .map( e -> e.getKey() + ":" + e.getValue() )
//                                                    .collect( Collectors.joining(",") ) + " = "
//                                       + bestIntCode +"\n");
//      return bestIntCode < 0 ? "9" : bestIntCode+"";
//   }


   @Override
   public int getIntCode( final String uri ) {
      return getBreastCode( uri, _lateralityCode );
   }

   static private int getBreastCode( final String uri, final String lateralityCode ) {
//      https://training.seer.cancer.gov/breast/anatomy/quadrants.html
//      https://training.seer.cancer.gov/breast/abstract-code-stage/codes.html
      if ( uri.contains( "Nipple" ) ) {
         return 0;
      } else if ( uri.startsWith( "Central_" )
                  || uri.contains( "Areola" )
                  || uri.contains( "Subareolar" )) {
         return 1;
      } else if ( uri.startsWith( "Upper_inner_Quadrant" ) || uri.startsWith( "Upper_Inner_Quadrant" ) ) {
         // Somehow "Upper_inner_Quadrant" ended up with a lower-case 'i'
         return 2;
      } else if ( uri.startsWith( "Lower_Inner_Quadrant" ) ) {
         return 3;
      } else if ( uri.startsWith( "Upper_Outer_Quadrant" ) ) {
         return 4;
      } else if ( uri.startsWith( "Lower_Outer_Quadrant" ) ) {
         return 5;
      } else if ( uri.contains( "Axillary_Tail" ) ) {
         return 6;
      } else if ( CENTERS.contains( uri ) ) {
         return 8;
      } else if ( C_12_3.contains( uri ) ) {
         if ( lateralityCode.equals( "1" ) ) {
            return 2;
         } else if ( lateralityCode.equals( "2" ) ) {
            return 4;
         }
      } else if ( C_3_6.contains( uri ) ) {
         if ( lateralityCode.equals( "1" ) ) {
            return 3;
         } else if ( lateralityCode.equals( "2" ) ) {
            return 5;
         }
      } else if ( C_6_9.contains( uri ) ) {
         if ( lateralityCode.equals( "1" ) ) {
            return 5;
         } else if ( lateralityCode.equals( "2" ) ) {
            return 3;
         }
      } else if ( C_9_12.contains( uri ) ) {
         if ( lateralityCode.equals( "1" ) ) {
            return 4;
         } else if ( lateralityCode.equals( "2" ) ) {
            return 2;
         }
      }
      return -1;
   }

//   ICDO Codes for Breast:
//   //   C50.0	Nipple
////C50.1	Central portion of breast
////C50.2	Upper-inner quadrant of breast (UIQ)
////C50.3	Lower-inner quadrant of breast (LIQ)
////C50.4	Upper-outer quadrant of breast (UOQ)
////C50.5	Lower-outer quadrant of breast (LOQ)
////C50.6	Axillary tail of breast
////C50.8	Overlapping lesion of breast
////C50.9	Breast, NOS (excludes Skin of breast C44.5);
// multi-focal neoplasm in more than one quadrant of the breast.

//   Branch Uris for Breast_Quadrant
//Upper_inner_Quadrant 4
//Lower_Inner_Quadrant_Of_Left_Breast 5
//Upper_Outer_Quadrant_Of_Right_Breast 5
//Lower_Outer_Quadrant 4
//Lower_Inner_Quadrant_Of_Right_Breast 5
//Lower_Outer_Quadrant_Of_Male_Breast 4
//Upper_Outer_Quadrant 4
//Lower_Inner_Quadrant 4
//Upper_Inner_Quadrant_Of_Right_Breast 5
//Upper_Inner_Quadrant_Of_Left_Breast 5
//Lower_Outer_Quadrant_Of_Left_Breast 5
//Upper_Outer_Quadrant_Of_Male_Breast 4
//Upper_Inner_Quadrant_Of_Male_Breast 4
//Upper_Outer_Quadrant_Of_Left_Breast 5
//Lower_Inner_Quadrant_Of_Male_Breast 4
//Lower_Outer_Quadrant_Of_Right_Breast 5
//Breast_Quadrant 3
//      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Nipple" ) );
//      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Areola" ) );
//      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Central_Portion_Of_The_Breast" ) );
//      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Subareolar_Region" ) );
//   QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Axillary_Tail_Of_The_Breast" ) );

}
