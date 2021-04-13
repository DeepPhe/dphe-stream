package org.healthnlp.deepphe.summary.attribute.biomarker;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/9/2021}
 */
public class BiomarkerUriInfoVisitor implements UriInfoVisitor {

   final private String _biomarkerName;
   private Collection<ConceptAggregate> _biomarkerConcepts;

   public BiomarkerUriInfoVisitor( final String biomarkerName ) {
      _biomarkerName = biomarkerName;
   }

   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _biomarkerConcepts == null ) {
         _biomarkerConcepts = neoplasms.stream()
                                        .map( c -> c.getRelated( RelationConstants.has_Biomarker ) )
                                        .flatMap( Collection::stream )
                                       .filter( c -> c.getUri().equals( _biomarkerName ) )
//                                        .filter( c -> !c.isNegated() )
                                        .collect( Collectors.toSet() );
      }
      return _biomarkerConcepts;
   }

}
