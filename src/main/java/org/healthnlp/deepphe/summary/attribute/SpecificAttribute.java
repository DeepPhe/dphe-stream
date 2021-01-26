package org.healthnlp.deepphe.summary.attribute;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;
import java.util.stream.Collectors;

public interface SpecificAttribute {

   enum EvidenceLevel {
      DIRECT_EVIDENCE,
      INDIRECT_EVIDENCE,
      NOT_EVIDENCE;
   }

   NeoplasmAttribute toNeoplasmAttribute();

   static NeoplasmAttribute createAttribute( final String name, final String value ) {
      return createAttribute( name, value, Collections.emptyList(), Collections.emptyList() );
   }

   static NeoplasmAttribute createAttribute( final String name, final String value,
                                                     final List<Mention> directEvidence ) {
      return createAttribute( name, value, directEvidence, Collections.emptyList() );
   }

   static NeoplasmAttribute createAttribute( final String name, final String value,
                                               final List<Mention> directEvidence,
                                               final List<Mention> indirectEvidence ) {
      return createAttribute( name, value, directEvidence, indirectEvidence, indirectEvidence );
   }

   static NeoplasmAttribute createAttribute( final String name, final String value,
                                             final List<Mention> directEvidence,
                                             final List<Mention> indirectEvidence,
                                             final List<Mention> notEvidence ) {
      return createAttributeWithFeatures( name, value, directEvidence, indirectEvidence, notEvidence, Collections.emptyList() );
   }

   static NeoplasmAttribute createAttribute( final String name, final String value,
                                                         final List<Mention> directEvidence,
                                                         final List<Mention> indirectEvidence,
                                                         final List<Mention> notEvidence,
                                                         final List<Integer> features ) {
      return createAttributeWithFeatures( name, value, directEvidence, indirectEvidence, notEvidence, features );
   }

      static NeoplasmAttribute createAttributeWithFeatures( final String name, final String value,
                                             final List<Mention> directEvidence,
                                             final List<Mention> indirectEvidence,
                                             final List<Integer> features  ) {
      return createAttributeWithFeatures( name, value, directEvidence, indirectEvidence, indirectEvidence, features );
   }

   static NeoplasmAttribute createAttributeWithFeatures( final String name, final String value,
                                             final List<Mention> directEvidence,
                                             final List<Mention> indirectEvidence,
                                             final List<Mention> notEvidence,
                                             final List<Integer> features ) {
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      attribute.setName( name );
      attribute.setValue( value );
      attribute.setDirectEvidence( directEvidence );
      attribute.setIndirectEvidence( indirectEvidence );
      attribute.setNotEvidence( notEvidence );
      attribute.setConfidenceFeatures( features );
      return attribute;
   }



   static Map<EvidenceLevel, Collection<Mention>> mapEvidence( final Collection<ConceptAggregate> primaryConcepts,
                                                               final Collection<ConceptAggregate> neoplasmConcepts,
                                                               final Collection<ConceptAggregate> patientConcepts ) {
      final Map<EvidenceLevel,Collection<Mention>> evidenceMap = new HashMap<>();
      Arrays.stream( EvidenceLevel.values() )
            .forEach( l -> evidenceMap.put( l, new HashSet<>() ) );
      final Collection<Mention> primaryMentions = getAllMentions( primaryConcepts );
      final Collection<Mention> secondaryMentions = getAllMentions( neoplasmConcepts );
      final Collection<Mention> otherMentions = getAllMentions( patientConcepts  );
      secondaryMentions.removeAll( primaryMentions );
      otherMentions.removeAll( primaryMentions );
      otherMentions.removeAll( secondaryMentions );
      evidenceMap.put( EvidenceLevel.DIRECT_EVIDENCE, primaryMentions );
      evidenceMap.put( EvidenceLevel.INDIRECT_EVIDENCE, secondaryMentions );
      evidenceMap.put( EvidenceLevel.NOT_EVIDENCE, otherMentions );
      return evidenceMap;
   }


   static Collection<Mention> getAllMentions( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getMentions )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }

   static Collection<Mention> getMentions( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getMentions )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }

   static Collection<String> getMainUris( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getUri )
                     .collect( Collectors.toSet() );
   }

   static Collection<String> getAllUris( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getAllUris )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }


   static Collection<ConceptAggregate> getUriIsMain( final String uri,
                                                      final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .filter( c -> c.getUri().equals( uri ) )
                     .collect( Collectors.toSet() );
   }


   static Collection<ConceptAggregate> getUriInAny( final String uri, final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .filter( c -> c.getAllUris().contains( uri ) )
                     .collect( Collectors.toSet() );
   }



}