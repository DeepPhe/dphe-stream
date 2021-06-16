package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {6/3/2021}
 */
final public class SiteTypeBin {

   static private final Logger LOGGER = Logger.getLogger( "SiteTypeBin" );


   private final SiteType _siteType;
   private final Collection<SiteNeoplasmBin> _siteNeoplasmBins = new ArrayList<>();

   static private final String NO_SITE_URI = "NO_SITE_URI";

   SiteTypeBin( final SiteType siteType ) {
      _siteType = siteType;
   }


   static Map<SiteType,Collection<Mention>> getSiteTypes( final Collection<Mention> neoplasms,
                                                          final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      final Map<SiteType,Collection<Mention>> siteTypes = new EnumMap<>( SiteType.class );
      for ( Mention neoplasm : neoplasms ) {
         final Map<String, Collection<Mention>> relations = relationsMap.get( neoplasm );
         if ( relations == null || relations.isEmpty() ) {
            siteTypes.computeIfAbsent( SiteType.NO_SITE, b -> new HashSet<>() )
                        .add( neoplasm );
         } else {
            siteTypes.computeIfAbsent( SiteType.getSiteType( relations.keySet() ), b -> new HashSet<>() )
                     .add( neoplasm );
         }
      }
      return siteTypes;
   }

   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      if ( _siteType == SiteType.NO_SITE ) {
         setNoTypeNeoplasms( cancers, tumors, relationsMap );
         return;
      }
      final Collection<String> allSiteUris = new HashSet<>();
      final Map<String,Collection<Mention>> siteUriCancersMap = new HashMap<>();
      final Map<String,Collection<Mention>> siteUriTumorsMap = new HashMap<>();
      final Map<Mention,Map<String,Set<Mention>>> neoplasmSiteUriSitesMap = new HashMap<>();
      for ( Mention cancer : cancers ) {
         final Map<String,Collection<Mention>> relations = relationsMap.get( cancer );
         if ( relations == null || relations.isEmpty() ) {
            continue;
         }
         final Map<String,Set<Mention>> siteUriSitesMap = _siteType.getMatchingSites( relations );
         if ( !siteUriSitesMap.isEmpty() ) {
            allSiteUris.addAll( siteUriSitesMap.keySet() );
            siteUriSitesMap.keySet()
                   .forEach( s -> siteUriCancersMap.computeIfAbsent( s, n -> new HashSet<>() )
                                                     .add( cancer ) );
            neoplasmSiteUriSitesMap.put( cancer, siteUriSitesMap );
         }
      }
      for ( Mention tumor : tumors ) {
         final Map<String,Collection<Mention>> relations = relationsMap.get( tumor );
         if ( relations == null || relations.isEmpty() ) {
            continue;
         }
         final Map<String,Set<Mention>> siteUriSitesMap = _siteType.getMatchingSites( relations );
         if ( !siteUriSitesMap.isEmpty() ) {
            allSiteUris.addAll( siteUriSitesMap.keySet() );
            siteUriSitesMap.keySet()
                           .forEach( s -> siteUriTumorsMap.computeIfAbsent( s, n -> new HashSet<>() )
                                                             .add( tumor ) );
            neoplasmSiteUriSitesMap.put( tumor, siteUriSitesMap );
         }
      }
      createSiteNeoplasmBins( allSiteUris, siteUriCancersMap, siteUriTumorsMap, neoplasmSiteUriSitesMap );
   }


   void setNoTypeNeoplasms( final Collection<Mention> cancers,
                            final Collection<Mention> tumors,
                            final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      final Collection<Mention> noSiteCancers = new HashSet<>();
      final Collection<Mention> noSiteTumors = new HashSet<>();
      for ( Mention cancer : cancers ) {
         final Map<String, Collection<Mention>> relations = relationsMap.get( cancer );
         if ( relations == null || relations.isEmpty() ) {
            if ( _siteType == SiteType.NO_SITE ) {
               noSiteCancers.add( cancer );
            }
            continue;
         }
         if ( SiteType.isNoSiteType( relations.keySet() ) ) {
            noSiteCancers.add( cancer );
         }
      }
      for ( Mention tumor : tumors ) {
         final Map<String, Collection<Mention>> relations = relationsMap.get( tumor );
         if ( relations == null || relations.isEmpty() ) {
            if ( _siteType == SiteType.NO_SITE ) {
               noSiteTumors.add( tumor );
            }
            continue;
         }
         if ( SiteType.isNoSiteType( relations.keySet() ) ) {
            noSiteTumors.add( tumor );
         }
      }
      final SiteNeoplasmBin noSiteNeoplasmBin = new SiteNeoplasmBin( NO_SITE_URI,
                                                                     Collections.singletonMap( NO_SITE_URI,
                                                                                               Collections.emptySet() ),
                                                                     noSiteCancers,
                                                                     noSiteTumors );
      _siteNeoplasmBins.add( noSiteNeoplasmBin );
   }


   void createSiteNeoplasmBins( final Collection<String> allSiteUris,
                                final Map<String,Collection<Mention>> siteUriCancersMap,
                                final Map<String,Collection<Mention>> siteUriTumorsMap,
                                final Map<Mention,Map<String,Set<Mention>>> neoplasmSiteUriSitesMap ) {
      final Map<String,Collection<String>> siteChains = UriUtil.getAssociatedUriMap( allSiteUris );
      for ( Map.Entry<String,Collection<String>> siteChain : siteChains.entrySet() ) {
         final Map<String,Set<Mention>> neoplasmSiteUriSites = new HashMap<>();
         final Collection<Mention> cancers = siteChain.getValue()
                                                        .stream()
                                                        .map( siteUriCancersMap::get )
                                                      .filter( Objects::nonNull )
                                                      .flatMap( Collection::stream )
                                                        .collect( Collectors.toSet() );
         for ( Mention cancer : cancers ) {
            final Map<String,Set<Mention>> siteUriSites = neoplasmSiteUriSitesMap.get( cancer );
            siteUriSites.forEach( (k,v) -> neoplasmSiteUriSites.computeIfAbsent( k, u -> new HashSet<>() )
                                                              .addAll( v ) );
         }
         final Collection<Mention> tumors = siteChain.getValue()
                                                      .stream()
                                                      .map( siteUriTumorsMap::get )
                                                     .filter( Objects::nonNull )
                                                     .flatMap( Collection::stream )
                                                      .collect( Collectors.toSet() );
         for ( Mention tumor : tumors ) {
            final Map<String,Set<Mention>> siteUriSites = neoplasmSiteUriSitesMap.get( tumor );
            siteUriSites.forEach( (k,v) -> neoplasmSiteUriSites.computeIfAbsent( k, u -> new HashSet<>() )
                                                               .addAll( v ) );
         }
         final SiteNeoplasmBin siteNeoplasmBin = new SiteNeoplasmBin( siteChain.getKey(),
                                                                      neoplasmSiteUriSites,
                                                                      cancers,
                                                                      tumors );
         _siteNeoplasmBins.add( siteNeoplasmBin );
      }
   }


   void clean() {
      _siteNeoplasmBins.forEach( SiteNeoplasmBin::clean );
      final Collection<SiteNeoplasmBin> invalid = _siteNeoplasmBins.stream()
                                                                   .filter( b -> !b.isValid() )
                                                                   .collect( Collectors.toSet() );
      _siteNeoplasmBins.removeAll( invalid );
   }

   void clear() {
      _siteNeoplasmBins.clear();
   }



   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreBestMatchingSiteNeoplasmBins(
         final SiteTypeBin otherSiteTypeBin ) {
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap
            = scoreMatchingSiteNeoplasmBins( otherSiteTypeBin );
      return getBestScoresMatchingSiteNeoplasmBins( matchingBinsMap );
   }


   // For moving no-site neoplasm chains here.
   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreMatchingSiteNeoplasmBins(
         final SiteTypeBin otherSiteTypeBin ) {
      final Map<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap = new HashMap<>();
      final Collection<SiteNeoplasmBin> otherSiteNeoplasmBins = otherSiteTypeBin.getSiteNeoplasmBins();
      for ( SiteNeoplasmBin siteNeoplasmBin : _siteNeoplasmBins ) {
         final KeyValue<Long,Collection<SiteNeoplasmBin>> bestMatches
               = siteNeoplasmBin.scoreBestMatchingSites( otherSiteNeoplasmBins );
         if ( bestMatches.getKey() <= 0 ) {
            continue;
         }
         matchingBinsMap.put( siteNeoplasmBin, bestMatches );
      }
      return matchingBinsMap;
   }

   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> getBestScoresMatchingSiteNeoplasmBins(
         final Map<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> bestMatchingBinsMap ) {
      Map<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> adjustedMatchingBinsMap = new HashMap<>();
      final Map<SiteNeoplasmBin,Long> bestOpposingScores = getBestOpposingScores( bestMatchingBinsMap );
      for ( Map.Entry<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> bestMatchingBins
            : bestMatchingBinsMap.entrySet() ) {
         final Long score = bestMatchingBins.getValue().getKey();
         if ( score <= 0 ) {
            continue;
         }
         final Collection<SiteNeoplasmBin> bestBins = new HashSet<>();
         for ( SiteNeoplasmBin otherBin : bestMatchingBins.getValue()
                                                          .getValue() ) {
            final Long bestScore = bestOpposingScores.get( otherBin );
            if ( bestScore != null && bestScore.equals( score ) ) {
               bestBins.add( otherBin );
            }
         }
         if ( !bestBins.isEmpty() ) {
            adjustedMatchingBinsMap.put( bestMatchingBins.getKey(), new KeyValue<>( score, bestBins ) );
         }
      }
      return adjustedMatchingBinsMap;
   }

   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreBestMatchingNeoplasmBinsByRoots(
         final SiteTypeBin otherSiteTypeBin,
         final Collection<SiteNeoplasmBin> notLocated,
         final Map<String,Collection<String>> allUriRoots ) {
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap
            = scoreMatchingSiteNeoplasmBinsByRoots( otherSiteTypeBin, notLocated, allUriRoots );
      return getBestScoresMatchingSiteNeoplasmBins( matchingBinsMap );
   }


   Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> scoreMatchingSiteNeoplasmBinsByRoots(
         final SiteTypeBin otherSiteTypeBin,
         final Collection<SiteNeoplasmBin> notLocated,
         final Map<String,Collection<String>> allUriRoots ) {
      final Map<SiteNeoplasmBin,KeyValue<Long,Collection<SiteNeoplasmBin>>> matchingBinsMap = new HashMap<>();
      final Collection<SiteNeoplasmBin> otherSiteNeoplasmBins = new HashSet<>( otherSiteTypeBin.getSiteNeoplasmBins() );
      otherSiteNeoplasmBins.retainAll( notLocated );
      for ( SiteNeoplasmBin siteNeoplasmBin : _siteNeoplasmBins ) {
         final KeyValue<Long,Collection<SiteNeoplasmBin>> bestMatches
               = siteNeoplasmBin.scoreBestMatchingSitesByRoots( otherSiteNeoplasmBins, allUriRoots );
         if ( bestMatches.getKey() <= 0 ) {
            continue;
         }
         matchingBinsMap.put( siteNeoplasmBin, bestMatches );
      }
      return matchingBinsMap;
   }


   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmChains(
         final SiteTypeBin otherSiteTypeBin ) {
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> matchingBinsMap
            = scoreMatchingNeoplasmChains( NeoplasmType.CANCER, otherSiteTypeBin );
      matchingBinsMap.putAll( scoreMatchingNeoplasmChains( NeoplasmType.TUMOR, otherSiteTypeBin ) );
      return getBestScoresMatchingNeoplasmChains( matchingBinsMap );
   }



   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmChains(
         final NeoplasmType neoplasmType,
         final SiteTypeBin otherSiteTypeBin ) {
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> matchingBinsMap
            = scoreMatchingNeoplasmChains( neoplasmType, otherSiteTypeBin );
      return getBestScoresMatchingNeoplasmChains( matchingBinsMap );
   }


   // For moving no-site neoplasm chains here.
   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreMatchingNeoplasmChains(
         final NeoplasmType neoplasmType,
         final SiteTypeBin otherSiteTypeBin ) {
      final Collection<SiteNeoplasmBin> otherSiteNeoplasmBins = otherSiteTypeBin.getSiteNeoplasmBins();
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : _siteNeoplasmBins ) {
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchMap
               = siteNeoplasmBin.scoreBestMatchingNeoplasms( neoplasmType, otherSiteNeoplasmBins );

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


   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> getBestScoresMatchingNeoplasmChains(
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap ) {
      Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> adjustedMatchingBinsMap = new HashMap<>();
      final Map<NeoplasmChain,Long> bestOpposingScores = getBestOpposingChainScores( bestMatchingChainsMap );
      for ( Map.Entry<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingBins
            : bestMatchingChainsMap.entrySet() ) {
         final Long score = bestMatchingBins.getValue().getKey();
         if ( score <= 0 ) {
            continue;
         }
         final Collection<NeoplasmChain> bestBins = new HashSet<>();
         for ( NeoplasmChain otherBin : bestMatchingBins.getValue()
                                                          .getValue() ) {
            final Long bestScore = bestOpposingScores.get( otherBin );
            if ( bestScore != null && bestScore.equals( score ) ) {
               bestBins.add( otherBin );
            }
         }
         if ( !bestBins.isEmpty() ) {
            adjustedMatchingBinsMap.put( bestMatchingBins.getKey(), new KeyValue<>( score, bestBins ) );
         }
      }
      return adjustedMatchingBinsMap;
   }



   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreBestMatchingNeoplasmChains(
         final NeoplasmType neoplasmType,
         final SiteTypeBin otherSiteTypeBin,
         final Map<String,Collection<String>> allUriRoots ) {
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> matchingBinsMap
            = scoreMatchingNeoplasmChains( neoplasmType, otherSiteTypeBin, allUriRoots );
      return getBestScoresMatchingNeoplasmChains( matchingBinsMap );
   }


   // For moving no-site neoplasm chains here.
   Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> scoreMatchingNeoplasmChains(
         final NeoplasmType neoplasmType,
         final SiteTypeBin otherSiteTypeBin,
         final Map<String,Collection<String>> allUriRoots ) {
      final Collection<SiteNeoplasmBin> otherSiteNeoplasmBins = otherSiteTypeBin.getSiteNeoplasmBins();
      final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchingChainsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : _siteNeoplasmBins ) {
         final Map<NeoplasmChain,KeyValue<Long,Collection<NeoplasmChain>>> bestMatchMap
               = siteNeoplasmBin.scoreBestMatchingNeoplasmsByRoots( neoplasmType, otherSiteNeoplasmBins, allUriRoots );

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






   static Map<SiteNeoplasmBin,Long> getBestOpposingScores( final Map<SiteNeoplasmBin,KeyValue<Long,
         Collection<SiteNeoplasmBin>>> bestMatchingBinsMap ) {
      final Map<SiteNeoplasmBin,Long> bestScores = new HashMap<>();
      for ( KeyValue<Long,Collection<SiteNeoplasmBin>> bestMatchingBins
            : bestMatchingBinsMap.values() ) {
         if ( bestMatchingBins.getKey() <= 0 ) {
            continue;
         }
         final Long score = bestMatchingBins.getKey();
         for ( SiteNeoplasmBin oppositeBin : bestMatchingBins.getValue() ) {
            Long bestScore = bestScores.get( oppositeBin );
            if ( bestScore == null || bestScore < score ) {
               bestScores.put( oppositeBin, score );
            }
         }
      }
      return bestScores;
   }



   static Map<NeoplasmChain,Long> getBestOpposingChainScores( final Map<NeoplasmChain,KeyValue<Long,
         Collection<NeoplasmChain>>> bestMatchingChainsMap ) {
      final Map<NeoplasmChain,Long> bestScores = new HashMap<>();
      for ( KeyValue<Long,Collection<NeoplasmChain>> bestMatchingChains
            : bestMatchingChainsMap.values() ) {
         if ( bestMatchingChains.getKey() <= 0 ) {
            continue;
         }
         final Long score = bestMatchingChains.getKey();
         for ( NeoplasmChain oppositeBin : bestMatchingChains.getValue() ) {
            Long bestScore = bestScores.get( oppositeBin );
            if ( bestScore == null || bestScore < score ) {
               bestScores.put( oppositeBin, score );
            }
         }
      }
      return bestScores;
   }




   Collection<SiteNeoplasmBin> getSiteNeoplasmBins() {
      return _siteNeoplasmBins;
   }


   public String toString() {
      return "SiteTypeBin site neoplasm chains :\n"
             + _siteNeoplasmBins.stream()
                                .map( SiteNeoplasmBin::toString )
                                .collect( Collectors.joining( "\n" ) );
   }


}
