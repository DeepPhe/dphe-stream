package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.lung;

import org.healthnlp.deepphe.summary.attribute.cr.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class LungInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<ConceptAggregateRelation> getAllRelations() {
      return getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) )
                          .stream()
                          .filter( LungInfoCollector::hasLungTarget )
                          .collect( Collectors.toSet() );
   }

   static private boolean hasLungTarget( final ConceptAggregateRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return LungUriCollection.getInstance().getLungUris().contains( uri )
             || LungUriCollection.getInstance().getBronchusUris().contains( uri );
   }

}
