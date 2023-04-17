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
 * C48.1	Specified Parts of Peritoneum
 * C48.2	Peritoneum NOS
 * C48.8	Overlapping lesion of Retroperitoneum and Peritoneum
 *
 * @author SPF , chip-nlp
 * @since {4/14/2023}
 */
final public class PeritoneumNormalizer extends AbstractAttributeNormalizer {


   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "Peritoneum best = " + getBestCode() + " counts= " + getUniqueCodeCount() +
                                       "\n" );
   }


   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      if ( aggregates.isEmpty() ) {
         // The Cancer Registry default is 2.
         return "2";
      }
      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
      final List<Integer> bestCodes = getBestIntCodes( intCountMap );
      bestCodes.sort( Comparator.reverseOrder() );
      final int bestIntCode = bestCodes.get( 0 );
      long bestCount = intCountMap.get( bestIntCode );
      setBestCodesCount( (int)bestCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( intCountMap.size() );
      NeoplasmSummaryCreator.addDebug( "PeritoneumNormalizer "
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

   protected int getIntCode( final String uri ) {
      if ( "Peritoneum".equals( uri ) ) {
         return 2;
      }
      if ( OvaryUriCollection.getInstance().getPeritoneumPartUris().contains( uri ) ) {
         return 1;
      }
      if ( OvaryUriCollection.getInstance().getOverlappingRpUris().contains( uri ) ) {
         return 8;
      }
      return 2;
   }


}
