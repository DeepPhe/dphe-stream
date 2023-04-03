package org.healthnlp.deepphe.summary.concept;

import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/29/2023}
 */
final public class ConfidenceCalculator {

   private ConfidenceCalculator() {}

   static public double calculateAggregateRelation( final Collection<MentionRelation> mentionRelations ) {
      final List<Double> confidences = mentionRelations.stream()
                                                             .map( MentionRelation::getConfidence )
                                                             .collect( Collectors.toList() );
      NeoplasmSummaryCreator.addDebug( "ConfidenceCalculator.calculateAggregateRelation:  " +
                                       mentionRelations
                                             .stream()
                                             .map( MentionRelation::getConfidence )
                                             .sorted()
                                             .map( d -> ""+d )
                                             .collect( Collectors.joining( "," ) )
                                       + " : " + getStandardNumerator( confidences ) + "/"
                                       + getStandardDenominator( confidences.size() )
                                       + " = " + getStandardConfidence( confidences ) +"\n");
      return getStandardConfidence( confidences );
   }

   static public double calculateAsRelationTarget( final List<Double> confidences ) {
      NeoplasmSummaryCreator.addDebug( "ConfidenceCalculator.calculateAsRelationTarget:  "
                                       + confidences.stream().sorted()
                                                                  .map( d -> d+"" )
                                                                  .collect( Collectors.joining(",") )
                                       + " : " + getStandardNumerator( confidences ) + "/"
                                       + getStandardDenominator( confidences.size() )
                                       + " = " + getStandardConfidence( confidences ) +"\n");
      return getStandardConfidence( confidences );
   }

   static public double getStandardConfidence( final List<Double> values ) {
      if ( values.isEmpty() ) {
         return 0;
      }
      final double numerator = getStandardNumerator( values );
      final double denominator = getStandardDenominator( values.size() );
      return Math.min( 100, (numerator / denominator) );
   }

   static public double getStandardNumerator( final Collection<Double> values ) {
      // Value larger numbers much more than smaller numbers.
//      return values.stream().mapToDouble( d -> d*d/100 ).sum();
      return values.stream().mapToDouble( d -> d*d/100 ).sum();
   }

   static public double getStandardDenominator( final int count ) {
      return count/2d + Math.sqrt( count );
   }

   static public double getBy100StandardConfidence( final List<Double> values ) {
      if ( values.isEmpty() ) {
         return 0;
      }
      final double numerator = getStandardNumerator( values );
      final double denominator = getStandardDenominator( values.size() );
      return Math.min( 1, (numerator / denominator) / 100 );
   }

   static public double getBy100StandardNumerator( final Collection<Double> values ) {
      // Value larger numbers much more than smaller numbers.
//      return values.stream().mapToDouble( d -> d*d/100 ).sum();
      return values.stream().mapToDouble( d -> 100*d ).map( d -> d*d/100 ).sum();
   }


}
