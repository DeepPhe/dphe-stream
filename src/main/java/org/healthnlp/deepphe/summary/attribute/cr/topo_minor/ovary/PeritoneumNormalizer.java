package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.ovary;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;

/**
 * https://training.seer.cancer.gov/ovarian/abstract-code-stage/codes.html
 *
 * C48.1	Specified Parts of Peritoneum
 * C48.2	Peritoneum NOS
 * C48.8	Overlapping lesion of Retroperitoneum and Peritoneum
 *
 * @author SPF , chip-nlp
 * @since {4/14/2023}
 */
final public class PeritoneumNormalizer extends AbstractAttributeNormalizer {


   public String getBestCode( final AttributeInfoCollector infoCollector ) {
      return getBestIntCode( infoCollector.getAllRelations() );
   }

   public String getDefaultTextCode() {
      return "2";
   }

//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // The Cancer Registry default is 2.
//         return "2";
//      }
//      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
//      final List<Integer> bestCodes = getBestIntCodes( intCountMap );
//      bestCodes.sort( Comparator.reverseOrder() );
//      final int bestIntCode = bestCodes.get( 0 );
//      long bestCount = intCountMap.get( bestIntCode );
//      setBestCodesCount( (int)bestCount );
//      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( intCountMap.size() );
//      NeoplasmSummaryCreator.addDebug( "PeritoneumNormalizer "
//                                       + intCountMap.entrySet().stream()
//                                                    .map( e -> e.getKey() + ":" + e.getValue() )
//                                                    .collect( Collectors.joining(",") ) + " = "
//                                       + bestIntCode +"\n");
//      return bestIntCode+"";
//   }


   public int getIntCode( final String uri ) {
      if ( "Peritoneum".equals( uri ) ) {
         return 2;
      }
      if ( OvaryUriCollection.getInstance().getPeritoneumPartUris().contains( uri ) ) {
         return 1;
      }
      if ( OvaryUriCollection.getInstance().getOverlappingRpUris().contains( uri ) ) {
         return 8;
      }
//      if ( OvaryUriCollection.getInstance().getAllPeritoneumUris().contains( uri ) ) {
//         return 2;
//      }
      return -1;
   }


}
