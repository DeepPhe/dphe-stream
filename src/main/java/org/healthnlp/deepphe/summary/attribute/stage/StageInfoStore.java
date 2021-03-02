package org.healthnlp.deepphe.summary.attribute.stage;

import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Collections;

public class StageInfoStore extends AttributeInfoStore {

   public StageCodeInfoStore _codeInfoStore = new StageCodeInfoStore();

   public StageInfoStore( final ConceptAggregate neoplasm, final UriInfoVisitor uriInfoVisitor ) {
      this( Collections.singletonList( neoplasm ), uriInfoVisitor );
   }

   public StageInfoStore( final Collection<ConceptAggregate> neoplasms, final UriInfoVisitor uriInfoVisitor ) {
      super( neoplasms, uriInfoVisitor );
   }


}
