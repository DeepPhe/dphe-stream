package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;
import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
abstract public class AbstractAttributeNormalizer implements AttributeNormalizer {

   protected enum EvidenceLevel {
      DIRECT_EVIDENCE,
      INDIRECT_EVIDENCE,
      NOT_EVIDENCE;
   }

   private String _bestCode;
   private int _uniqueCodeCount = 1;
   private int _bestCodesCount = 1;
   private int _allCodesCount = 1;
   private final EnumMap<EvidenceLevel,Collection<Mention>> _evidenceMap = new EnumMap<>( EvidenceLevel.class );

   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      final String bestCode = getBestCode( infoCollector );
      setBestCode( bestCode );
      fillEvidenceMap( infoCollector, dependencies );
   }

   protected void setBestCode( final String bestCode ) {
      _bestCode = bestCode;
   }

   public String getBestCode() {
      return _bestCode;
   }

   protected void setUniqueCodeCount( final int uniqueCodeCount ) {
      _uniqueCodeCount = uniqueCodeCount;
   }

   protected int getUniqueCodeCount() {
      return _uniqueCodeCount;
   }

   protected void setBestCodesCount( final int bestCodesCount ) {
      _bestCodesCount = bestCodesCount;
   }

   protected int getBestCodesCount() {
      return _bestCodesCount;
   }

   protected void setAllCodesCount( final int allCodesCount ) {
      _allCodesCount = allCodesCount;
   }

   protected int getAllCodesCount() {
      return _allCodesCount;
   }


   public double getConfidenceMultiplier() {
      return (double)getBestCodesCount() / getAllCodesCount();
   }

   protected Map<Integer,Double> createIntCodeConfidenceMap( final Collection<ConceptAggregateRelation> relations ) {
      return relations.stream()
                       .collect( Collectors.toMap( this::getIntCode,
                                                   ConceptAggregateRelation::getConfidence ) );

   }

   protected int getIntCode( final ConceptAggregateRelation relation ) {
      return getIntCode( relation.getTarget() );
   }

   protected int getIntCode( final CrConceptAggregate aggregate ) {
      return getIntCode( aggregate.getUri() );
   }

   protected int getIntCode( final String uri ) {
      return -1;
   }

   protected Map<Integer,Long> createIntCodeCountMap( final Collection<CrConceptAggregate> aggregates ) {
      return createAllIntCodeCountMap( new ConfidenceGroup<>( aggregates ).getBest() );
   }

   protected Map<String,Long> createCodeCountMap( final Collection<CrConceptAggregate> aggregates ) {
      return createAllCodeCountMap( new ConfidenceGroup<>( aggregates ).getBest() );
   }

   protected Map<Integer,Long> createAllIntCodeCountMap( final Collection<CrConceptAggregate> aggregates ) {
      return aggregates.stream()
                         .map( this::getIntCode )
                         .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }

   protected Map<String,Long> createAllCodeCountMap( final Collection<CrConceptAggregate> aggregates ) {
      return aggregates.stream()
                      .map( this::getCode )
                      .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) );
   }

   /**
    *
    * @param codeCountMap - map of codes and the number of times those codes appear
    * @return a collection of the best codes, and a pair representing the count of those codes and number of codes.
    */
   protected List<Integer> getBestIntCodes( final Map<Integer,Long> codeCountMap ) {
      final List<Integer> bestCodes = new ArrayList<>();
      long maxCount = 0;
      for ( Map.Entry<Integer,Long> codeCount : codeCountMap.entrySet() ) {
         if ( codeCount.getValue() == maxCount ) {
            bestCodes.add( codeCount.getKey() );
         } else if ( codeCount.getValue() > maxCount ) {
            bestCodes.clear();
            bestCodes.add( codeCount.getKey() );
            maxCount = codeCount.getValue();
         }
      }
      return bestCodes;
   }

   /**
    *
    * @param codeCountMap - map of codes and the number of times those codes appear
    * @return a collection of the best codes, and a pair representing the count of those codes and number of codes.
    */
   protected List<String> getBestCodes( final Map<String,Long> codeCountMap ) {
      final List<String> bestCodes = new ArrayList<>();
      long maxCount = 0;
      for ( Map.Entry<String,Long> codeCount : codeCountMap.entrySet() ) {
         if ( codeCount.getValue() == maxCount ) {
            bestCodes.add( codeCount.getKey() );
         } else if ( codeCount.getValue() > maxCount ) {
            bestCodes.clear();
            bestCodes.add( codeCount.getKey() );
            maxCount = codeCount.getValue();
         }
      }
      return bestCodes;
   }

   public Collection<Mention> getDirectEvidence() {
      return _evidenceMap.getOrDefault( EvidenceLevel.DIRECT_EVIDENCE, Collections.emptyList() );
   }

   public Collection<Mention> getIndirectEvidence() {
      return _evidenceMap.getOrDefault( EvidenceLevel.INDIRECT_EVIDENCE, Collections.emptyList() );
   }

   public Collection<Mention> getNotEvidence() {
      return _evidenceMap.getOrDefault( EvidenceLevel.NOT_EVIDENCE, Collections.emptyList() );
   }

   protected void putEvidence( final EvidenceLevel level, final Collection<Mention> mentions ) {
      _evidenceMap.put( level, mentions );
   }

   protected void fillEvidenceMap( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies) {
      final String bestCode = getBestCode();
      final ConfidenceGroup<CrConceptAggregate> confidenceGroup = new ConfidenceGroup<>( infoCollector.getAllAggregates() );
      final Collection<Mention> bestMentions
            = confidenceGroup.getBest()
                             .stream()
            .filter( a -> hasBestCode( a, bestCode ) )
                             .map( CrConceptAggregate::getMentions )
                             .flatMap( Collection::stream )
                             .collect( Collectors.toSet() );
      putEvidence( EvidenceLevel.DIRECT_EVIDENCE, bestMentions );
      final Collection<Mention> notMentions
            = confidenceGroup.getBest()
                             .stream()
                             .map( CrConceptAggregate::getMentions )
                             .flatMap( Collection::stream )
                             .filter( m -> !bestMentions.contains( m ) )
                             .collect( Collectors.toSet() );
      putEvidence( EvidenceLevel.NOT_EVIDENCE, notMentions );
   }

   protected void useAllEvidenceMap( final AttributeInfoCollector infoCollector,
                                     final Map<String,String> dependencies) {
      final String bestCode = getBestCode();
      final ConfidenceGroup<CrConceptAggregate> confidenceGroup = new ConfidenceGroup<>( infoCollector.getAllAggregates() );
      final Collection<Mention> bestMentions
            = confidenceGroup.getBest()
                             .stream()
                             .filter( a -> hasBestCode( a, bestCode ) )
                             .map( CrConceptAggregate::getMentions )
                             .flatMap( Collection::stream )
                             .collect( Collectors.toSet() );
      putEvidence( EvidenceLevel.DIRECT_EVIDENCE, bestMentions );
      final Collection<Mention> nextMentions
            = confidenceGroup.getBest()
                             .stream()
                             .filter( a -> !getCode( a ).isEmpty() )
                             .map( CrConceptAggregate::getMentions )
                             .flatMap( Collection::stream )
                             .filter( m -> !bestMentions.contains( m ) )
                             .collect( Collectors.toSet() );
      putEvidence( EvidenceLevel.INDIRECT_EVIDENCE, nextMentions );
      final Collection<Mention> notMentions
            = confidenceGroup.getBest()
                             .stream()
                             .map( CrConceptAggregate::getMentions )
                             .flatMap( Collection::stream )
                             .filter( m -> !bestMentions.contains( m ) )
                             .filter( m -> !nextMentions.contains( m ) )
                             .collect( Collectors.toSet() );
      putEvidence( EvidenceLevel.NOT_EVIDENCE, notMentions );
   }


}
