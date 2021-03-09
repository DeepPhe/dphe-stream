package org.healthnlp.deepphe.summary.attribute.laterality;

import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Collections;

public class LateralityInfoStore extends AttributeInfoStore<LateralityCodeInfoStore> {

   public LateralityInfoStore( final ConceptAggregate neoplasm, final UriInfoVisitor uriInfoVisitor ) {
      this( Collections.singletonList( neoplasm ), uriInfoVisitor );
   }

   public LateralityInfoStore( final Collection<ConceptAggregate> neoplasms, final UriInfoVisitor uriInfoVisitor ) {
      super( neoplasms, uriInfoVisitor );
   }

   protected LateralityCodeInfoStore createCodeInfoStore() {
      return new LateralityCodeInfoStore();
   }

}
