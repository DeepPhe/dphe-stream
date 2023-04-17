package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.crc;

import org.healthnlp.deepphe.summary.attribute.cr.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class AnusInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<ConceptAggregateRelation> getAllRelations() {
      return getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) )
                          .stream()
                  .filter( AnusInfoCollector::hasAnusTarget )
                  .collect( Collectors.toSet() );
   }

   static private boolean hasAnusTarget( final ConceptAggregateRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return CrcUriCollection.getInstance().getAllAnusUris().contains( uri );
   }

}
