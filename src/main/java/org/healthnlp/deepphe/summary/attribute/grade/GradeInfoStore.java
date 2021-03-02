package org.healthnlp.deepphe.summary.attribute.grade;

import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.Collections;

public class GradeInfoStore extends AttributeInfoStore {

   public GradeCodeInfoStore _codeInfoStore = new GradeCodeInfoStore();

   public GradeInfoStore( final ConceptAggregate neoplasm, final UriInfoVisitor uriInfoVisitor ) {
      this( Collections.singletonList( neoplasm ), uriInfoVisitor );
   }

   public GradeInfoStore( final Collection<ConceptAggregate> neoplasms, final UriInfoVisitor uriInfoVisitor ) {
      super( neoplasms, uriInfoVisitor );
   }


}
