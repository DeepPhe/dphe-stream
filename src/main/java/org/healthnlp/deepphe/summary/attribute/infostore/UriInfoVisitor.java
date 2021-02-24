package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Map;

public interface UriInfoVisitor {

   Collection<String> getAllUris( Collection<ConceptAggregate> neoplasms );

   Collection<String> getMainUris( Collection<ConceptAggregate> neoplasms );

   Map<String,Integer> getAllUriStrengths( Collection<ConceptAggregate> neoplasms );

}
