package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

import java.util.Collection;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public interface FeatureCreator {

   void init( CrConceptAggregate neoplasm,
              Collection<CrConceptAggregate> allPatientNeoplasms,
              Collection<CrConceptAggregate> allNonNeoplasms );

   List<Double> getFeatures();

}
