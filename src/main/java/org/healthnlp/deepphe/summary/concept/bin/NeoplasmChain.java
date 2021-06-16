package org.healthnlp.deepphe.summary.concept.bin;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {6/8/2021}
 */
final public class NeoplasmChain {

   private boolean _valid;
   private String _headUri;
   final private Map<String, Collection<Mention>> _uriMentions = new HashMap<>();


   NeoplasmChain( final String headUri,
                  final Map<String,Collection<Mention>> uriMentions ) {
      _headUri = headUri;
      _uriMentions.putAll( uriMentions );
      _valid = true;
   }

   boolean isValid() {
      return _valid;
   }

   void invalidate() {
      _valid = false;
   }


   String getHeadUri() {
      return _headUri;
   }

   Collection<String> getChainUris() {
      return _uriMentions.keySet();
   }

   Map<String,Collection<Mention>> getUriMentions() {
      return _uriMentions;
   }

   Collection<Mention> getAllMentions() {
      return _uriMentions.values()
                         .stream()
                         .flatMap( Collection::stream )
                         .collect( Collectors.toSet() );
   }


   public void copyInto( final Collection<NeoplasmChain> otherChains ) {
      NeoplasmChain bestHeadUriChain = this;
      for ( NeoplasmChain otherChain : otherChains ) {
         bestHeadUriChain = getBestHeadUriChain( bestHeadUriChain, otherChain );
         otherChain._uriMentions.forEach( (k,v) -> _uriMentions.computeIfAbsent( k, m -> new HashSet<>() )
                                                              .addAll( v ) );
      }
      _headUri = bestHeadUriChain._headUri;
   }

   public void copyInto( final NeoplasmChain otherChain ) {
      _headUri = getBestHeadUriChain( this, otherChain )._headUri;
      otherChain._uriMentions.forEach( (k,v) -> _uriMentions.computeIfAbsent( k, m -> new HashSet<>() )
                                                           .addAll( v ) );
   }


   long scoreNeoplasmByUrisMatch( final NeoplasmChain otherChain ) {
      return scoreNeoplasmUrisMatch( otherChain.getChainUris() );
   }

   long scoreNeoplasmUrisMatch( final Collection<String> neoplasmUris ) {
      return neoplasmUris.stream()
                         .map( this::scoreNeoplasmUriMatch )
                         .mapToLong( l -> l )
                         .sum() / _uriMentions.size();
   }

   long scoreNeoplasmUriMatch( final String neoplasmUri ) {
      final Collection<Mention> matches = _uriMentions.get( neoplasmUri );
      if ( matches == null ) {
         return 0;
      }
      long score = matches.size();
      if ( neoplasmUri.equals( _headUri ) ) {
         score *= 10;
      }
      return score;
   }

   long scoreNeoplasmRootsMatch( final NeoplasmChain otherChain, final Map<String,Collection<String>> allUriRoots ) {
      return otherChain.getChainUris().stream()
                         .map( u -> scoreNeoplasmRootsMatch( u, allUriRoots ) )
                         .mapToLong( l -> l )
                         .sum();
   }

   long scoreNeoplasmRootsMatch( final String neoplasmUri, final Map<String,Collection<String>> allUriRoots ) {
      long score = 0;
      for ( Map.Entry<String, Collection<Mention>> uriMentions : _uriMentions.entrySet() ) {
         final Collection<String> roots = allUriRoots.get( uriMentions.getKey() );
         if ( roots.contains( neoplasmUri ) ) {
            score += uriMentions.getValue().size();
            if (  uriMentions.getKey().equals( _headUri ) ) {
               score *= 10;
            }
         }
      }
      return score;
   }


   ConceptAggregate createConceptAggregate( final String patientId,
                                             final Map<Mention, String> patientMentionNoteIds,
                                             final Map<String,Collection<String>> allUriRoots ) {
      final Map<String, Collection<Mention>> noteIdMentionsMap
            = _uriMentions.values()
                           .stream()
                           .flatMap( Collection::stream )
                           .collect( Collectors.groupingBy( patientMentionNoteIds::get,
                                                            Collectors.toCollection( HashSet::new ) ) );
      // smaller map of uris to roots that only contains pertinent uris.
      final Map<String,Collection<String>> uriRoots = new HashMap<>();
      _uriMentions.keySet().forEach( u -> uriRoots.put( u, allUriRoots.get( u ) ) );
      return new DefaultConceptAggregate( patientId, uriRoots, noteIdMentionsMap ) ;
   }


   ConceptAggregate createConceptAggregate( final String patientId,
                                            final Map<Mention, String> patientMentionNoteIds,
                                            final Map<String,Collection<String>> allUriRoots,
                                            final Collection<Mention> usedMentions ) {
      final Map<String, Collection<Mention>> noteIdMentionsMap
            = _uriMentions.values()
                          .stream()
                          .filter( c -> !usedMentions.containsAll( c ) )
                          .flatMap( Collection::stream )
                          .collect( Collectors.groupingBy( patientMentionNoteIds::get,
                                                           Collectors.toCollection( HashSet::new ) ) );
      if ( noteIdMentionsMap.isEmpty() ) {
         return null;
      }
      // smaller map of uris to roots that only contains pertinent uris.
      final Map<String,Collection<String>> uriRoots = new HashMap<>();
      _uriMentions.keySet().forEach( u -> uriRoots.put( u, allUriRoots.get( u ) ) );
      return new DefaultConceptAggregate( patientId, uriRoots, noteIdMentionsMap ) ;
   }


   KeyValue<Long,Collection<NeoplasmChain>> getBestMatchingChains( final Collection<NeoplasmChain> otherChains ) {
      long bestScore = 0;
      final Collection<NeoplasmChain> bestChains = new HashSet<>();
      for ( NeoplasmChain otherChain : otherChains ) {
         final long score = scoreNeoplasmByUrisMatch( otherChain );
         if ( score > 0 && score >= bestScore ) {
            if ( score > bestScore ) {
               bestScore = score;
               bestChains.clear();
            }
            bestChains.add( otherChain );
         }
      }
      return new KeyValue<>( bestScore, bestChains );
   }


   KeyValue<Long,Collection<NeoplasmChain>> getBestMatchingChains( final Collection<NeoplasmChain> otherChains,
                                                                   final Map<String,Collection<String>> allUriRoots ) {
      long bestScore = 0;
      final Collection<NeoplasmChain> bestChains = new HashSet<>();
      for ( NeoplasmChain otherChain : otherChains ) {
         final long score = scoreNeoplasmRootsMatch( otherChain, allUriRoots );
         if ( score > 0 && score >= bestScore ) {
            if ( score > bestScore ) {
               bestScore = score;
               bestChains.clear();
            }
            bestChains.add( otherChain );
         }
      }
      return new KeyValue<>( bestScore, bestChains );
   }



   // This should be faster than trying to split a UriUtil.getAssociatedUri
   static private NeoplasmChain getBestHeadUriChain( final NeoplasmChain chain1, final NeoplasmChain chain2 ) {
      if ( chain1._headUri.equals( chain2._headUri ) ) {
         return chain1;
      }
      final int headDiff = chain1._uriMentions.get( chain1._headUri ).size()
                           - chain2._uriMentions.get( chain2._headUri ).size();
      if ( headDiff > 0 ) {
         return chain1;
      } else if ( headDiff < 0 ) {
         return chain2;
      }
      final int allDiff = chain1._uriMentions.values()
                                      .stream()
                                      .mapToInt( Collection::size )
                                      .sum()
                          - chain2._uriMentions.values()
                                                   .stream()
                                                   .mapToInt( Collection::size )
                                                   .sum();
      if ( allDiff > 0 ) {
         return chain1;
      } else if ( allDiff < 0 ) {
         return chain2;
      }
      final int uriDiff = chain1._uriMentions.size() - chain2._uriMentions.size();
      if ( uriDiff > 0 ) {
         return chain1;
      } else if ( uriDiff < 0 ) {
         return chain2;
      }
      return chain1;
   }



   public String toString() {
      return "NeoplasmChain "
             + _headUri + " : " + _uriMentions.entrySet()
                                              .stream()
                                              .map( e -> e.getKey() + " " + e.getValue().size() )
                                              .collect( Collectors.joining( ";" ) ) + "\n";
   }


}
