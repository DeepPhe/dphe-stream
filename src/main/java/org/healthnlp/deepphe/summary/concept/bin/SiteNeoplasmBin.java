package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.CANCER;
import static org.healthnlp.deepphe.summary.concept.bin.NeoplasmType.TUMOR;

/**
 * @author SPF , chip-nlp
 * @since {6/3/2021}
 */
final public class SiteNeoplasmBin {

   static private final Logger LOGGER = Logger.getLogger( "SiteNeoplasmBin" );

   private final SiteChain _siteChain;
   private final Map<NeoplasmType,Collection<NeoplasmChain>> _neoplasmChainsMap;
   private boolean _valid;


   SiteNeoplasmBin( final String siteUri,
                    final Map<String,Set<Mention>> neoplasmSiteUriSites,
                    final Collection<Mention> cancers,
                    final Collection<Mention> tumors ) {
      this( new SiteChain( siteUri, neoplasmSiteUriSites ),
            createNeoplasmChains( cancers ), createNeoplasmChains( tumors ) );
   }

   SiteNeoplasmBin( final SiteChain siteChain,
                    final Collection<NeoplasmChain> cancerChains,
                    final Collection<NeoplasmChain> tumorChains ) {
      this( siteChain, createChainsMap( cancerChains, tumorChains ) );
   }

   SiteNeoplasmBin( final SiteChain siteChain,
                    final Map<NeoplasmType,Collection<NeoplasmChain>> neoplasmChainsMap ) {
      _siteChain = siteChain;
      _neoplasmChainsMap = neoplasmChainsMap;
      _valid = true;
   }

   static private Map<NeoplasmType,Collection<NeoplasmChain>> createChainsMap( final Collection<NeoplasmChain> cancerChains,
                                   final Collection<NeoplasmChain> tumorChains ) {
      final Map<NeoplasmType,Collection<NeoplasmChain>> chainsMap = new EnumMap<>( NeoplasmType.class );
      chainsMap.put( CANCER, cancerChains );
      chainsMap.put( TUMOR, tumorChains );
      return chainsMap;
   }

   // Using isValid should decrease introduced errors from copying things around.  Used with mergeToNew(..)
   boolean isValid() {
      return _valid
             && _neoplasmChainsMap.values()
                                  .stream()
                                  .flatMap( Collection::stream )
                                  .allMatch( NeoplasmChain::isValid );
   }

   void invalidate() {
      _valid = false;
      _siteChain.invalidate();
      _neoplasmChainsMap.values()
                        .stream()
                        .flatMap( Collection::stream )
                        .forEach( NeoplasmChain::invalidate );
   }

   void clean() {
      final boolean empty = _neoplasmChainsMap.values()
                                              .stream()
                                              .allMatch( this::clean );
      if ( empty ) {
         invalidate();
      }
   }

   private boolean clean( final Collection<NeoplasmChain> neoplasmChains ) {
      Collection<NeoplasmChain> invalid = neoplasmChains.stream()
                                                       .filter( c -> !c.isValid() )
                                                       .collect( Collectors.toSet() );
      neoplasmChains.removeAll( invalid );
      return neoplasmChains.isEmpty();
   }

   SiteChain getSiteChain() {
      return _siteChain;
   }

   Collection<NeoplasmChain> getNeoplasmChains( final NeoplasmType neoplasmType ) {
      return _neoplasmChainsMap.get( neoplasmType );
   }

   static private Collection<NeoplasmChain> createNeoplasmChains( final Collection<Mention> neoplasms ) {
      final Map<String, Collection<Mention>> neoplasmUriMentions = new HashMap<>();
      for ( Mention neoplasm : neoplasms ) {
         final String neoplasmUri = neoplasm.getClassUri();
         neoplasmUriMentions.computeIfAbsent( neoplasmUri, u -> new HashSet<>() )
                    .add( neoplasm );
      }
      return createNeoplasmChains( neoplasmUriMentions );
   }

   static private Collection<NeoplasmChain> createNeoplasmChains( final Map<String,
                                                                        Collection<Mention>> neoplasmUriMentions ) {
      final Collection<NeoplasmChain> neoplasmChains = new HashSet<>();
      final Map<String, Collection<String>> neoplasmUriChains
            = UriUtil.getAssociatedUriMap( neoplasmUriMentions.keySet() );
      for ( Map.Entry<String, Collection<String>> neoplasmUriChain : neoplasmUriChains.entrySet() ) {
         final Map<String,Collection<Mention>> chainUriMentions
               = neoplasmUriChain.getValue()
                                 .stream()
                                 .collect( Collectors.toMap(
                                       Function.identity(),
                                       neoplasmUriMentions::get ) );
         neoplasmChains.add( new NeoplasmChain( neoplasmUriChain.getKey(),
                                                 chainUriMentions ) );
      }
      return neoplasmChains;
   }

   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SITE MATCHING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   // Done By Site
   KeyValue<Long,Collection<SiteNeoplasmBin>> scoreBestMatchingSites( final Collection<SiteNeoplasmBin> otherBins ) {
      long bestScore = 0;
      final Collection<SiteNeoplasmBin> bestBins = new HashSet<>();
      for ( SiteNeoplasmBin otherBin : otherBins ) {
         final long score = scoreSiteMatch( otherBin );
         if ( score > 0 && score >= bestScore ) {
            if ( score > bestScore ) {
               bestScore = score;
               bestBins.clear();
            }
            bestBins.add( otherBin );
         }
      }
      return new KeyValue<>( bestScore, bestBins );
   }

   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SITE MATCHING     BY ROOTS
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   // Done By Site
   KeyValue<Long,Collection<SiteNeoplasmBin>> scoreBestMatchingSitesByRoots( final Collection<SiteNeoplasmBin> otherBins,
                                                                      final Map<String,Collection<String>> allUriRoots ) {
      long bestScore = 0;
      final Collection<SiteNeoplasmBin> bestBins = new HashSet<>();
      for ( SiteNeoplasmBin otherBin : otherBins ) {
         final long score = scoreSiteRootsMatch( otherBin, allUriRoots );
         if ( score > 0 && score >= bestScore ) {
            if ( score > bestScore ) {
               bestScore = score;
               bestBins.clear();
            }
            bestBins.add( otherBin );
         }
      }
      return new KeyValue<>( bestScore, bestBins );
   }



   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NEOPLASM MATCHING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasms(
         final NeoplasmType neoplasmType,
         final Collection<SiteNeoplasmBin> otherBins ) {
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin otherBin : otherBins ) {
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchMap =
               scoreBestMatchingNeoplasms( neoplasmType, otherBin );
         for ( Map.Entry<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchEntry :
               bestMatchMap.entrySet() ) {
            final KeyValue<Long, Collection<NeoplasmChain>> bestMatch =
                  bestMatchingChainsMap.get( bestMatchEntry.getKey() );
            if ( bestMatch == null ) {
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatchEntry.getValue() );
               continue;
            }
            if ( bestMatchEntry.getValue()
                               .getKey() > bestMatch.getKey() ) {
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatchEntry.getValue() );
            } else if ( bestMatchEntry.getValue()
                                      .getKey()
                                      .equals( bestMatch.getKey() ) ) {
               bestMatch.getValue()
                        .addAll( bestMatchEntry.getValue()
                                               .getValue() );
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatch );
            }
         }
      }
      return bestMatchingChainsMap;
   }

   Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasms(
         final NeoplasmType neoplasmType,
         final SiteNeoplasmBin otherBin ) {
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      final Collection<NeoplasmChain> otherNeoplasmChains = otherBin.getNeoplasmChains( neoplasmType );
      for ( NeoplasmChain neoplasmChain : getNeoplasmChains( neoplasmType ) ) {
         final KeyValue<Long,Collection<NeoplasmChain>> bestMatches
               = neoplasmChain.getBestMatchingChains( otherNeoplasmChains );
         if ( bestMatches.getKey() <= 0 ) {
            continue;
         }
         bestMatchingChainsMap.put( neoplasmChain, bestMatches );
      }
      return bestMatchingChainsMap;
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NEOPLASM MATCHING   BY ROOTS
   //
   ////////////////////////////////////////////////////////////////////////////////////////



   Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmsByRoots(
         final NeoplasmType neoplasmType,
         final Collection<SiteNeoplasmBin> otherBins,
         final Map<String,Collection<String>> allUriRoots ) {
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin otherBin : otherBins ) {
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchMap =
               scoreBestMatchingNeoplasmsByRoots( neoplasmType, otherBin, allUriRoots );
         for ( Map.Entry<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchEntry :
               bestMatchMap.entrySet() ) {
            final KeyValue<Long, Collection<NeoplasmChain>> bestMatch =
                  bestMatchingChainsMap.get( bestMatchEntry.getKey() );
            if ( bestMatch == null ) {
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatchEntry.getValue() );
               continue;
            }
            if ( bestMatchEntry.getValue()
                               .getKey() > bestMatch.getKey() ) {
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatchEntry.getValue() );
            } else if ( bestMatchEntry.getValue()
                                      .getKey()
                                      .equals( bestMatch.getKey() ) ) {
               bestMatch.getValue()
                        .addAll( bestMatchEntry.getValue()
                                               .getValue() );
               bestMatchingChainsMap.put( bestMatchEntry.getKey(), bestMatch );
            }
         }
      }
      return bestMatchingChainsMap;
   }

   Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmsByRoots(
         final NeoplasmType neoplasmType,
         final SiteNeoplasmBin otherBin,
         final Map<String,Collection<String>> allUriRoots ) {
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      final Collection<NeoplasmChain> otherNeoplasmChains = otherBin.getNeoplasmChains( neoplasmType );
      for ( NeoplasmChain neoplasmChain : getNeoplasmChains( neoplasmType ) ) {
         final KeyValue<Long,Collection<NeoplasmChain>> bestMatches
               = neoplasmChain.getBestMatchingChains( otherNeoplasmChains, allUriRoots );
         if ( bestMatches.getKey() <= 0 ) {
            continue;
         }
         bestMatchingChainsMap.put( neoplasmChain, bestMatches );
      }
      return bestMatchingChainsMap;
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            SITE SCORING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   long scoreSiteMatch( final SiteNeoplasmBin otherChain ) {
      long score = _siteChain.scoreSiteByUrisMatch( otherChain._siteChain );
      if ( score > 0 ) {
         score = score * 10 + scoreNeoplasmsMatch( CANCER, otherChain.getNeoplasmChains( CANCER )
                                               .stream()
                                               .map( NeoplasmChain::getHeadUri )
                                               .collect( Collectors.toSet() ) );
         score = score * 10 + scoreNeoplasmsMatch( TUMOR, otherChain.getNeoplasmChains( TUMOR )
                                                             .stream()
                                                             .map( NeoplasmChain::getHeadUri )
                                                             .collect( Collectors.toSet() ) );
//         LOGGER.info( "Site Match " + _siteChain.getHeadUri() + " score vs "
//                      + otherChain._siteChain.getHeadUri() + " = " + score );
      }
      return score;
   }

   long scoreSiteRootsMatch( final SiteNeoplasmBin otherBin, final Map<String,Collection<String>> allUriRoots ) {
      long score = _siteChain.scoreSiteRootsMatch( otherBin._siteChain, allUriRoots );
      if ( score > 0 ) {
         score  = score * 5 + scoreNeoplasmsMatch( CANCER, otherBin.getNeoplasmChains( CANCER )
                                                 .stream()
                                                 .map( NeoplasmChain::getHeadUri )
                                                 .collect( Collectors.toSet() ) );
         score  = score * 5 + scoreNeoplasmsMatch( TUMOR, otherBin.getNeoplasmChains( TUMOR )
                                                           .stream()
                                                           .map( NeoplasmChain::getHeadUri )
                                                           .collect( Collectors.toSet() ) );

//         LOGGER.info( "Site Roots Match " + _siteChain.getHeadUri() + " score vs "
//                      + otherBin._siteChain.getHeadUri() + " = " + score );
      }
      return score;
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            NEOPLASM SCORING
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   long scoreNeoplasmsMatch( final NeoplasmType neoplasmType, final Collection<String> neoplasmUris ) {
      return getNeoplasmChains( neoplasmType ).stream()
                          .mapToLong( c -> c.scoreNeoplasmUrisMatch( neoplasmUris ) )
                          .sum();
   }


   long scoreNeoplasmMatch( final NeoplasmType neoplasmType, final String neoplasmUri ) {
      return getNeoplasmChains( neoplasmType ).stream()
                          .mapToLong( c -> c.scoreNeoplasmUriMatch( neoplasmUri ) )
                          .sum();
   }

   long scoreNeoplasmRootsMatch( final NeoplasmType neoplasmType,
                                 final String neoplasmUri,
                                 final Map<String,Collection<String>> allUriRoots ) {
      return getNeoplasmChains( neoplasmType ).stream()
                          .mapToLong( c -> c.scoreNeoplasmRootsMatch( neoplasmUri, allUriRoots ) )
                          .sum();
   }



   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            MERGING
   //
   ////////////////////////////////////////////////////////////////////////////////////////



   void copyInto( final SiteNeoplasmBin otherBin ) {
      _siteChain.copyInto( otherBin.getSiteChain() );
      copyNeoplasmChains( CANCER, otherBin );
      copyNeoplasmChains( TUMOR, otherBin );
   }

   void copyNeoplasmChains( final NeoplasmType neoplasmType, final SiteNeoplasmBin otherBin ) {
      final Collection<Mention> neoplasmMentions = getNeoplasmChains( neoplasmType ).stream()
                                                                  .map( NeoplasmChain::getAllMentions )
                                                                  .flatMap( Collection::stream )
                                                                  .collect( Collectors.toSet() );
      otherBin.getNeoplasmChains( neoplasmType )
              .stream()
              .map( NeoplasmChain::getAllMentions )
              .forEach( neoplasmMentions::addAll );
      getNeoplasmChains( neoplasmType ).clear();
      getNeoplasmChains( neoplasmType ).addAll( createNeoplasmChains( neoplasmMentions ) );
   }


   void copyNeoplasmChain( final NeoplasmType neoplasmType, final NeoplasmChain neoplasmChain ) {
      final Collection<Mention> neoplasmMentions = getNeoplasmChains( neoplasmType ).stream()
                                                                  .map( NeoplasmChain::getAllMentions )
                                                                  .flatMap( Collection::stream )
                                                                  .collect( Collectors.toSet() );
      neoplasmMentions.addAll( neoplasmChain.getAllMentions() );
      getNeoplasmChains( neoplasmType ).clear();
      getNeoplasmChains( neoplasmType ).addAll( createNeoplasmChains( neoplasmMentions ) );
   }



   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            CONCEPT AGGREGATE FORMATION
   //
   ////////////////////////////////////////////////////////////////////////////////////////





   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //                            UTIL
   //
   ////////////////////////////////////////////////////////////////////////////////////////


   public String toString() {
      return "SiteNeoplasmBin at "
             + _siteChain.toString()
             + "\n  Cancers :\n"
             + getNeoplasmChains( CANCER ).stream()
                               .map( NeoplasmChain::toString )
                               .collect( Collectors.joining() )
             + "\n  Tumors :\n"
             + getNeoplasmChains( TUMOR ).stream()
                                          .map( NeoplasmChain::toString )
                                          .collect( Collectors.joining() );
   }


}
