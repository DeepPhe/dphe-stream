package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.ovary;

import org.healthnlp.deepphe.summary.attribute.cr.topo_minor.AbstractTopoMinorInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 *
 * NOTE:  Peritoneum is treated as a tissue, therefore this will never be reached.
 *
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public class PeritoneumInfoCollector extends AbstractTopoMinorInfoCollector {


   public Collection<ConceptAggregateRelation> getAllRelations() {
      return getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) )
                          .stream()
                  .filter( PeritoneumInfoCollector::hasPeritoneumTarget )
                  .collect( Collectors.toSet() );
   }

   static private boolean hasPeritoneumTarget( final ConceptAggregateRelation relation ) {
      final String uri = relation.getTarget().getUri();
      return OvaryUriCollection.getInstance().getAllPeritoneumUris().contains( uri );
   }

}
