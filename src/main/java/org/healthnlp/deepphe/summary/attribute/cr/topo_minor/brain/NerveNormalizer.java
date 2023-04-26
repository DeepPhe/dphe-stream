package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.brain;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;

/**
 * @author SPF , chip-nlp
 * @since {4/5/2023}
 */
public class NerveNormalizer extends AbstractAttributeNormalizer {

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
//      NeoplasmSummaryCreator.addDebug( "NerveNormalizer "
//                                       + intCountMap.entrySet().stream()
//                                                    .map( e -> e.getKey() + ":" + e.getValue() )
//                                                    .collect( Collectors.joining( "," ) ) + " = "
//                                       + bestIntCode + "\n");
//      return bestIntCode < 0 ? "9" : bestIntCode+"";
//   }

   public int getIntCode( final String uri ) {
      if ( BrainUriCollection.getInstance().getSpinalCord().contains( uri ) ) {
         return 0;
      }
      if ( BrainUriCollection.getInstance().getCaudaEquina().contains( uri ) ) {
         return 1;
      }
      if ( BrainUriCollection.getInstance().getOlfactoryNerve().contains( uri ) ) {
         return 2;
      }
      if ( BrainUriCollection.getInstance().getOpticNerve().contains( uri ) ) {
         return 3;
      }
      if ( BrainUriCollection.getInstance().getAcousticNerve().contains( uri ) ) {
         return 4;
      }
      if ( BrainUriCollection.getInstance().getCranialNerve().contains( uri ) ) {
         return 5;
      }
//      if ( BrainUriCollection.getInstance().getOverlapping().contains( uri ) ) {
//         return 8;
//      }
//      if ( BrainUriCollection.getInstance().getCnsNOS().contains( uri ) ) {
//         return 9;
//      }
      return -1;
   }

}
