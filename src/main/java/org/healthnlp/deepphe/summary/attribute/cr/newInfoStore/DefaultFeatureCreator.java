package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class DefaultFeatureCreator implements FeatureCreator {

   public void init( final CrConceptAggregate neoplasm,
              final Collection<CrConceptAggregate> allPatientNeoplasms,
              final Collection<CrConceptAggregate> allNonNeoplasms ) {
      // TODO implement
   }

   public List<Double> getFeatures() {
      // TODO implement
      return Collections.emptyList();
   }

}
