package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.lung;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * C34.0	Main bronchus
 * C34.1	Upper lobe, lung
 * C34.2	Middle lobe, lung (right lung only)
 * C34.3	Lower lobe, lung
 * C34.8	Overlapping lesion of lung
 * C34.9	Lung, NOS
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class LungNormalizer extends AbstractAttributeNormalizer {

   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "Lung best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      if ( aggregates.isEmpty() ) {
         // The Cancer Registry default is 9.
         return "9";
      }
      int bestIntCode = -1;
      long bestCount = 0;
      int uniqueCount = 0;
      final ConfidenceGroup<CrConceptAggregate> confidenceGroup = new ConfidenceGroup<>( aggregates );
      final Map<Integer,Long> bestCountMap = confidenceGroup.getBest()
                                                            .stream()
                                                            .map( CrConceptAggregate::getUri )
                                                            .map( LungNormalizer::getUriLungNumber )
                                                            .filter( c -> c >= 0 )
                                                            .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
      if ( !bestCountMap.isEmpty() ) {
         final List<Integer> bestCodes = getBestIntCodes( bestCountMap );
         bestCodes.sort( Comparator.reverseOrder() );
         bestIntCode = bestCodes.get( 0 );
         bestCount = bestCountMap.get( bestIntCode );
         uniqueCount += bestCountMap.size();
      }
      final Map<Integer,Long> otherCountMap = confidenceGroup.getBest()
                                                             .stream()
                                                             .map( CrConceptAggregate::getUri )
                                                             .map( LungNormalizer::getOtherUriLungNumber )
                                                             .filter( c -> c >= 0 )
                                                             .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
      if ( !otherCountMap.isEmpty() ) {
         if ( bestIntCode < 0 ) {
            final List<Integer> bestCodes = getBestIntCodes( otherCountMap );
            bestCodes.sort( Comparator.reverseOrder() );
            bestIntCode = bestCodes.get( 0 );
            bestCount = otherCountMap.get( bestIntCode );
         }
         uniqueCount += otherCountMap.size();
      }
      setBestCodesCount( (int)bestCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( uniqueCount );
      NeoplasmSummaryCreator.addDebug( "LungNormalizer "
                                       + bestCountMap.entrySet().stream()
                                                     .map( e -> e.getKey() + ":" + e.getValue() )
                                                     .collect( Collectors.joining(",") ) + " ; "
                                       + otherCountMap.entrySet().stream()
                                                      .map( e -> e.getKey() + ":" + e.getValue() )
                                                      .collect( Collectors.joining(",") ) + " ; "
                                       + bestIntCode +"\n" );
      return bestIntCode < 0 ? "9" : bestIntCode+"";
   }

   public String getCode( final String uri ) {
      final int code = getIntCode( uri );
      return code < 0 ? "" : code+"";
   }

   protected int getIntCode( final String uri ) {
      int lungNumber = getUriLungNumber( uri );
      if ( lungNumber >= 0 ) {
         return lungNumber;
      }
      int otherNumber = getOtherUriLungNumber( uri );
      if ( otherNumber >= 0 ) {
         return otherNumber;
      }
      return -1;
   }


   static private int getUriLungNumber( final String uri ) {
      if ( LungUriCollection.getInstance().getUpperLobeUris().contains( uri ) ) {
         return 1;
      } else if ( LungUriCollection.getInstance().getMiddleLobeUris().contains( uri ) ) {
         return 2;
      } else if ( LungUriCollection.getInstance().getLowerLobeUris().contains( uri ) ) {
         return 3;
      }
      return -1;
   }

   static private int getOtherUriLungNumber( final String uri ) {
      if ( LungUriCollection.getInstance().getBronchusUris().contains( uri ) ) {
         return 0;
//      } else if ( LungUriCollection.getInstance().getLungUris().contains( uri ) ) {
//         return 9;
      }
      return -1;
   }


}
