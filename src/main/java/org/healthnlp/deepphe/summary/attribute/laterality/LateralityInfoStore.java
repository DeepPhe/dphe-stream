package org.healthnlp.deepphe.summary.attribute.laterality;

import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Collections;

public class LateralityInfoStore extends AttributeInfoStore {

   public LateralityCodeInfoStore _codeInfoStore = new LateralityCodeInfoStore();

   public LateralityInfoStore( final ConceptAggregate neoplasm  ) {
      this( Collections.singletonList( neoplasm ) );
   }

   public LateralityInfoStore( final Collection<ConceptAggregate> neoplasms ) {
      super( neoplasms );
   }


}
