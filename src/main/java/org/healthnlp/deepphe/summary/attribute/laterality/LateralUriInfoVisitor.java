package org.healthnlp.deepphe.summary.attribute.laterality;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.stream.Collectors;

final public class LateralUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _lateralityConcepts;

   public Collection<ConceptAggregate> getConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _lateralityConcepts == null ) {
         _lateralityConcepts = neoplasms.stream()
                                  .map( c -> c.getRelated( RelationConstants.HAS_LATERALITY ) )
                                  .flatMap( Collection::stream )
                                  .collect( Collectors.toSet() );
      }
      return _lateralityConcepts;
   }

}
