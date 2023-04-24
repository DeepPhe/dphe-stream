package org.healthnlp.deepphe.summary.attribute.cr.laterality;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;
import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class LateralityInfoCollector extends AbstractAttributeInfoCollector {

   public Collection<String> getRelationTypes() {
      return Collections.singletonList( RelationConstants.HAS_LATERALITY );
   }


   /**
    *
    * @return between 0 and 1
    */
   public double getConfidence() {
      final Collection<ConceptAggregateRelation> allRelations = getAllRelations();
      final ConfidenceGroup<ConceptAggregateRelation> group = new ConfidenceGroup<>( allRelations );
      final Collection<ConceptAggregateRelation> bestRelations = group.getBest();
      if ( bestRelations.isEmpty() ) {
         NeoplasmSummaryCreator.addDebug( "LateralityInfoCollector.getConfidence No Relations\n" );
         return 0;
      }
      final double bestConfidence = bestRelations.stream()
                                                 .mapToDouble( ConceptAggregateRelation::getConfidence )
                                                 .sum();
      NeoplasmSummaryCreator.addDebug( "LateralityInfoCollector.getConfidence BestRelations "
                                       + bestRelations
                                             .stream()
                                             .mapToDouble( ConceptAggregateRelation::getConfidence )
                                             .sorted()
                                             .mapToObj( d -> d + "" )
                                             .collect( Collectors.joining( "," ) )
                                       + " = " + bestConfidence + "\n" );
      final Collection<ConceptAggregateRelation> nextRelations = group.getNext();
      if ( nextRelations.isEmpty() ) {
         return bestConfidence;
      }
      final double nextConfidence = nextRelations.stream()
                                                 .mapToDouble( ConceptAggregateRelation::getConfidence )
                                                 .sum();
      NeoplasmSummaryCreator.addDebug( "LateralityInfoCollector.getConfidence NextRelations "
                                       + nextRelations
                                             .stream()
                                             .mapToDouble( ConceptAggregateRelation::getConfidence )
                                             .sorted()
                                             .mapToObj( d -> d + "" )
                                             .collect( Collectors.joining( "," ) )
                                       + " = " + nextConfidence + "\n" );
      final Collection<ConceptAggregateRelation> otherRelations = group.getOther();
      double restConfidence = nextConfidence;
      double aveRestConfidence = restConfidence;
      if ( !otherRelations.isEmpty() ) {
         final Collection<ConceptAggregateRelation> restRelations = new HashSet<>( nextRelations );
         restRelations.addAll( otherRelations );
         restConfidence = restRelations.stream()
                                       .mapToDouble( ConceptAggregateRelation::getConfidence )
                                       .sum();
         aveRestConfidence = restConfidence / restRelations.size();
         NeoplasmSummaryCreator.addDebug( "LateralityInfoCollector.getConfidence RestRelations "
                                          + restRelations
                                                .stream()
                                                .mapToDouble( ConceptAggregateRelation::getConfidence )
                                                .sorted()
                                                .mapToObj( d -> d + "" )
                                                .collect( Collectors.joining( "," ) )
                                          + " = " + restConfidence + " ave = " + aveRestConfidence + "\n" );
      }

      final double allConfidence = allRelations.stream()
                                                .mapToDouble( ConceptAggregateRelation::getConfidence )
                                                .sum();
      final double aveAllConfidence = allConfidence / allRelations.size();
      NeoplasmSummaryCreator.addDebug( "LateralityInfoCollector.getConfidence AllRelations  "
                                       + allRelations
                                             .stream()
                                             .mapToDouble( ConceptAggregateRelation::getConfidence )
                                             .sorted()
                                             .mapToObj( d -> d + "" )
                                             .collect( Collectors.joining( "," ) )
                                       + " = " + allConfidence + " ave = " + aveAllConfidence + "\n" );
      NeoplasmSummaryCreator.addDebug( "LateralityInfoCollector.getConfidence Possibilities "
                                       + "  best-rest = " + (bestConfidence - restConfidence)
                                       + "  all-best = " + (allConfidence - bestConfidence)
                                       + "  rest-best = " + (restConfidence - bestConfidence)
                                       + "  best-aveRest = " + (bestConfidence - aveRestConfidence)
                                       + "  best-aveAll = " + (bestConfidence - aveAllConfidence)

                                       + "\n* best/rest = " + (bestConfidence/restConfidence)
                                       + "  best/all = " + (bestConfidence/allConfidence)
                                       + "  best/aveRest = " + (bestConfidence/aveRestConfidence)
                                       + "  best/aveAll = " + (bestConfidence/aveAllConfidence)

                                       + "\nall-best/all = " + ((allConfidence-bestConfidence)/allConfidence)
                                       + "  best-rest/all = " + ((bestConfidence-restConfidence)/allConfidence)
                                       + "  all-best/best = " + ((allConfidence-bestConfidence)/bestConfidence)
                                       + "  best-rest/best = " + ((bestConfidence-restConfidence)/bestConfidence)

                                       + "\nbest-rest/aveRest = " + ((bestConfidence-restConfidence)/aveRestConfidence)
                                       + "  best-aveRest/all = " + ((bestConfidence-aveRestConfidence)/aveRestConfidence)
                                       + "  best-aveAll/all = " + ((bestConfidence-aveAllConfidence)/aveRestConfidence)
                                       + "  best-aveRest/aveRest = " + ((bestConfidence-aveRestConfidence)/aveRestConfidence)
                                       + "  best-aveRest/aveAll = " + ((bestConfidence-aveRestConfidence)/aveAllConfidence)
                                       + "\n" );
      return bestConfidence/allConfidence;
   }


}
