package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.Collection;
import java.util.Map;

public class AllUriInfoStore extends UriInfoStore {

   public AllUriInfoStore( final Collection<ConceptAggregate> neoplasms,
                           final UriInfoVisitor uriInfoVisitor ) {
      super( uriInfoVisitor.getAllUris( neoplasms ) );
      final Map<String, Integer> uriStrengths = uriInfoVisitor.getAllUriStrengths( neoplasms );
      setUriStrengths( uriStrengths );
      setClassLevelMap( UriScoreUtil.createUriClassLevelMap( _uris ) );
   }


}
