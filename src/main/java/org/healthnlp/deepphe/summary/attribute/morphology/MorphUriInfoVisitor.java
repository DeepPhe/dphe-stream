package org.healthnlp.deepphe.summary.attribute.morphology;

import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;

final public class MorphUriInfoVisitor implements UriInfoVisitor {

   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      return neoplasms;
   }


}
