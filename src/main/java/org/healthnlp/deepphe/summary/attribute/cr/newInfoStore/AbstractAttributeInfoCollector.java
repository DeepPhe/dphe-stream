package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
abstract public class AbstractAttributeInfoCollector implements AttributeInfoCollector {

   private CrConceptAggregate _neoplasm;

   public void init( final CrConceptAggregate neoplasm ) {
      _neoplasm = neoplasm;
   }

   public CrConceptAggregate getNeoplasm() {
      return _neoplasm;
   }

}
