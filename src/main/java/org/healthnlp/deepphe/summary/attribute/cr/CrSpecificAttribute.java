package org.healthnlp.deepphe.summary.attribute.cr;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.*;
import java.util.stream.Collectors;


public interface CrSpecificAttribute {


   NeoplasmAttribute toNeoplasmAttribute();

   static NeoplasmAttribute createAttribute( final String name, final String value, final String uri ,
                                             final double confidence ) {
      return createAttribute( name, value, uri, confidence,
                              Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                              Collections.emptyList() );
   }

   static NeoplasmAttribute createAttribute( final String name, final String value,
                                             final String uri, final double confidence,
                                             final Collection<Mention> directEvidence,
                                             final Collection<Mention> indirectEvidence,
                                             final Collection<Mention> notEvidence,
                                             final List<Double> features ) {
      return createAttributeWithFeatures( name, value, uri, confidence,
                                          directEvidence, indirectEvidence, notEvidence,
                                          features );
   }

   // Todo  prettyName    prettyValue
   static NeoplasmAttribute createAttributeWithFeatures( final String name, final String value,
                                                         final String uri, final double confidence,
                                                         final Collection<Mention> directEvidence,
                                             final Collection<Mention> indirectEvidence,
                                             final Collection<Mention> notEvidence,
                                             final List<Double> features ) {
      NeoplasmSummaryCreator.addDebug( name + " = " + value + " : " + uri + "\n" );
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      attribute.setName( name );
      attribute.setValue( value );
      attribute.setClassUri( uri );
      // Simple intValue.  May want to round or ceil or floor.
      attribute.setConfidence( new Double( 100*confidence ).intValue() );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setDirectEvidence( getSortedMentions( directEvidence ) );
      attribute.setIndirectEvidence( getSortedMentions( indirectEvidence ) );
      attribute.setNotEvidence( getSortedMentions( notEvidence ) );
      // Simple intValue.  May want to round or ceil or floor.
      final List<Integer> intFeatures = features.stream()
                                                .map( Double::intValue )
                                                .collect( Collectors.toList() );
      attribute.setConfidenceFeatures( intFeatures );
      return attribute;
   }


   static Collection<Mention> getAllMentions( final Collection<CrConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getMentions )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }

   static Collection<Mention> getMentions( final Collection<CrConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getMentions )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }

   static Collection<String> getMainUris( final Collection<CrConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getUri )
                     .collect( Collectors.toSet() );
   }

   static Collection<String> getAllUris( final Collection<CrConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getAllUris )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }


   static Collection<CrConceptAggregate> getIfUriIsMain( final String uri,
                                                       final Collection<CrConceptAggregate> concepts ) {
      return concepts.stream()
                     .filter( c -> c.getUri().equals( uri ) )
                     .collect( Collectors.toSet() );
   }


   static Collection<CrConceptAggregate> getIfUriIsAny( final String uri, final Collection<CrConceptAggregate> concepts ) {
      return concepts.stream()
                     .filter( c -> c.getAllUris().contains( uri ) )
                     .collect( Collectors.toSet() );
   }


   static int getBranchCountsSum( final Map<String, Integer> conceptBranchCounts ) {
      return conceptBranchCounts.values()
                                .stream()
                                .mapToInt( i -> i )
                                .sum();
   }

   static List<Mention> getSortedMentions( final Collection<Mention> mentions ) {
      return mentions.stream()
                     .sorted( Comparator.comparingInt( Mention::getBegin )
                                        .thenComparingInt( Mention::getEnd ) )
                     .collect( Collectors.toList() );
   }

}
