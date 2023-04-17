package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.ovary;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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


   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "Female Genitalia best = " + getBestCode() + " counts= " + getUniqueCodeCount() +
                                       "\n" );
   }


   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      if ( aggregates.isEmpty() ) {
         // The Cancer Registry default is 9.
         return "9";
      }
      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
      final List<Integer> bestCodes = getBestIntCodes( intCountMap );
      bestCodes.sort( Comparator.reverseOrder() );
      final int bestIntCode = bestCodes.get( 0 );
      long bestCount = intCountMap.get( bestIntCode );
      setBestCodesCount( (int)bestCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( intCountMap.size() );
      NeoplasmSummaryCreator.addDebug( "GenitaliaNormalizer "
                                       + intCountMap.entrySet().stream()
                                                    .map( e -> e.getKey() + ":" + e.getValue() )
                                                    .collect( Collectors.joining(",") ) + " = "
                                       + bestIntCode +"\n");
      return bestIntCode+"";
   }

   public String getCode( final String uri ) {
      final int code = getIntCode( uri );
      return code+"";
   }

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
   protected int getIntCode( final String uri ) {
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
      return 9;
   }


}
