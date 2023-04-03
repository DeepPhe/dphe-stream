package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;
import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public interface AttributeInfoCollector {

   void init( CrConceptAggregate neoplasm );

   CrConceptAggregate getNeoplasm();

   Collection<String> getRelationTypes();

   default Collection<ConceptAggregateRelation> getAllRelations() {
      return getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) );
   }

   default Collection<ConceptAggregateRelation> getBestRelations() {
      final Collection<ConceptAggregateRelation> relations = getAllRelations();
      if ( relations.size() <= 2 ) {
         return relations;
      }
      return new ConfidenceGroup<>( relations ).getBest();
   }

   default Collection<CrConceptAggregate> getAllAggregates() {
      return getAllRelations().stream()
                               .map( ConceptAggregateRelation::getTarget )
                               .collect( Collectors.toSet() );
   }


   default Collection<CrConceptAggregate> getBestAggregates() {
      return getBestRelations().stream()
                               .map( ConceptAggregateRelation::getTarget )
                               .collect( Collectors.toSet() );
   }

   default Collection<String> getBestUris() {
      return getBestAggregates().stream()
                                .map( CrConceptAggregate::getUri )
                                .collect( Collectors.toSet() );
   }

   /**
    *
    * @return between 0 and 1
    */
   default double getConfidence() {
      if ( getBestRelations().isEmpty() ) {
         return 0;
      }
      final double best = getBestRelations().stream()
                                            .mapToDouble( ConceptAggregateRelation::getConfidence )
                                            .sum();
      final double all = getAllRelations().size();
      NeoplasmSummaryCreator.addDebug( "AttributeInfoCollector.getConfidence Relations "
                                                                        + getBestRelations()
                                             .stream()
                                             .mapToDouble( ConceptAggregateRelation::getConfidence )
                                                                                            .sorted()
                                                                             .mapToObj( d -> d + "" )
                                                                             .collect( Collectors.joining(",") )
                                                                        + " / " + all + " = " + (best/all) +"\n");
      return best / all;
   }

}
