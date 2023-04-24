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
//                                                         .map( ConfidenceCalculator::getBumpedPerfection )
                                                          .collect( Collectors.toList() );
      NeoplasmSummaryCreator.addDebug( "ConfidenceCalculator.calculateAggregateRelation:  " +
                                       mentionRelations
                                             .stream()
                                             .map( MentionRelation::getConfidence )
//                                             .map( ConfidenceCalculator::getBumpedPerfection )
                                             .sorted()
                                             .map( d -> ""+d )
                                             .collect( Collectors.joining( "," ) )
                                       + " :\n(" + confidences.size() + ") "
                                       + getStandardNumerator( confidences ) + "/"
                                       + getHighDenominator( confidences.size() )
                                       + " = " + getStandardConfidence( confidences )
                                       +"   vs.  "
                                       + getStandardNumerator( confidences ) + "/"
                                       + getStandardDenominator( confidences.size() )
                                       + " = " + (getStandardNumerator( confidences )
                                                  / getStandardDenominator( confidences.size() ) )
                                       + "\n" );
      return getStandardConfidence( confidences );
   }

//   static public double calculateAsRelationTarget( final List<Double> confidences ) {
//      NeoplasmSummaryCreator.addDebug( "ConfidenceCalculator.calculateAsRelationTarget:  "
//                                       + confidences.stream().sorted()
//                                                                  .map( d -> d+"" )
//                                                                  .collect( Collectors.joining(",") )
//                                       + " :\n(" + confidences.size() + ") "
//                                       + getStandardNumerator( confidences ) + "/"
//                                       + getHighDenominator( confidences.size() )
//                                       + " = " + (getStandardNumerator( confidences )
//                                                  / getStandardDenominator( confidences.size() ) )
//                                       +"   vs.  "
//                                       + getStandardNumerator( confidences ) + "/"
//                                       + getStandardDenominator( confidences.size() )
//                                       + " = " + (getStandardNumerator( confidences )
//                                                  / getHighDenominator( confidences.size() ) )
//                                     + "\n" );
//      return getStandardConfidence( confidences );
//   }

   // TODO - use minimum value of zero and count of non-zero values?  Need to return 0 when count == 0;
   static public double getStandardConfidence( final List<Double> values ) {
      if ( values.isEmpty() ) {
         return 0;
      }
      final double numerator = getStandardNumerator( values );
      final double denominator = getHighDenominator( values.size() );
      return numerator / denominator;
   }

   static private double getStandardNumerator( final Collection<Double> values ) {
      // Value larger numbers much more than smaller numbers.
      return values.stream().mapToDouble( d -> d*d/100 ).sum();
   }

   // Ovary Laterality +.12, .04
   static private double getStandardDenominator( final int count ) {
      return count/2d + Math.sqrt( count );
   }

   // Ovary Topo Major +.07, .08  Grade +07, .04
   static private double getHighDenominator( final int count ) {
      return 2 * Math.sqrt( count );
   }

   /**
    *
    * @param relation mention relation in aggregate relation
    * @return mention relation confidence OR mention relation confidence + 10 if mention relation confidence >= 100.
    */
   static private double getBumpedPerfection( final MentionRelation relation ) {
      return relation.getConfidence() >= 100 ? relation.getConfidence() + 10 : relation.getConfidence();
   }

}
