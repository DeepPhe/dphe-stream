package org.healthnlp.deepphe.summary.attribute.topography.minor;

import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.HashSet;


final public class TopoMinorUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _topoMinorConcepts;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _topoMinorConcepts == null ) {
         _topoMinorConcepts = new HashSet<>();
         _topoMinorConcepts.addAll( LungMinorCodifier.getLungParts( neoplasms ) );
         _topoMinorConcepts.addAll( BreastMinorCodifier.geBreastParts( neoplasms ) );
         _topoMinorConcepts.addAll( CrcMinorCodifier.getColonParts( neoplasms ) );
         _topoMinorConcepts.addAll( CrcMinorCodifier.getAnusParts( neoplasms ) );


//         final Collection<ConceptAggregate> locations = neoplasms.stream()
//                                                                      .map( ConceptAggregate::getRelatedSites )
//                                                                      .flatMap( Collection::stream )
////                                                                      .filter( c -> !c.isNegated() )
//                                                                      .collect( Collectors.toSet() );
//         _topoMinorConcepts.addAll( locations );
      }
      return _topoMinorConcepts;
   }


}
