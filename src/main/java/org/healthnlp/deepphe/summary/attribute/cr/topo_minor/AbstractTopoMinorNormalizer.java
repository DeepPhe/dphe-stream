package org.healthnlp.deepphe.summary.attribute.cr.topo_minor;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {4/5/2023}
 */
abstract public class AbstractTopoMinorNormalizer extends AbstractAttributeNormalizer {


   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      if ( aggregates.isEmpty() ) {
         // The Cancer Registry default is 9.
         return "9";
      }
      final Map<Integer,Long> intCountMap = createAllIntCodeCountMap( aggregates );
      final List<Integer> bestCodes = getBestIntCodes( intCountMap );
      bestCodes.sort( Comparator.reverseOrder() );
      final int bestIntCode = bestCodes.get( 0 );
      long bestCount = intCountMap.get( bestIntCode );
      setBestCodesCount( (int)bestCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( intCountMap.size() );
      return bestIntCode < 0 ? "9" : bestIntCode+"";
   }

   public String getCode( final String uri ) {
      final int code = getIntCode( uri );
      return code < 0 ? "9" : code+"";
   }

}
