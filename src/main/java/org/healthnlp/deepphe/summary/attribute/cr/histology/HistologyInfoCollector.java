package org.healthnlp.deepphe.summary.attribute.cr.histology;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class HistologyInfoCollector extends AbstractAttributeInfoCollector {


   public Collection<String> getRelationTypes() {
      return Collections.emptyList();
   }

   public Collection<CrConceptAggregate> getAllAggregates() {
      return Collections.singletonList( getNeoplasm() );
   }

   public Collection<CrConceptAggregate> getBestAggregates() {
      return getAllAggregates();
   }


}
