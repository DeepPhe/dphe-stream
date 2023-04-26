package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.ovary;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;

/**
 * https://training.seer.cancer.gov/ovarian/abstract-code-stage/codes.html
 *
 * C57.0	Fallopian tube
 * C57.1	Broad Ligament
 * C57.2	Round ligament
 * C57.3	Parametrium
 * C57.4	Uterine adnexa
 * C57.7	Other specified parts of female genital organs
 * C57.8	Overlapping lesion of female genital organs
 * C57.9	Female genital tract, NOS
 *
 * @author SPF , chip-nlp
 * @since {4/14/2023}
 */
final public class GenitaliaNormalizer extends AbstractAttributeNormalizer {


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
//      NeoplasmSummaryCreator.addDebug( "GenitaliaNormalizer "
//                                       + intCountMap.entrySet().stream()
//                                                    .map( e -> e.getKey() + ":" + e.getValue() )
//                                                    .collect( Collectors.joining(",") ) + " = "
//                                       + bestIntCode +"\n");
//      return bestIntCode+"";
//   }


   /**
    *  * C57.0	Fallopian tube
    *  * C57.1	Broad Ligament
    *  * C57.2	Round ligament
    *  * C57.3	Parametrium
    *  * C57.4	Uterine adnexa
    *  * C57.7	Other specified parts of female genital organs
    *  * C57.8	Overlapping lesion of female genital organs
    *  * C57.9	Female genital tract, NOS
    * @param uri -
    * @return -
    */
   public int getIntCode( final String uri ) {
      if ( OvaryUriCollection.getInstance().getFallopianTubeUris().contains( uri ) ) {
         return 0;
      }
      if ( OvaryUriCollection.getInstance().getBroadLigamentUris().contains( uri ) ) {
         return 1;
      }
      if ( OvaryUriCollection.getInstance().getRoundLigamentUris().contains( uri ) ) {
         return 2;
      }
      if ( OvaryUriCollection.getInstance().getParametriumUris().contains( uri ) ) {
         return 3;
      }
      if ( OvaryUriCollection.getInstance().getUterineAdnexaUris().contains( uri ) ) {
         return 4;
      }
      if ( OvaryUriCollection.getInstance().getOtherGenitalUris().contains( uri ) ) {
         return 7;
      }
      // No overlapping for 8
//      if ( OvaryUriCollection.getInstance().getAllGenitalUris().contains( uri ) ) {
//         return 9;
//      }
      return -1;
   }


}
