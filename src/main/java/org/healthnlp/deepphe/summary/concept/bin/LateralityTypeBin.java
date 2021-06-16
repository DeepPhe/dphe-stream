package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.util.KeyValue;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.concept.bin.SiteType.ALL_SITES;
import static org.healthnlp.deepphe.summary.concept.bin.SiteType.NO_SITE;

/**
 * @author SPF , chip-nlp
 * @since {5/24/2021}
 */
final public class LateralityTypeBin {

   static private final Logger LOGGER = Logger.getLogger( "LateralityTypeBin" );

   static public final String NO_LATERALITY_URI = "NO_LATERALITY_URI";

   private final LateralityType _lateralityType;

   private final Map<SiteType,SiteTypeBin> _siteTypeBins = new EnumMap<>( SiteType.class );

   public LateralityTypeBin( final LateralityType lateralityType ) {
      _lateralityType = lateralityType;
   }

   static Map<LateralityType,Collection<Mention>> getLateralityTypes(
         final Collection<Mention> neoplasms,
         final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      final Map<LateralityType,Collection<Mention>> lateralityTypes = new EnumMap<>( LateralityType.class );
      for ( Mention neoplasm : neoplasms ) {
         final Map<String, Collection<Mention>> relations = relationsMap.get( neoplasm );
         LateralityType.getLateralityTypes( relations )
                 .forEach( l -> lateralityTypes.computeIfAbsent( l, b -> new HashSet<>() )
                                            .add( neoplasm ) );
      }
      return lateralityTypes;
   }

   void clear() {
      getOrCreateSiteTypeBins().values()
                               .forEach( SiteTypeBin::clear );
   }

   void setNeoplasms( final Collection<Mention> cancers,
                      final Collection<Mention> tumors,
                      final Map<Mention,Map<String,Collection<Mention>>> relationsMap ) {
      clear();
      final Map<SiteType,Collection<Mention>> siteTypeCancersMap
            = SiteTypeBin.getSiteTypes( cancers, relationsMap );
      final Map<SiteType,Collection<Mention>> siteTypeTumorsMap
            = SiteTypeBin.getSiteTypes( tumors, relationsMap );
      for ( SiteType siteType : SiteType.values() ) {
         _siteTypeBins.get( siteType )
                      .setNeoplasms( siteTypeCancersMap.getOrDefault( siteType, new HashSet<>() ),
                                     siteTypeTumorsMap.getOrDefault( siteType, new HashSet<>() ),
                                     relationsMap );

      }
   }

   LateralityType getLateralityType() {
      return _lateralityType;
   }

   SiteTypeBin getSiteTypeBin( final SiteType siteType ) {
      return getOrCreateSiteTypeBins().get( siteType );
   }

   private Map<SiteType,SiteTypeBin> getOrCreateSiteTypeBins() {
      if ( _siteTypeBins.isEmpty() ) {
         _siteTypeBins.put( NO_SITE, new SiteTypeBin( NO_SITE ) );
         _siteTypeBins.put( ALL_SITES, new SiteTypeBin( ALL_SITES ) );
      }
      return _siteTypeBins;
   }

   void distributeSites( final Map<String, Collection<String>> allUriRoots ) {
      LOGGER.info( "!!! Laterality Type Bin " + _lateralityType.name() + " Distributing Sites !!!" );
      distributeNoSites( NeoplasmType.CANCER, allUriRoots );
      distributeNoSites( NeoplasmType.TUMOR, allUriRoots );
      _siteTypeBins.get( NO_SITE ).clean();
   }


   void distributeNoSites( final NeoplasmType neoplasmType, final Map<String, Collection<String>> allUriRoots ) {
      LOGGER.info( "!!! Laterality Type Bin " + _lateralityType.name() + " Distributing No Sites !!!" );
      final SiteTypeBin noSiteBin = _siteTypeBins.get( NO_SITE );
      final Collection<NeoplasmChain> notLocated = new HashSet<>();
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> organScores
            = noSiteBin.scoreBestMatchingNeoplasmChains( _siteTypeBins.get( ALL_SITES ) );
      final Map<NeoplasmChain,Collection<SiteNeoplasmBin>> neoplasmChainBinsMap = new HashMap<>();
      for ( SiteNeoplasmBin siteNeoplasmBin : _siteTypeBins.get( ALL_SITES ).getSiteNeoplasmBins() ) {
         siteNeoplasmBin.getNeoplasmChains( neoplasmType )
                        .forEach( c -> neoplasmChainBinsMap.computeIfAbsent( c, s -> new HashSet<>() )
                                                           .add( siteNeoplasmBin ) );
      }
      final Collection<NeoplasmChain> noSiteChains = noSiteBin.getSiteNeoplasmBins()
                                                              .stream()
                                                              .map( b -> b.getNeoplasmChains( neoplasmType ) )
                                                              .flatMap( Collection::stream )
                                                              .collect( Collectors.toSet() );
      for ( NeoplasmChain noSiteChain : noSiteChains ) {
         final KeyValue<Long,Collection<NeoplasmChain>> organScore = organScores.get( noSiteChain );
         boolean located = false;
         if ( organScore != null && organScore.getKey() > 0 ) {
               LOGGER.info( "Organ Score " + organScore.getKey() + " for " + noSiteChain.toString() + " vs. organ\n"
                            + organScore.getValue()
                                        .stream()
                                        .map( NeoplasmChain::toString )
                                        .collect( Collectors.joining("\n  " ) ) );
            organScore.getValue()
                      .stream()
                      .map( neoplasmChainBinsMap::get )
                      .flatMap( Collection::stream )
                      .forEach( b -> b.copyNeoplasmChain( neoplasmType, noSiteChain ) );
               noSiteChain.invalidate();
               located = true;
         }
         if ( !located ) {
            notLocated.add( noSiteChain );
         }
      }
      distributeNoSitesByRoots( neoplasmType, notLocated, neoplasmChainBinsMap, allUriRoots );
   }


   void distributeNoSitesByRoots( final NeoplasmType neoplasmType,
                                  final Collection<NeoplasmChain> notLocated,
                                  final Map<NeoplasmChain,Collection<SiteNeoplasmBin>> neoplasmChainBinsMap,
                                  final Map<String, Collection<String>> allUriRoots ) {
      LOGGER.info( "!!! Laterality Type Bin " + _lateralityType.name() + " Distributing No Sites By Roots !!!" );
      final SiteTypeBin noSiteBin = _siteTypeBins.get( NO_SITE );
      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> organScores
            = noSiteBin.scoreBestMatchingNeoplasmChains( neoplasmType, _siteTypeBins.get( ALL_SITES ), allUriRoots );
      for ( NeoplasmChain noSiteChain : notLocated ) {
         final KeyValue<Long,Collection<NeoplasmChain>> organScore = organScores.get( noSiteChain );
         if ( organScore != null && organScore.getKey() > 0 ) {
               LOGGER.info( "Organ Score " + organScore.getKey()  + " for " + noSiteChain.toString() + " vs. organ\n"
                            + organScore.getValue()
                                       .stream()
                                       .map( NeoplasmChain::toString )
                                       .collect( Collectors.joining("\n  " ) ) );
            organScore.getValue()
                      .stream()
                      .map( neoplasmChainBinsMap::get )
                      .flatMap( Collection::stream )
                      .forEach( b -> b.copyNeoplasmChain( neoplasmType, noSiteChain ) );
               noSiteChain.invalidate();
         }
      }
   }





//   static private final int REGION_FACTOR = 3;
//
//
//   void distributeRegions( final Map<String, Collection<String>> allUriRoots ) {
//      LOGGER.info( "!!! Laterality Type Bin " + _lateralityType.name() + " Distributing Regions !!!" );
//      final SiteTypeBin regionBin = _siteTypeBins.get( REGION );
//      final Collection<NeoplasmChain> notLocated = new HashSet<>();
//      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> organScores
//            = regionBin.scoreBestMatchingNeoplasmChains( _siteTypeBins.get( ORGAN ) );
//      final Collection<NeoplasmChain> regionChains = regionBin.getSiteNeoplasmBins()
//                                                              .stream()
//                                                              .map( SiteNeoplasmBin::getNeoplasmChains )
//                                                              .flatMap( Collection::stream )
//                                                              .collect( Collectors.toSet() );
//      for ( NeoplasmChain regionChain : regionChains ) {
//         final KeyValue<Long,Collection<NeoplasmChain>> organScore = organScores.get( regionChain );
//         boolean located = false;
//         if ( organScore != null && organScore.getKey() > REGION_FACTOR ) {
//            LOGGER.info( "Organ Score " + organScore.getKey() + " "
//                         + organScore.getValue()
//                                    .stream()
//                                    .map( NeoplasmChain::toString )
//                                    .collect( Collectors.joining("\n  " ) )
//                                              + "\nFor\n" + regionChain.toString() );
//               organScore.getValue().forEach( r -> r.copyInto( regionChain ) );
//               regionChain.invalidate();
//               located = true;
//         }
//         if ( !located ) {
//            notLocated.add( regionChain );
//         }
//      }
//      distributeRegionsByRoots( notLocated, allUriRoots );
//   }
//
//
//   void distributeRegionsByRoots( final Collection<NeoplasmChain> notLocated,
//                                  final Map<String, Collection<String>> allUriRoots ) {
//      LOGGER.info( "!!! Laterality Type Bin " + _lateralityType.name() + " Distributing Regions by Roots !!!" );
//      final SiteTypeBin regionBin = _siteTypeBins.get( REGION );
//      final Map<NeoplasmChain, KeyValue<Long,Collection<NeoplasmChain>>> organScores
//            = regionBin.scoreBestMatchingNeoplasmChains( _siteTypeBins.get( ORGAN ), allUriRoots );
//      for ( NeoplasmChain regionChain : notLocated ) {
//         final KeyValue<Long,Collection<NeoplasmChain>> organScore = organScores.get( regionChain );
//         if ( organScore != null && organScore.getKey() > REGION_FACTOR ) {
//            LOGGER.info( "Organ Score " + organScore.getKey() + " "
//                         + organScore.getValue()
//                                     .stream()
//                                     .map( NeoplasmChain::toString )
//                                     .collect( Collectors.joining("\n  " ) )
//                         + "\nFor\n" + regionChain.toString() );
//            organScore.getValue()
//                         .forEach( r -> r.copyInto( regionChain ) );
//            regionChain.invalidate();
//         }
//      }
//   }






   public Collection<SiteNeoplasmBin> getSiteNeoplasmBins() {
      return getOrCreateSiteTypeBins().values()
                                     .stream()
                                     .map( SiteTypeBin::getSiteNeoplasmBins )
                                     .flatMap( Collection::stream )
                                     .collect( Collectors.toSet() );
   }


}
