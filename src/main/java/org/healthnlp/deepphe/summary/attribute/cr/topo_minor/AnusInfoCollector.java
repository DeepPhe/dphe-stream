package org.healthnlp.deepphe.summary.attribute.cr.topo_minor;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class AnusInfoCollector extends AbstractTopoMinorInfoCollector {



   public Collection<String> getRelationTypes() {
      return Collections.singletonList( RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE );
   }

   public Collection<ConceptAggregateRelation> getAllRelations() {
      return getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) )
                          .stream()
                  .filter( AnusInfoCollector::isAnusRelation )
                  .collect( Collectors.toSet() );
   }

   static private boolean isAnusRelation( final ConceptAggregateRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return CrcUriCollection.getInstance().getAllAnusUris().contains( uri );
   }

}
