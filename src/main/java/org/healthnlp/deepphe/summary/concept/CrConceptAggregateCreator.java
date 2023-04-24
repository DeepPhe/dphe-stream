package org.healthnlp.deepphe.summary.concept;

import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {3/13/2023}
 */
public class CrConceptAggregateCreator {

   static private final double NEGATED_THRESHOLD = 0.75;

   static private void addAggregates( final String patientId,
                                      final Map<Mention, String> patientMentionNoteIds,
                                      final Collection<String> applicableUris,
                                      final Map<String, List<Mention>> uriMentionsMap,
                                      final Map<String, Collection<CrConceptAggregate>> conceptAggregates ) {
      final Map<SemanticTui,List<String>> semanticUrisMap
            = applicableUris.stream()
                            .collect( Collectors.groupingBy( u -> UriInfoCache.getInstance().getSemanticTui( u ) ) );
      for ( Map.Entry<SemanticTui,List<String>> semanticUris : semanticUrisMap.entrySet() ) {
         final Collection<String> uris = semanticUris.getValue();
         if ( semanticUris.getKey()
                          .equals( SemanticTui.T191 ) ) {
            final CrConceptAggregate neoplasmAggregate = createAggregate( patientId, patientMentionNoteIds,
                                                                          uris, uriMentionsMap );
            conceptAggregates.computeIfAbsent( neoplasmAggregate.getUri(), ci -> new ArrayList<>() )
                             .add( neoplasmAggregate );
         } else {
            final Map<String, Collection<String>> uriUrisMap = UriUtil.getAssociatedUriMap( uris );
            for ( Map.Entry<String,Collection<String>> uriUris : uriUrisMap.entrySet() ) {
               final CrConceptAggregate otherAggregate = createOtherAggregate( patientId,
                                                                               patientMentionNoteIds,
                                                                               uriUris.getKey(),
                                                                               uriUris.getValue(),
                                                                               uriMentionsMap );
               conceptAggregates.computeIfAbsent( otherAggregate.getUri(), ci -> new ArrayList<>() )
                                .add( otherAggregate );
            }
         }
      }

//      final Map<String,Collection<String>> uriUrisMap = UriUtil.getAssociatedUriMap( applicableUris );
//      final Collection<String> neoplasmUris
//            = uriUrisMap.keySet()
//                                .stream()
//                                .filter( u -> UriInfoCache.getInstance()
//                                                          .getSemanticTui( u ) == SemanticTui.T191 )
//                                .collect( Collectors.toSet() );
//      final Collection<String> otherUris = new HashSet<>( uriUrisMap.keySet() );
//      if ( !neoplasmUris.isEmpty() ) {
//         // For Cancer Registries we just want a single neoplasm.
//         final CrConceptAggregate neoplasmAggregate = createNeoplasmAggregate( patientId,
//                                                                             patientMentionNoteIds,
//                                                                             neoplasmUris,
//                                                                             uriUrisMap,
//                                                                             uriMentionsMap );
//         conceptAggregates.computeIfAbsent( neoplasmAggregate.getUri(), ci -> new ArrayList<>() )
//                          .add( neoplasmAggregate );
//         otherUris.removeAll( neoplasmUris );
//      }
//      if ( !otherUris.isEmpty() ) {
//         for ( String uri : otherUris ) {
//            final CrConceptAggregate otherAggregate = createOtherAggregate( patientId,
//                                                                          patientMentionNoteIds,
//                                                                          uri,
//                                                                          uriUrisMap.get( uri ),
//                                                                          uriMentionsMap );
//            conceptAggregates.computeIfAbsent( otherAggregate.getUri(), ci -> new ArrayList<>() )
//                             .add( otherAggregate );
//         }
//      }
   }



   static private Collection<CrConceptAggregate> createAggregates( final String patientId,
                                      final Map<Mention, String> patientMentionNoteIds,
                                      final Map<String, List<Mention>> uriMentionsMap ) {
      final Collection<CrConceptAggregate> aggregates = new HashSet<>();
      final Map<SemanticTui, List<String>> semanticUrisMap
            = uriMentionsMap.keySet().stream()
                            .collect( Collectors.groupingBy( u -> UriInfoCache.getInstance()
                                                                              .getSemanticTui( u ) ) );
      for ( Map.Entry<SemanticTui, List<String>> semanticUris : semanticUrisMap.entrySet() ) {
         final Collection<String> uris = semanticUris.getValue();
         if ( semanticUris.getKey()
                          .equals( SemanticTui.T191 ) ) {
            final CrConceptAggregate neoplasmAggregate = createAggregate( patientId, patientMentionNoteIds,
                                                                          uris, uriMentionsMap );
            aggregates.add( neoplasmAggregate );
         } else {
            final Map<String, Collection<String>> uriUrisMap = UriUtil.getAssociatedUriMap( uris );
            for ( Map.Entry<String, Collection<String>> uriUris : uriUrisMap.entrySet() ) {
               final CrConceptAggregate otherAggregate = createOtherAggregate( patientId,
                                                                               patientMentionNoteIds,
                                                                               uriUris.getKey(),
                                                                               uriUris.getValue(),
                                                                               uriMentionsMap );
               aggregates.add( otherAggregate );
            }
         }
      }
      return aggregates;
   }



      static private CrConceptAggregate createNeoplasmAggregate( final String patientId,
                                                            final Map<Mention, String> patientMentionNoteIds,
                                                            final Collection<String> neoplasmUris,
                                                            final Map<String,Collection<String>> uriUrisMap,
                                                            final Map<String, List<Mention>> uriMentionsMap ) {
      final Collection<String> allNeoplasmUris = new HashSet<>();
      for ( String neoplasmUri : neoplasmUris ) {
         allNeoplasmUris.add( neoplasmUri );
         allNeoplasmUris.addAll( uriUrisMap.get( neoplasmUri ) );
      }
      return createAggregate( patientId, patientMentionNoteIds, allNeoplasmUris, uriMentionsMap );
   }

   static private CrConceptAggregate createOtherAggregate( final String patientId,
                                                            final Map<Mention, String> patientMentionNoteIds,
                                                            final String otherUri,
                                                            final Collection<String> otherUris,
                                                            final Map<String, List<Mention>> uriMentionsMap ) {
      final Collection<String> allOtherUris = new HashSet<>( otherUris );
      allOtherUris.add( otherUri );
      return createAggregate( patientId, patientMentionNoteIds, allOtherUris, uriMentionsMap );
   }

   static private CrConceptAggregate createAggregate( final String patientId,
                                                         final Map<Mention, String> patientMentionNoteIds,
                                                         final Collection<String> uris,
                                                         final Map<String, List<Mention>> uriMentionsMap ) {
      // smaller map of uris to roots that only contains pertinent uris.
      final Map<String,Collection<String>> uriRoots = new HashMap<>();
      final Collection<Mention> mentions = new HashSet<>();
      for ( String uri : uris ) {
         uriRoots.put( uri, UriInfoCache.getInstance().getUriRoots( uri ) );
         mentions.addAll( uriMentionsMap.get( uri ) );
      }
      final Map<String, Collection<Mention>> noteIdMentionsMap = new HashMap<>();
      for ( Mention mention : mentions ) {
         noteIdMentionsMap.computeIfAbsent( patientMentionNoteIds.get( mention ), d -> new HashSet<>() )
                          .add( mention );
      }
      return new CrConceptAggregate( patientId, uriRoots, noteIdMentionsMap );
   }

//   /**
//    * @param patientId      -
//    * @param patientMentionNoteIds -
//    * @param patientRelations   -
//    * @return map of best uri to its concept instances
//    */
//   static public Map<String, Collection<CrConceptAggregate>> createUriConceptAggregateMapOld(
//         final String patientId,
//         final Map<Mention, String> patientMentionNoteIds,
//         final Collection<MentionRelation> patientRelations ) {
//      // Map of unique URIs to all Mentions with that URI.
//      final Map<String, List<Mention>> uriMentionsMap = mapUriMentions( patientMentionNoteIds.keySet() );
//      final Collection<String> negatedUris = getNegatedUris( uriMentionsMap );
//      final Collection<String> affirmedUris = new HashSet<>( uriMentionsMap.keySet() );
//      affirmedUris.removeAll( negatedUris );
//      // Map of unique xDoc URIs to URIs that are associated (e.g. same branch).
//      // Has nothing to do with previously determined in-doc coreference chains.
//      final Map<String, Collection<CrConceptAggregate>> aggregates = new HashMap<>();
//      if ( !affirmedUris.isEmpty() ) {
//         addAggregates( patientId, patientMentionNoteIds, affirmedUris, uriMentionsMap, aggregates );
//      }
//      if ( !negatedUris.isEmpty() ) {
//         addAggregates( patientId, patientMentionNoteIds, negatedUris, uriMentionsMap, aggregates );
//      }
//      addRelations( aggregates.values(), patientRelations );
//      return aggregates;
//   }

   /**
    * @param patientId      -
    * @param patientMentionNoteIds -
    * @param patientRelations   -
    * @return map of best uri to its concept instances
    */
   static public Collection<CrConceptAggregate> createCrConceptAggregates(
         final String patientId,
         final Map<Mention, String> patientMentionNoteIds,
         final Collection<MentionRelation> patientRelations ) {
      final Map<Boolean,Collection<Mention>> assertedMentions
            = sortAssertedMentions( patientMentionNoteIds.keySet() );
      // Map of unique URIs to all Mentions with that URI.
      final Map<String, List<Mention>> uriAssertedMentionsMap
            = mapUriMentions( assertedMentions.getOrDefault( Boolean.TRUE, Collections.emptyList() ) );
      // Map of unique xDoc URIs to URIs that are associated (e.g. same branch).
      // Has nothing to do with previously determined in-doc coreference chains.
      final Collection<CrConceptAggregate> aggregates = new HashSet<>();
      if ( !uriAssertedMentionsMap.isEmpty() ) {
         aggregates.addAll( createAggregates( patientId, patientMentionNoteIds, uriAssertedMentionsMap ) );
      }
      final Map<String, List<Mention>> uriNegatedMentionsMap
            = mapUriMentions( assertedMentions.getOrDefault( Boolean.FALSE, Collections.emptyList() ) );
      if ( !uriNegatedMentionsMap.isEmpty() ) {
         aggregates.addAll( createAggregates( patientId, patientMentionNoteIds, uriNegatedMentionsMap ) );
      }
      addRelations( aggregates, patientRelations );
      return aggregates;
   }

   static private void addRelationsOld( final Collection<Collection<CrConceptAggregate>> aggregates,
                                     final Collection<MentionRelation> mentionRelations ) {
      final Map<String,CrConceptAggregate> mentionIdAggregateMap = new HashMap<>();
      for ( Collection<CrConceptAggregate> aggregateSet : aggregates ) {
         for ( CrConceptAggregate aggregate : aggregateSet ) {
            aggregate.getMentions().forEach( m -> mentionIdAggregateMap.put( m.getId(), aggregate ) );
         }
      }
      final Map<CrConceptAggregate,Collection<MentionRelation>> sourceMentionRelationsMap = new HashMap<>();
      for ( MentionRelation mentionRelation : mentionRelations ) {
         final String sourceId = mentionRelation.getSourceId();
         final CrConceptAggregate sourceAggregate = mentionIdAggregateMap.get( sourceId );
         sourceMentionRelationsMap.computeIfAbsent( sourceAggregate, a -> new HashSet<>() )
                                  .add( mentionRelation );
      }
      for ( Map.Entry<CrConceptAggregate,Collection<MentionRelation>> sourceMentionRelations :
            sourceMentionRelationsMap.entrySet() ) {
         addRelations( sourceMentionRelations.getKey(), mentionIdAggregateMap, sourceMentionRelations.getValue() );
      }

//      addAsTargetConfidence( mentionRelations, mentionIdAggregateMap );
   }


   static private void addRelations( final Collection<CrConceptAggregate> aggregates,
                                     final Collection<MentionRelation> mentionRelations ) {
      final Map<String,CrConceptAggregate> mentionIdAggregateMap = new HashMap<>();
      for ( CrConceptAggregate aggregate : aggregates ) {
         if ( aggregate.isNegated() ) {
            continue;
         }
         aggregate.getMentions().forEach( m -> mentionIdAggregateMap.put( m.getId(), aggregate ) );
      }
      final Map<CrConceptAggregate,Collection<MentionRelation>> sourceMentionRelationsMap = new HashMap<>();
      for ( MentionRelation mentionRelation : mentionRelations ) {
         final String sourceId = mentionRelation.getSourceId();
         final CrConceptAggregate sourceAggregate = mentionIdAggregateMap.get( sourceId );
         if ( sourceAggregate == null ) {
            continue;
         }
         sourceMentionRelationsMap.computeIfAbsent( sourceAggregate, a -> new HashSet<>() )
                                  .add( mentionRelation );
      }
      for ( Map.Entry<CrConceptAggregate,Collection<MentionRelation>> sourceMentionRelations :
            sourceMentionRelationsMap.entrySet() ) {
         addRelations( sourceMentionRelations.getKey(), mentionIdAggregateMap, sourceMentionRelations.getValue() );
      }
//      addAsTargetConfidence( mentionRelations, mentionIdAggregateMap );
   }

   static private void addRelations( final CrConceptAggregate sourceAggregate,
                                     final Map<String,CrConceptAggregate> mentionIdAggregateMap,
                                     final Collection<MentionRelation> mentionRelations ) {
      final Map<String,List<MentionRelation>> typeMentionRelationsMap
            = mentionRelations.stream()
                              .collect( Collectors.groupingBy( getRelationType ) );
      for ( Map.Entry<String,List<MentionRelation>> typeMentionRelations : typeMentionRelationsMap.entrySet() ) {
         addRelations( typeMentionRelations.getKey(), sourceAggregate, mentionIdAggregateMap,
                       typeMentionRelations.getValue() );
      }
   }

   /**
    * We want to combine primary site and associated site.  Anything in both counts as 2.
    */
   static private final Function<MentionRelation,String> getRelationType
         = mentionRelation -> mentionRelation.getType().equals( UriInfoCache.PRIMARY_SITE )
                              ? UriInfoCache.ASSOCIATED_SITE : mentionRelation.getType();


   static private void addRelations( final String type,
                                     final CrConceptAggregate sourceAggregate,
                                     final Map<String,CrConceptAggregate> mentionIdAggregateMap,
                                     final Collection<MentionRelation> mentionRelations ) {
      final Map<CrConceptAggregate,Collection<MentionRelation>> targetMentionRelationsMap = new HashMap<>();
      for ( MentionRelation mentionRelation : mentionRelations ) {
         final CrConceptAggregate targetAggregate = mentionIdAggregateMap.get( mentionRelation.getTargetId() );
         if ( targetAggregate == null ) {
            continue;
         }
         targetMentionRelationsMap.computeIfAbsent( targetAggregate, a -> new HashSet<>() ).add( mentionRelation );
      }
      for ( Map.Entry<CrConceptAggregate,Collection<MentionRelation>> targetMentionRelations :
            targetMentionRelationsMap.entrySet() ) {
         final ConceptAggregateRelation relation = new ConceptAggregateRelation( type,
                                                                                 targetMentionRelations.getKey(),
                                                                                 targetMentionRelations.getValue() );
         sourceAggregate.addAggregateRelation( relation );
      }
   }

//   static private void addAsTargetConfidence( final Collection<MentionRelation> mentionRelations,
//                                              final Map<String,CrConceptAggregate> mentionIdAggregateMap ) {
//      final Map<CrConceptAggregate,List<Double>> targetRelationConfidencesMap = new HashMap<>();
//      for ( MentionRelation mentionRelation : mentionRelations ) {
//         final CrConceptAggregate aggregate = mentionIdAggregateMap.get( mentionRelation.getTargetId() );
//         if ( aggregate == null || aggregate.isNegated() ) {
//            continue;
//         }
//         targetRelationConfidencesMap.computeIfAbsent( aggregate, a -> new ArrayList<>() )
//                                     .add( mentionRelation.getConfidence() );
//      }
//      for ( Map.Entry<CrConceptAggregate, List<Double>> targetRelationConfidences :
//            targetRelationConfidencesMap.entrySet() ) {
//         final double confidence =
//               ConfidenceCalculator.calculateAsRelationTarget( targetRelationConfidences.getValue() );
//         targetRelationConfidences.getKey().setAsTargetConfidence( confidence );
//      }
//   }


   /**
    *
    * @param mentions mentions in text, any document
    * @return map of URI to mentions having that exact uri
    */
   static private Map<String,List<Mention>> mapUriMentions( final Collection<Mention> mentions ) {
      return mentions.stream().collect( Collectors.groupingBy( Mention::getClassUri ) );
   }

   static private Map<Boolean,Collection<Mention>> sortAssertedMentions( final Collection<Mention> mentions ) {
      final Map<Boolean,Collection<Mention>> sortedMentions = new HashMap<>( 2 );
      for ( Mention mention : mentions ) {
         if ( mention.isNegated() ) {
            sortedMentions.computeIfAbsent( Boolean.FALSE, m -> new HashSet<>() ).add( mention );
         } else {
            sortedMentions.computeIfAbsent( Boolean.TRUE, m -> new HashSet<>() ).add( mention );
         }
      }
      return sortedMentions;
   }


   static private Collection<String> getNegatedUris(
         final Map<String, List<Mention>> uriMentionsMap ) {
      final Collection<String> negatedUris = new HashSet<>();
      for ( Map.Entry<String,List<Mention>> uriMentions : uriMentionsMap.entrySet() ) {
         final double negated = uriMentions.getValue().stream().filter( Mention::isNegated ).count();
         if ( (negated / uriMentions.getValue().size()) >= NEGATED_THRESHOLD ) {
            negatedUris.add( uriMentions.getKey() );
         }
      }
      return negatedUris;
   }


}
