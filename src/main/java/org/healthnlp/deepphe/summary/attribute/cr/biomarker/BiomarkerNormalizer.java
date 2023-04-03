package org.healthnlp.deepphe.summary.attribute.cr.biomarker;

import org.apache.ctakes.core.util.StringUtil;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class BiomarkerNormalizer extends AbstractAttributeNormalizer {


   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "Biomarker best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      // Map of covered text to the number of times that
      final ConfidenceGroup<CrConceptAggregate> confidenceGroup = new ConfidenceGroup<>( aggregates );
      final Map<String,Long> countMap = confidenceGroup.getBest()
                                                           .stream()
                                                            .map( this::getCodes )
                                                           .flatMap( Collection::stream )
                                                            .collect( Collectors.groupingBy( Function.identity(),
                                                                                    Collectors.counting() ) );
      final Collection<String> bestCodes = new HashSet<>();
      long bestCodesCount = 0;
      long totalCount = 0;
      for ( Map.Entry<String,Long> codeCount : countMap.entrySet() ) {
         final long count = codeCount.getValue();
         if ( count > bestCodesCount ) {
            bestCodes.clear();
            bestCodes.add( codeCount.getKey() );
            bestCodesCount = count;
         } else if ( count == bestCodesCount ) {
            bestCodes.add( codeCount.getKey() );
            bestCodesCount += count;
         }
         totalCount += count;
      }
      setBestCodesCount( (int)bestCodesCount );
      setAllCodesCount( (int)totalCount );
      setUniqueCodeCount( countMap.size() );
      NeoplasmSummaryCreator.addDebug( "BiomarkerNormalizer "
                                       + countMap.entrySet().stream()
                                                 .map( e -> e.getKey() + ":" + e.getValue() )
                                                 .collect( Collectors.joining(",") ) + " = "
                                       + String.join( ";", bestCodes ) +"\n");
      return String.join( ";", bestCodes );
   }

   public String getCode( final CrConceptAggregate aggregate ) {
      return String.join( ";", getCodes( aggregate ) );
   }

   public String getCode( final String uri ) {
      // Use the aggregate form instead.
      return "";
   }

   public List<String> getCodes( final CrConceptAggregate aggregate ) {
      final String text = aggregate.getCoveredText()
                                   .replace( '[', ' ' )
                                   .replace( ']', ' ' )
                                   .trim()
                                   .toLowerCase();
      return Arrays.asList( StringUtil.fastSplit( text, ',' ) );
   }

}
