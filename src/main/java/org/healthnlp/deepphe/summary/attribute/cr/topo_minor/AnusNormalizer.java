package org.healthnlp.deepphe.summary.attribute.cr.topo_minor;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class AnusNormalizer extends AbstractAttributeNormalizer {

   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "Anus best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }


   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      if ( aggregates.isEmpty() ) {
         // The Cancer Registry default is 0.
         return "0";
      }
      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
      final List<Integer> codeList = new ArrayList<>( intCountMap.keySet() );
      codeList.sort( Comparator.reverseOrder() );
      final int bestIntCode = codeList.get( 0 );
      long bestCount = intCountMap.get( bestIntCode );
      setBestCodesCount( (int)bestCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( intCountMap.size() );
      NeoplasmSummaryCreator.addDebug( "AnusNormalizer "
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
      if ( CrcUriCollection.getInstance().getAnalCanalUris().contains( uri ) ) {
         return 1;
      }
      if ( CrcUriCollection.getInstance().getCloacogenicZone().equals( uri ) ) {
         return 2;
      }
      if ( CrcUriCollection.getInstance().getAnorectalUri().equals( uri ) ) {
         return 8;
      }
      return 0;
   }

   //Anus and Anal canal
//C21.0	Anus, NOS (excludes Skin of anus and Perianal skin C44.5)
//C21.1	Anal canal
//C21.2	Cloacogenic zone
//C21.8	Overlapping lesion of rectum, anus and anal canal


}
