package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.brain;

import org.healthnlp.deepphe.summary.attribute.cr.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/5/2023}
 */
public class MeningesInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<ConceptAggregateRelation> getAllRelations() {
      return getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) )
                          .stream()
                          .filter( MeningesInfoCollector::hasMeningesTarget )
                          .collect( Collectors.toSet() );
   }

   static private boolean hasMeningesTarget( final ConceptAggregateRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return BrainUriCollection.getInstance().getMeningesUris().contains( uri );
   }

}
