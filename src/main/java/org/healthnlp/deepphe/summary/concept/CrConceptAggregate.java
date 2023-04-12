package org.healthnlp.deepphe.summary.concept;


import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/17/2020
 */
final public class CrConceptAggregate implements ConceptAggregate, ConfidenceOwner {

   static private final IdCounter ID_COUNTER = new IdCounter();

   private final BigInteger _unique_id_num;
   private final Map<String,Collection<String>> _uriRootsMap;
   private final String _patientId;

   private Map<String,Collection<ConceptAggregateRelation>> _aggregateRelationMap;
   private double _asTargetConfidence = 0.0;
   private double _confidence = -1;

   // TODO What we should actually do is create a map of annotations to some DocInfo object,
   // which contains patientId, docId, date, etc.
   private final Map<Mention, Date> _noteDateMap;
   private final Map<Mention, String> _noteIdMap;
   private final Date _date;
   private final String _uri;
   private final List<KeyValue<String, Double>> _uriQuotients;
   private final double _uriScore;

   public CrConceptAggregate( final String patientId,
                              final Map<String,Collection<String>> uriRootsMap,
                              final Map<String, Collection<Mention>> docMentionMap ) {
      _unique_id_num = ID_COUNTER.incrementAndGet();
      _patientId = patientId;
      _uriRootsMap = uriRootsMap;
      _date = new Date();
      _noteIdMap = new HashMap<>();
      _noteDateMap = new HashMap<>();
      for ( Map.Entry<String, Collection<Mention>> docMentions : docMentionMap.entrySet() ) {
         final String documentId = docMentions.getKey();
         docMentions.getValue().forEach( a -> addMention( a, documentId, _date ) );
      }
      // Quotients are not used except to calculate uriScore, which goes into confidence.
      _uriQuotients = UriScoreUtil.mapUriQuotients( createUriList(), uriRootsMap, getMentions() );
      final KeyValue<String,Double> bestUriScore
            = UriScoreUtil.getBestUriScore( uriRootsMap, _uriQuotients );
      _uri = bestUriScore.getKey();
      _uriScore = bestUriScore.getValue();
      NeoplasmSummaryCreator.addDebug( "CrConceptAggregate " + _uri + " uriScore: "
                                       + _uriQuotients.stream().map( kv -> kv.getKey() + "," + kv.getValue() ).collect(
            Collectors.joining(";") ) + " = " + _uriScore + "\n" );
//      final List<Double> quotientsOnly = _uriQuotients.stream().map( KeyValue::getValue ).collect( Collectors.toList() );
//      NeoplasmSummaryCreator.addDebug( "CrConceptAggregate " + _uri + " standard: "
//                                       + _uriQuotients.stream().map( kv -> kv.getKey() + "," + kv.getValue() ).collect(
//            Collectors.joining(";") ) + " = "
//                                       + ConfidenceCalculator.getStandardConfidence( quotientsOnly ) +"\n");
   }


   /**
    *
    * @param mention annotation within Concept
    * @param documentId id of the document with annotation
    * @param date       the date for the document in which the given annotation is found
    */
   public void addMention( final Mention mention,
                                 final String documentId,
                                 final Date date ) {
      _noteIdMap.put( mention, documentId );
      if ( date != null ) {
         _noteDateMap.put( mention, date );
      }
   }


   /**
    * @return the url of the instance
    */
   @Override
   public String getUri() {
      return _uri;
   }

   /**
    * This is the relative score of the best uri for this aggregate.
    * It is NOT an indicator of overall aggregate strength, confidence, etc.
    * @return  NEVER USED FOR CrConceptAggregate
    */
   @Override
   public double getUriScore() {
      return _uriScore;
   }

   /**
    *
    * @return  NEVER USED FOR CrConceptAggregate
    */
   @Override
   public List<KeyValue<String, Double>> getUriQuotients() {
      return _uriQuotients;
   }

   /**
    * @return [PatientId]_[DocId]_[SemanticGroup]_[HashCode]  -- now much shorter 9/18/2020
    */
   @Override
   public String getId() {
      return getPatientId() + '_' + _unique_id_num;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPatientId() {
      return _patientId;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Date getNoteDate( final Mention mention ) {
      if ( _noteDateMap != null && mention != null ) {
         final Date date = _noteDateMap.get( mention );
         if ( date != null ) {
            return date;
         }
      }
      return _date;
   }

   public void addAggregateRelation( final ConceptAggregateRelation relation ) {
      if ( relation == null ) {
         return;
      }
      if ( _aggregateRelationMap == null ) {
         _aggregateRelationMap = new HashMap<>();
      }
      _aggregateRelationMap.computeIfAbsent( relation.getType(), c -> new HashSet<>() ).add( relation );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<String,Collection<ConceptAggregate>> getRelatedConceptMap() {
      final Map<String,Collection<ConceptAggregateRelation>> bestRelations = getAllBestRelations();
      final Map<String,Collection<ConceptAggregate>> bestRelatedConcepts
            = new HashMap<>( _aggregateRelationMap.size() );
      for ( Map.Entry<String,Collection<ConceptAggregateRelation>> typeRelations : bestRelations.entrySet() ) {
          bestRelatedConcepts.put( typeRelations.getKey(), getRelationTargets( typeRelations.getValue() ) );
      }
      return bestRelatedConcepts;
   }


   public Map<String,Collection<ConceptAggregateRelation>> getAllRelations() {
      if ( _aggregateRelationMap == null || _aggregateRelationMap.isEmpty() ) {
         return Collections.emptyMap();
      }
      return Collections.unmodifiableMap( _aggregateRelationMap );
   }

   public Map<String,Collection<ConceptAggregateRelation>> getAllBestRelations() {
      if ( _aggregateRelationMap == null || _aggregateRelationMap.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<String,Collection<ConceptAggregateRelation>> bestRelations
            = new HashMap<>( _aggregateRelationMap.size() );
      for ( Map.Entry<String,Collection<ConceptAggregateRelation>> typeRelations : _aggregateRelationMap.entrySet() ) {
         bestRelations.put( typeRelations.getKey(), getBestRelations( typeRelations.getValue() ) );
      }
      return bestRelations;
   }

   private Collection<ConceptAggregateRelation> getRelations_( final String type ) {
      if ( _aggregateRelationMap == null || _aggregateRelationMap.isEmpty() ) {
         return Collections.emptyList();
      }
      return _aggregateRelationMap.getOrDefault( type, Collections.emptyList() );
   }

   public Collection<ConceptAggregateRelation> getRelations( final String... types ) {
      if ( _aggregateRelationMap == null || _aggregateRelationMap.isEmpty() ) {
         return Collections.emptyList();
      }
      return Stream.of( types )
                   .map( this::getRelations_ )
                   .flatMap( Collection::stream )
                   .collect( Collectors.toSet() );
   }


   /**
    *
    * @param relations -
    * @return The Relations with the top 2 confidence rankings.
    */
   static private Collection<ConceptAggregateRelation> getBestRelations(
         final Collection<ConceptAggregateRelation> relations ) {
      if ( relations.size() <= 2 ) {
         return relations;
      }
      return new ConfidenceGroup<>( relations ).getBest();
   }



   static private Collection<ConceptAggregate> getRelationTargets(
         final Collection<ConceptAggregateRelation> relations ) {
      return relations.stream()
                      .map( ConceptAggregateRelation::getTarget )
                      .collect( Collectors.toSet() );
   }


   public double getAsTargetConfidence() {
      return _asTargetConfidence;
   }

   public void setAsTargetConfidence( final double confidence ) {
      _asTargetConfidence = confidence;
   }



   /**
    * {@inheritDoc}
    */
   @Override
   public void addRelated( final String type, final ConceptAggregate related ) {
      if ( related == null ) {
         return;
      }
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addRelated( final String type, final Collection<ConceptAggregate> related ) {
      if ( related == null || related.isEmpty() ) {
         return;
      }
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void clearRelations() {
      if ( _aggregateRelationMap != null ) {
         _aggregateRelationMap.clear();
      }
   }



   public Map<Mention, String> getNoteIdMap() {
      return _noteIdMap != null ? _noteIdMap : Collections.emptyMap();
   }

   private Map<Mention, Date> getNoteDateMap() {
      return _noteDateMap != null ? _noteDateMap : Collections.emptyMap();
   }

   private List<String> createUriList() {
      return getMentions().stream()
                           .map( Mention::getClassUri )
                           .collect( Collectors.toList() );
   }

   public Map<String,Collection<String>> getUriRootsMap() {
      return _uriRootsMap;
   }

   /**
    *
    * @return confidence between 0 and 1
    */
   public double getConfidence() {
      if ( _confidence >= 0 ) {
         return _confidence;
      }
      // Subtract 20 for negation and 5 for uncertainty ?
//      final double assertionBump = isNegated() ? 20 : (isUncertain() ? 5 : 0);
      final double assertionBump = isNegated() ? 0.20 : (isUncertain() ? 0.05 : 0);
      // There is either a relationConfidence or an AsTargetConfidence.
      final double relationConfidence = Math.max( computeRelationConfidence(), getAsTargetConfidence() );
      _confidence = Math.max( 0.05, relationConfidence - assertionBump );
      NeoplasmSummaryCreator.addDebug( "CrConceptAggregate.getConfidence for " + getUri()
                                       + ": " + relationConfidence + " - "
                                       + assertionBump + " = " + _confidence +"\n");
      return _confidence;
   }


   /**
    *
    * @return between 0 and 1
    */
   private double computeRelationConfidence() {
      final  Map<String,Collection<ConceptAggregateRelation>> allRelations = getAllRelations();
      if ( allRelations.isEmpty() ) {
         return 0d;
      }
      final List<Double> confidences = new ArrayList<>();
      for ( Collection<ConceptAggregateRelation> relationSet : allRelations.values() ) {
         relationSet.stream()
                    .mapToDouble( ConceptAggregateRelation::getConfidence )
                    .forEach( confidences::add );
      }
      NeoplasmSummaryCreator.addDebug( "CrConceptAttribute.computeRelationConfidence "
                                       + confidences.stream().sorted().map( d -> d+"" ).collect(
            Collectors.joining(",") ) + " = "
                                       + (ConfidenceCalculator.getStandardConfidence( confidences )/100) + "\n" );
      return ConfidenceCalculator.getStandardConfidence( confidences )/100;
   }






   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return toText();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object other ) {
      return other instanceof CrConceptAggregate && ((CrConceptAggregate)other).getId().equals( getId() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return getId().hashCode();
   }


   /**
    * Like an AtomicLong using a BigInteger to go beyond the signed 64bit long max.
    */
   static private final class IdCounter {
      private final AtomicReference<BigInteger> _reference = new AtomicReference<>();
      private IdCounter() {
         _reference.set( BigInteger.valueOf( System.currentTimeMillis() ) );
      }

      private BigInteger incrementAndGet() {
         for ( ; ; ) {
            final BigInteger current = _reference.get();
            final BigInteger next = current.add( BigInteger.ONE );
            if ( _reference.compareAndSet( current, next ) ) {
               return next;
            }
         }
      }
   }

}
