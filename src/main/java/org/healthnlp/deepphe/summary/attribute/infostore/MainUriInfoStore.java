package org.healthnlp.deepphe.summary.attribute.infostore;

import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MainUriInfoStore extends UriInfoStore {

   public MainUriInfoStore( final Collection<ConceptAggregate> neoplasms,
                            final AllUriInfoStore allUriInfoStore ) {
      super( getMainUris( neoplasms ) );
      final Map<String, Integer> uriStrengths = new HashMap<>( allUriInfoStore._uriStrengths );
      uriStrengths.keySet()
                  .retainAll( _uris );
      setUriStrengths( uriStrengths );
      final Map<String, Integer> classLevelMap = new HashMap<>( allUriInfoStore._classLevelMap );
      classLevelMap.keySet()
                   .retainAll( _uris );
      setClassLevelMap( classLevelMap );
   }

   // Allows for uris that have tied quotients
   static private Collection<String> getMainUris( final Collection<ConceptAggregate> neoplasms  ) {
      return neoplasms.stream()
                      .map( ConceptAggregate::getUri )
                      .collect( Collectors.toSet() );
   }

}
