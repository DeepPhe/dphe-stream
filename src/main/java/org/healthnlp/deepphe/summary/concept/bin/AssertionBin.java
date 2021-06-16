package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.util.KeyValue;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.concept.bin.LateralityType.*;
import static org.healthnlp.deepphe.summary.concept.bin.SiteType.ALL_SITES;
import static org.healthnlp.deepphe.summary.concept.bin.SiteType.NO_SITE;


/**
 * @author SPF , chip-nlp
 * @since {5/24/2021}
 */
final public class AssertionBin {

   static private final Logger LOGGER = Logger.getLogger( "AssertionBin" );


private final Map<LateralityType, LateralityTypeBin> _lateralityBins = new EnumMap<>( LateralityType.class );


   public void clear() {
      getOrCreateLateralityTypeBins().values()
                                     .forEach( LateralityTypeBin::clear );
   }

   static Map<Boolean,List<Mention>> getAssertionTypes( final Collection<Mention> mentions ) {
      return mentions.stream()
                     .collect( Collectors.groupingBy( Mention::isNegated  ) );
   }

   static public Collection<Mention> getAffirmedMentions( final Collection<Mention> mentions ) {
      return mentions.stream()
                     .filter( m -> !m.isNegated() )
                     .collect( Collectors.toSet() );
   }


   public Collection<Mention> splitMentions( final Collection<Mention> mentions,
                                             final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      clear();
      final Map<BinDistributor.MentionType,Collection<Mention>> categoryMentionsMap
            = splitMentionTypes( mentions );
      setNeoplasms( categoryMentionsMap.get( BinDistributor.MentionType.CANCER ),
                    categoryMentionsMap.get( BinDistributor.MentionType.TUMOR ),
                    relationsMap  );
      return categoryMentionsMap.get( BinDistributor.MentionType.OTHER );
   }

   static Map<BinDistributor.MentionType,Collection<Mention>> splitMentionTypes(
         final Collection<Mention> mentions ) {
      final Map<BinDistributor.MentionType,Collection<Mention>> categoryMap = new EnumMap<>( BinDistributor.MentionType.class );
      final Collection<Mention> cancers = categoryMap.computeIfAbsent( BinDistributor.MentionType.CANCER, c -> new HashSet<>() );
      final Collection<Mention> tumors = categoryMap.computeIfAbsent( BinDistributor.MentionType.TUMOR, c -> new HashSet<>() );
      final Collection<Mention> others = categoryMap.computeIfAbsent( BinDistributor.MentionType.OTHER, c -> new HashSet<>() );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> neoplasmUris = UriConstants.getNeoplasmUris( graphDb );
      final Collection<String> massUris = UriConstants.getMassUris( graphDb );
      for ( Mention mention : mentions ) {
         final String uri = mention.getClassUri();
         // Some uris are both mass and neoplasm because of the meaning of "tumor".
         // Favor Neoplasm (as Cancer) over Mass (as Tumor).
         if ( neoplasmUris.contains( uri ) ) {
            cancers.add( mention );
         } else if ( massUris.contains( uri ) ) {
            tumors.add( mention );
         } else {
            others.add( mention );
         }
      }
      return categoryMap;
   }

   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      getOrCreateLateralityTypeBins();
      final Map<LateralityType,Collection<Mention>> cancerLateralities
            = LateralityTypeBin.getLateralityTypes( cancers, relationsMap );
      final Map<LateralityType,Collection<Mention>> tumorLateralities
            = LateralityTypeBin.getLateralityTypes( tumors, relationsMap );
      for ( LateralityType lateralityType : LateralityType.values() ) {
         _lateralityBins.get( lateralityType )
                        .setNeoplasms( cancerLateralities.getOrDefault( lateralityType,
                                                                        new HashSet<>( 0 ) ),
                                       tumorLateralities.getOrDefault( lateralityType,
                                                                       new HashSet<>( 0 ) ),
                                       relationsMap );
      }
   }

   Map<LateralityType,LateralityTypeBin> getOrCreateLateralityTypeBins() {
      if ( _lateralityBins.isEmpty() ) {
         Arrays.stream( LateralityType.values() )
               .forEach( l -> _lateralityBins.put( l, new LateralityTypeBin( l ) ) );
      }
      return _lateralityBins;
   }

   public void distributeSites( final Map<String,Collection<String>> allUriRoots ) {
      LOGGER.info( "Assertion Bin Distributing Sites ..." );
      // Within each laterality bin, attempt to assign sites to any neoplasms that do not already have sites.
      _lateralityBins.values().forEach( b -> b.distributeSites( allUriRoots ) );
      LOGGER.info( "Assertion Bin Distributing No Lateralities ..." );
      distributeNoLateralities( allUriRoots );
      LOGGER.info( "Assertion Bin Improving Lateralities ..." );
      improveLateralities( allUriRoots );
   }


   private SiteTypeBin getSiteTypeBin( final LateralityType lateralityType, final SiteType siteType ) {
      final LateralityTypeBin lateralityTypeBin = getOrCreateLateralityTypeBins().get( lateralityType );
      return lateralityTypeBin.getSiteTypeBin( siteType );
   }

   void distributeNoLateralities( final Map<String, Collection<String>> allUriRoots ) {
      LOGGER.info( "!!! Assertion Bin Distributing No Lateralities !!!" );
      final SiteTypeBin noLateralityBin = getSiteTypeBin( NO_LATERALITY, NO_SITE );
      final Collection<SiteNeoplasmBin> notLocated = new HashSet<>();
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, NO_SITE ) );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> rightScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( RIGHT, NO_SITE ) );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> bilateralScores
            = noLateralityBin.scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( BILATERAL, NO_SITE ) );
      final Collection<SiteNeoplasmBin> noLateralitySites = noLateralityBin.getSiteNeoplasmBins();
      for ( SiteNeoplasmBin noSiteBin : noLateralitySites ) {
         final KeyValue<Long,Collection<SiteNeoplasmBin>> leftScore = leftScores.get( noSiteBin );
         final KeyValue<Long,Collection<SiteNeoplasmBin>> rightScore = rightScores.get( noSiteBin );
         final KeyValue<Long,Collection<SiteNeoplasmBin>> bilateralScore = bilateralScores.get( noSiteBin );
         boolean located = false;
         if ( leftScore != null && leftScore.getKey() > 0 ) {
            if ( rightScore == null || leftScore.getKey() >= rightScore.getKey() ) {
               if ( bilateralScore == null || leftScore.getKey() >= bilateralScore.getKey() ) {
                  LOGGER.info( "Left Score " + leftScore.getKey() + " for " + noSiteBin.toString() + " vs. left\n"
                               + leftScore.getValue()
                                          .stream()
                                          .map( SiteNeoplasmBin::toString )
                                          .collect( Collectors.joining("\n  " ) )
                               + "\n> Right Score "
                               + (rightScore == null ? "null"
                                                     : (rightScore.getKey() + "\n  "
                                                        + rightScore.getValue()
                                                                    .stream()
                                                                    .map( SiteNeoplasmBin::toString )
                                                                    .collect( Collectors.joining(
                                                                          "\n  " ) ))) );
                  leftScore.getValue()
                           .forEach( r -> r.copyInto( noSiteBin ) );
                  noSiteBin.invalidate();
                  located = true;
               }
            }
         }
         if ( rightScore != null && rightScore.getKey() > 0 ) {
            if ( leftScore == null || rightScore.getKey() >= leftScore.getKey() ) {
               if ( bilateralScore == null || rightScore.getKey() >= bilateralScore.getKey() ) {
                  LOGGER.info( "Right Score " + rightScore.getKey() + " for " + noSiteBin.toString() + " vs. right\n"
                               + rightScore.getValue()
                                           .stream()
                                           .map( SiteNeoplasmBin::toString )
                                           .collect( Collectors.joining("\n  " ) )
                               + "\n> Left Score "
                               + (leftScore == null ? "null"
                                                    : (rightScore.getKey() + "\n  "
                                                       + rightScore.getValue()
                                                                   .stream()
                                                                   .map( SiteNeoplasmBin::toString )
                                                                   .collect( Collectors.joining(
                                                                         "\n  " ) ))) );
                  rightScore.getValue()
                            .forEach( r -> r.copyInto( noSiteBin ) );
                  noSiteBin.invalidate();
                  located = true;
               }
            }
         }
         if ( bilateralScore != null && bilateralScore.getKey() > 0 ) {
            if ( leftScore == null || bilateralScore.getKey() >= leftScore.getKey() ) {
               if ( rightScore == null || bilateralScore.getKey() >= rightScore.getKey() ) {
                  LOGGER.info( "Bilateral Score " + bilateralScore.getKey() + " for " + noSiteBin.toString() + "\n"
                               + bilateralScore.getValue()
                                               .stream()
                                               .map( SiteNeoplasmBin::toString )
                                               .collect( Collectors.joining("\n  " ) ) );
                  bilateralScore.getValue()
                                .forEach( r -> r.copyInto( noSiteBin ) );
                  noSiteBin.invalidate();
                  located = true;
               }
            }
         }
         if ( !located ) {
            notLocated.add( noSiteBin );
         }
      }
      noLateralityBin.clean();
      if ( !notLocated.isEmpty() ) {
//         notLocated.forEach( s -> LOGGER.info( "  Could not distribute laterality for " + s.toString() ) );
         distributeNoLateralitiesByRoots( notLocated, allUriRoots );
      }
      noLateralityBin.clean();
   }

   void distributeNoLateralitiesByRoots( final Collection<SiteNeoplasmBin> notLocated,
                                         final Map<String, Collection<String>> allUriRoots ) {
      LOGGER.info( "!!! Assertion Bin Distributing No Lateralities By Roots !!!" );
      final SiteTypeBin noLateralityBin = getSiteTypeBin( NO_LATERALITY, NO_SITE );
//      final Collection<SiteNeoplasmBin> notLocated = new HashSet<>();
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftScores
            = noLateralityBin.scoreBestMatchingNeoplasmBinsByRoots( getSiteTypeBin( LEFT, NO_SITE ),
                                                                    notLocated,
                                                                    allUriRoots );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> rightScores
            = noLateralityBin.scoreBestMatchingNeoplasmBinsByRoots( getSiteTypeBin( RIGHT, NO_SITE ),
                                                                    notLocated,
                                                                    allUriRoots );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> bilateralScores
            = noLateralityBin.scoreBestMatchingNeoplasmBinsByRoots( getSiteTypeBin( BILATERAL, NO_SITE ),
                                                                    notLocated,
                                                                    allUriRoots );
      final Collection<SiteNeoplasmBin> noLateralitySites = noLateralityBin.getSiteNeoplasmBins();
      for ( SiteNeoplasmBin noSiteBin : noLateralitySites ) {
         final KeyValue<Long,Collection<SiteNeoplasmBin>> leftScore = leftScores.get( noSiteBin );
         final KeyValue<Long,Collection<SiteNeoplasmBin>> rightScore = rightScores.get( noSiteBin );
         final KeyValue<Long,Collection<SiteNeoplasmBin>> bilateralScore = bilateralScores.get( noSiteBin );
         boolean located = false;
         if ( leftScore != null && leftScore.getKey() > 0 ) {
            if ( rightScore == null || leftScore.getKey() >= rightScore.getKey() ) {
               if ( bilateralScore == null || leftScore.getKey() >= bilateralScore.getKey() ) {
                  LOGGER.info( "Left Score " + leftScore.getKey() + " for " + noSiteBin.toString() + " vs. left\n"
                               + leftScore.getValue()
                                          .stream()
                                          .map( SiteNeoplasmBin::toString )
                                          .collect( Collectors.joining("\n  " ) )
                               + "\n> Right Score "
                               + (rightScore == null ? "null"
                                                     : (rightScore.getKey() + "\n  "
                                                        + rightScore.getValue()
                                                                    .stream()
                                                                    .map( SiteNeoplasmBin::toString )
                                                                    .collect( Collectors.joining(
                                                                          "\n  " ) ))) );
                  leftScore.getValue()
                           .forEach( r -> r.copyInto( noSiteBin ) );
                  noSiteBin.invalidate();
                  located = true;
               }
            }
         }
         if ( rightScore != null && rightScore.getKey() > 0 ) {
            if ( leftScore == null || rightScore.getKey() >= leftScore.getKey() ) {
               if ( bilateralScore == null || rightScore.getKey() >= bilateralScore.getKey() ) {
                  LOGGER.info( "Right Score " + rightScore.getKey() + " for " + noSiteBin.toString() + " vs. right\n"
                               + rightScore.getValue()
                                           .stream()
                                           .map( SiteNeoplasmBin::toString )
                                           .collect( Collectors.joining("\n  " ) )
                               + "\n> Left Score "
                               + (leftScore == null ? "null"
                                                    : (rightScore.getKey() + "\n  "
                                                       + rightScore.getValue()
                                                                   .stream()
                                                                   .map( SiteNeoplasmBin::toString )
                                                                   .collect( Collectors.joining(
                                                                         "\n  " ) ))) );
                  rightScore.getValue()
                            .forEach( r -> r.copyInto( noSiteBin ) );
                  noSiteBin.invalidate();
                  located = true;
               }
            }
         }
         if ( bilateralScore != null && bilateralScore.getKey() > 0 ) {
            if ( leftScore == null || bilateralScore.getKey() >= leftScore.getKey() ) {
               if ( rightScore == null || bilateralScore.getKey() >= rightScore.getKey() ) {
                  LOGGER.info( "Bilateral Score " + bilateralScore.getKey() + " for " + noSiteBin.toString() + "\n"
                               + bilateralScore.getValue()
                                               .stream()
                                               .map( SiteNeoplasmBin::toString )
                                               .collect( Collectors.joining("\n  " ) ) );
                  bilateralScore.getValue()
                                .forEach( r -> r.copyInto( noSiteBin ) );
                  noSiteBin.invalidate();
                  located = true;
               }
            }
         }
         if ( !located ) {
            notLocated.add( noSiteBin );
         }
      }
      if ( !notLocated.isEmpty() ) {
         notLocated.forEach( s -> LOGGER.info( "  Could not distribute laterality for " + s.toString() ) );
      }
   }



   void improveLateralities( final Map<String, Collection<String>> allUriRoots ) {
      improveLateralities( ALL_SITES, allUriRoots );
   }

   static private final int SWITCH_FACTOR = 3;

   void improveLateralities( final SiteType siteType, final Map<String, Collection<String>> allUriRoots ) {
      LOGGER.info( "!!! Assertion Bin Improving Lateralities !!!" );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> leftRightScores
            = getSiteTypeBin( RIGHT, siteType ).scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( LEFT, siteType ) );
      final Map<SiteNeoplasmBin, KeyValue<Long,Collection<SiteNeoplasmBin>>> rightLeftScores
            = getSiteTypeBin( LEFT, siteType ).scoreBestMatchingSiteNeoplasmBins( getSiteTypeBin( RIGHT, siteType ) );
      final Collection<SiteNeoplasmBin> allLateralitySites = new HashSet<>( leftRightScores.keySet() );
      allLateralitySites.addAll( rightLeftScores.keySet() );
      for ( SiteNeoplasmBin siteBin : allLateralitySites ) {
         final KeyValue<Long, Collection<SiteNeoplasmBin>> leftScore = leftRightScores.get( siteBin );
         final KeyValue<Long, Collection<SiteNeoplasmBin>> rightScore = rightLeftScores.get( siteBin );
         if ( leftScore != null && leftScore.getKey() > 0 ) {
            if ( rightScore == null || leftScore.getKey() >= SWITCH_FACTOR * rightScore.getKey() ) {
               LOGGER.info( "Left Score " + leftScore.getKey() + " for " + siteBin.toString() + " vs. left\n"
                            + leftScore.getValue()
                                       .stream()
                                       .map( SiteNeoplasmBin::toString )
                                       .collect( Collectors.joining("\n  " ) )
                            + "\n> Right Score "
                            + (rightScore == null ? "null"
                                                  : (rightScore.getKey() + "\n  "
                                                     + rightScore.getValue()
                                                                 .stream()
                                                                 .map( SiteNeoplasmBin::toString )
                                                                 .collect( Collectors.joining(
                                                                       "\n  " ) ))) );
               leftScore.getValue()
                        .forEach( r -> r.copyInto( siteBin ) );
               siteBin.invalidate();
            }
         }
         if ( rightScore != null && rightScore.getKey() > 0 ) {
            if ( leftScore == null || rightScore.getKey() >= SWITCH_FACTOR * leftScore.getKey() ) {
               LOGGER.info( "Right Score " + rightScore.getKey() + " for " + siteBin.toString() + " vs. right\n"
                            + rightScore.getValue()
                                        .stream()
                                        .map( SiteNeoplasmBin::toString )
                                        .collect( Collectors.joining("\n  " ) )
                            + "\n> Left Score "
                            + (leftScore == null ? "null"
                                                 : (leftScore.getKey() + "\n  "
                                                    + leftScore.getValue()
                                                               .stream()
                                                               .map( SiteNeoplasmBin::toString )
                                                               .collect( Collectors.joining(
                                                                     "\n  " ) ))) );
               rightScore.getValue()
                         .forEach( r -> r.copyInto( siteBin ) );
               siteBin.invalidate();
            }
         }
      }
      getSiteTypeBin( LEFT, siteType ).clean();
      getSiteTypeBin( RIGHT, siteType ).clean();
   }



   public Collection<SiteNeoplasmBin> getSiteNeoplasmBins() {
      return getOrCreateLateralityTypeBins().values()
                                      .stream()
                                      .map( LateralityTypeBin::getSiteNeoplasmBins )
                                      .flatMap( Collection::stream )
                                      .collect( Collectors.toSet() );
   }



}
