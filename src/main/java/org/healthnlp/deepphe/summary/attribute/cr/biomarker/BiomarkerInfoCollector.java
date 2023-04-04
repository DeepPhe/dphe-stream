package org.healthnlp.deepphe.summary.attribute.cr.biomarker;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class BiomarkerInfoCollector extends AbstractAttributeInfoCollector {

   private final Collection<String> _wantedUris = new HashSet<>();

   public void setWantedUris( final String... uris ) {
      _wantedUris.addAll( Arrays.asList( uris ) );
   }

   public Collection<String> getRelationTypes() {
      return Collections.singletonList( RelationConstants.has_Biomarker );
   }

   public Collection<ConceptAggregateRelation> getAllRelations() {
//      NeoplasmSummaryCreator.addDebug( "BiomarkerInfoCollector getting relations for "
//                                       + String.join( ",", _wantedUris ) +
//                                       " " + getNeoplasm()
//                                             .getRelations( getRelationTypes()
//                                                                  .toArray( new String[0] ) )
//                                             .stream().map( ConceptAggregateRelation::getTarget )
//                                             .map( CrConceptAggregate::getCoveredText ).collect( Collectors.joining(
//                                                   ",") ) + "  --> "
//                                       + getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) )
//                                                     .stream()
//                                                     .filter( hasWantedUri )
//                                                      .map( ConceptAggregateRelation::getTarget )
//                                                      .map( CrConceptAggregate::getCoveredText )
//                                                     .collect( Collectors.joining(",") )
//                                                     + "\n" );
      return getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) )
                          .stream()
                          .filter( hasWantedUri )
                          .collect( Collectors.toSet() );
   }


   private final Predicate<ConceptAggregateRelation> hasWantedUri
         = r -> r.getTarget()
                  .getAllUris()
                  .stream()
                  .anyMatch( _wantedUris::contains );


}
