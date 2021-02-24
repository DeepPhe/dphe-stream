package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.Collection;

public class AllUriInfoStore extends UriInfoStore {

   public AllUriInfoStore( final Collection<ConceptAggregate> neoplasms,
                           final UriInfoVisitor uriInfoVisitor ) {
      super( uriInfoVisitor.getAllUris( neoplasms ) );
      setUriStrengths( uriInfoVisitor.getUriStrengths( neoplasms ) );
      setClassLevelMap( UriScoreUtil.createUriClassLevelMap( _uris ) );
   }


}
