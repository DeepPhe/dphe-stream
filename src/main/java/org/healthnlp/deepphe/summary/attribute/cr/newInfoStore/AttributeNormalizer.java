package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;


import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This takes the place of [Cr]CodeInfoStore.
 *
 * @author SPF , chip-nlp
 * @since {3/22/2023}
 */
public interface AttributeNormalizer {

   void init( AttributeInfoCollector infoCollector, Map<String,String> dependencies );

   default String getDefaultTextCode() {
      return "9";
   }

   String getBestCode();

   String getBestCode( final AttributeInfoCollector infoCollector );

//   String getBestCode( final Collection<CrConceptAggregate> aggregates );

   default int getIntCode( final ConceptAggregateRelation relation ) {
      return getIntCode( relation.getTarget() );
   }

   default int getIntCode( final CrConceptAggregate aggregate ) {
      return getIntCode( aggregate.getUri() );
   }

   default int getIntCode( final String uri ) {
      return -1;
   }

//   default String getTextCode( final ConceptAggregateRelation relation ) {
//      return getTextCode( relation.getTarget() );
//   }

   default String getTextCode( final CrConceptAggregate aggregate ) {
      return getTextCode( aggregate.getUri() );
   }

   default String getTextCode( final String uri ) {
      final int code = getIntCode( uri );
      return code <= 0 ? getDefaultTextCode() : code+"";
   }


   default boolean hasBestCode( final CrConceptAggregate aggregate, final String bestCode ) {
      return getTextCode( aggregate ).equals( bestCode );
   }

//   default Map<Integer,Double> createIntCodeConfidenceMap( final Collection<ConceptAggregateRelation> relations ) {
//      final Map<Integer, List<ConceptAggregateRelation>> codeRelationsMap
//            = relations.stream()
//                       .collect( Collectors.groupingBy( this::getIntCode ) );
//      final Map<Integer,Double>  confidenceMap = new HashMap<>();
//      for ( Map.Entry<Integer,List<ConceptAggregateRelation>> codeRelations : codeRelationsMap.entrySet() ) {
//         NeoplasmSummaryCreator.addDebug( "AttributeNormalizer.createIntCodeConfidenceMap " +
//                                          codeRelations.getKey() + " "
//                                          + codeRelations.getValue().stream()
//                                                         .map( ConceptAggregateRelation::getTarget )
//                                                         .map( CrConceptAggregate::getUri )
//                                                         .collect( Collectors.joining(", ") ) + "\n" );
//         NeoplasmSummaryCreator.addDebug( "AttributeNormalizer.createIntCodeConfidenceMap " +
//                                          codeRelations.getKey() + " "
//                                          + codeRelations.getValue().stream()
//                                                         .map( ConceptAggregateRelation::getConfidence )
//                                                         .map( c -> c+"" )
//                                                         .collect( Collectors.joining(", ") ) + "\n");
//         final double confidence = codeRelations.getValue()
//                                                .stream()
//                                                .mapToDouble( ConceptAggregateRelation::getConfidence )
//                                                .sum();
//         confidenceMap.put( codeRelations.getKey(), confidence );
//      }
//      return confidenceMap;
//   }


   default Map<Integer,Double> createIntCodeConfidenceMap( final Collection<ConceptAggregateRelation> relations ) {
      final Map<Integer,Collection<MentionRelation>> codeRelationsMap = createIntCodeMentionsMap( relations );
      final Map<Integer,Double>  confidenceMap = new HashMap<>();
      for ( Map.Entry<Integer,Collection<MentionRelation>> codeRelations : codeRelationsMap.entrySet() ) {
         NeoplasmSummaryCreator.addDebug( "AttributeNormalizer.createIntCodeConfidenceMap " +
                                          codeRelations.getKey() + " "
                                          + codeRelations.getValue().stream()
                                                         .map( MentionRelation::getdConfidence )
                                                         .map( c -> c+"" )
                                                         .collect( Collectors.joining(", ") ) + "\n");
         final double confidence = codeRelations.getValue()
                                                .stream()
                                                .mapToDouble( MentionRelation::getdConfidence )
                                                .sum();
         confidenceMap.put( codeRelations.getKey(), confidence );
      }
      return confidenceMap;
   }

//   default Map<String,Double> createTextCodeConfidenceMap( final Collection<ConceptAggregateRelation> relations ) {
//      final Map<String,List<ConceptAggregateRelation>> codeRelationsMap
//            = relations.stream()
//                       .collect( Collectors.groupingBy( this::getTextCode ) );
//      final Map<String,Double>  confidenceMap = new HashMap<>();
//      for ( Map.Entry<String,List<ConceptAggregateRelation>> codeRelations : codeRelationsMap.entrySet() ) {
//         NeoplasmSummaryCreator.addDebug( "AttributeNormalizer.createTextCodeConfidenceMap " +
//                                          codeRelations.getKey() + " "
//                                          + codeRelations.getValue().stream()
//                                                         .map( ConceptAggregateRelation::getTarget )
//                                                         .map( CrConceptAggregate::getUri )
//                                                         .collect( Collectors.joining(", ") ) + "\n");
//         NeoplasmSummaryCreator.addDebug( "AttributeNormalizer.createTextCodeConfidenceMap " +
//                                          codeRelations.getKey() + " "
//                                          + codeRelations.getValue().stream()
//                                                         .map( ConceptAggregateRelation::getConfidence )
//                                                         .map( c -> c+"" )
//                                                         .collect( Collectors.joining(", ") ) + "\n");
//         final double confidence = codeRelations.getValue()
//                                                .stream()
//                                                .mapToDouble( ConceptAggregateRelation::getConfidence )
//                                                .sum();
//         confidenceMap.put( codeRelations.getKey(), confidence );
//      }
//      return confidenceMap;
//   }

   default Map<String,Double> createTextCodeConfidenceMap( final Collection<ConceptAggregateRelation> relations ) {
      final Map<String,Collection<MentionRelation>> codeRelationsMap = createTextCodeMentionsMap( relations );
      final Map<String,Double>  confidenceMap = new HashMap<>();
      for ( Map.Entry<String,Collection<MentionRelation>> codeRelations : codeRelationsMap.entrySet() ) {
         NeoplasmSummaryCreator.addDebug( "AttributeNormalizer.createIntCodeConfidenceMap " +
                                          codeRelations.getKey() + " "
                                          + codeRelations.getValue().stream()
                                                         .map( MentionRelation::getdConfidence )
                                                         .map( c -> c+"" )
                                                         .collect( Collectors.joining(", ") ) + "\n");
         final double confidence = codeRelations.getValue()
                                                .stream()
                                                .mapToDouble( MentionRelation::getdConfidence )
                                                .sum();
         confidenceMap.put( codeRelations.getKey(), confidence );
      }
      return confidenceMap;
   }

   default Map<Integer,Collection<MentionRelation>> createIntCodeMentionsMap(
         final Collection<ConceptAggregateRelation> relations ) {
      final Map<Integer,Collection<MentionRelation>> map = new HashMap<>();
      for ( ConceptAggregateRelation relation : relations ) {
         final CrConceptAggregate target = relation.getTarget();
         final Map<String, Mention> idMentions = target.getMentions()
                                                       .stream()
                                                       .collect(
                                                             Collectors.toMap( Mention::getId, Function.identity() ) );
         final Collection<MentionRelation> mentionRelations = relation.getMentionRelations();
         for ( MentionRelation mentionRelation : mentionRelations ) {
            final Mention mention = idMentions.get( mentionRelation.getTargetId() );
            if ( mention != null ) {
               final int code = getIntCode( mention.getClassUri() );
               if ( code >= 0 ) {
                  map.computeIfAbsent( code, c -> new HashSet<>() ).add( mentionRelation );
               }
            }
         }
      }
      return map;
   }

   default Map<String,Collection<MentionRelation>> createTextCodeMentionsMap(
         final Collection<ConceptAggregateRelation> relations ) {
      final Map<String,Collection<MentionRelation>> map = new HashMap<>();
      for ( ConceptAggregateRelation relation : relations ) {
         final CrConceptAggregate target = relation.getTarget();
         final Map<String, Mention> idMentions = target.getMentions()
                                                       .stream()
                                                       .collect(
                                                             Collectors.toMap( Mention::getId, Function.identity() ) );
         final Collection<MentionRelation> mentionRelations = relation.getMentionRelations();
         for ( MentionRelation mentionRelation : mentionRelations ) {
            final Mention mention = idMentions.get( mentionRelation.getTargetId() );
            if ( mention != null ) {
               final String code = getTextCode( mention.getClassUri() );
               if ( !code.isEmpty() ) {
                  map.computeIfAbsent( code, c -> new HashSet<>() ).add( mentionRelation );
               }
            }
         }
      }
      return map;
   }


   default List<Integer> getConfidentIntCodes( final Map<Integer,Double> codeConfidenceMap ) {
      if ( codeConfidenceMap.size() < 2 ) {
         return new ArrayList<>( codeConfidenceMap.keySet() );
      }
      final List<Integer> bestCodes = new ArrayList<>();
      double maxConfidence = 0;
      for ( Map.Entry<Integer,Double> codeConfidence : codeConfidenceMap.entrySet() ) {
         if ( codeConfidence.getValue() == maxConfidence ) {
            bestCodes.add( codeConfidence.getKey() );
         } else if ( codeConfidence.getValue() > maxConfidence ) {
            bestCodes.clear();
            bestCodes.add( codeConfidence.getKey() );
            maxConfidence = codeConfidence.getValue();
         }
      }
      return bestCodes;
   }

   default List<String> getConfidentTextCodes( final Map<String,Double> codeConfidenceMap ) {
      if ( codeConfidenceMap.size() < 2 ) {
         return new ArrayList<>( codeConfidenceMap.keySet() );
      }
      final List<String> bestCodes = new ArrayList<>();
      double maxConfidence = 0;
      for ( Map.Entry<String,Double> codeConfidence : codeConfidenceMap.entrySet() ) {
         if ( codeConfidence.getValue() == maxConfidence ) {
            bestCodes.add( codeConfidence.getKey() );
         } else if ( codeConfidence.getValue() > maxConfidence ) {
            bestCodes.clear();
            bestCodes.add( codeConfidence.getKey() );
            maxConfidence = codeConfidence.getValue();
         }
      }
      return bestCodes;
   }

   /**
    *
    * @return between 0 and 1
    */
   double getConfidence();


   default Collection<Mention> getDirectEvidence() {
      return Collections.emptyList();
   }

   default Collection<Mention> getIndirectEvidence() {
      return Collections.emptyList();
   }

   default Collection<Mention> getNotEvidence() {
      return Collections.emptyList();
   }

}
