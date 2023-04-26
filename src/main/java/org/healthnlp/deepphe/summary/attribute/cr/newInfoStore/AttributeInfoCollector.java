package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

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

   default Collection<CrConceptAggregate> getAllAggregates() {
      return getAllRelations().stream()
                               .map( ConceptAggregateRelation::getTarget )
                               .collect( Collectors.toSet() );
   }

}
