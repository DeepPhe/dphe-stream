package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.concept.ConfidenceOwner;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public interface ConfidenceGrouper {

   void init( final Collection<ConfidenceOwner> confidenceOwners );

   ConfidenceGroup<ConfidenceOwner> getConfidenceGroup();

}
