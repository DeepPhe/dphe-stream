//package org.healthnlp.deepphe.summary.attribute.laterality;
//
//import org.apache.log4j.Logger;
//import org.healthnlp.deepphe.core.uri.UriUtil;
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
//import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//import org.healthnlp.deepphe.util.KeyValue;
//import org.healthnlp.deepphe.util.UriScoreUtil;
//
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.*;
//import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
//import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.*;
//import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addBooleanFeatures;
//import static org.healthnlp.deepphe.summary.attribute.util.UriMapUtil.*;
//import static org.healthnlp.deepphe.summary.attribute.util.UriMapUtil.mapUriBranchMentionCounts;
//
//public class Laterality implements SpecificAttribute {
//
//   static private final Logger LOGGER = Logger.getLogger( "Laterality" );
//
//   private String _bestLateralityCode = "";
//   final private NeoplasmAttribute _neoplasmAttribute;
//
//   public Laterality( final ConceptAggregate neoplasm,
//                       final Collection<ConceptAggregate> allConcepts,
//                      final Collection<ConceptAggregate> patientNeoplasms ) {
//      _neoplasmAttribute = createLateralityAttribute( neoplasm, allConcepts, patientNeoplasms );
//   }
//
//   public NeoplasmAttribute toNeoplasmAttribute() {
//      return _neoplasmAttribute;
//   }
//
//   public String getBestLateralityCode() {
//      return _bestLateralityCode;
//   }
//
//   static private class FullAttributeStore {
//      final private Collection<String> _mainCodes;
//      final private Collection<String> _allCodes;
//      final private String _bestCode;
//      final private Collection<ConceptAggregate> _concepts;
//      final private Collection<Mention> _mainMentions;
//      final private Collection<Mention> _allMentions;
//      final private Collection<String> _mainUris;
//      final private Collection<String> _allUris;
//      final private Map<String, Integer> _uriMentionCounts;
//      final private Map<String, Integer> _allClassLevelMap;
//      final private Map<String,Collection<String>> _uriRootsMap;
//
//      private FullAttributeStore( final ConceptAggregate neoplasm ) {
//         this( Collections.singletonList( neoplasm ) );
//      }
//      private FullAttributeStore( final Collection<ConceptAggregate> neoplasms ) {
//         _concepts = neoplasms.stream()
//                              .map( Laterality::getLateralityConcepts )
//                              .flatMap( Collection::stream )
//                              .collect( Collectors.toSet() );
//         _mainUris = getMainUris( _concepts );
//         _allUris = getAllUris( _concepts );
//         _mainMentions = getMainMentions( _concepts, _mainUris );
//         _allMentions = getAllMentions( _concepts );
//         _mainCodes = _mainUris.stream()
//                              .map( Laterality::getLateralityCode )
//                              .filter( c -> !c.isEmpty() )
//                              .collect( Collectors.toSet() );
//         _allCodes = _allUris.stream()
//                              .map( Laterality::getLateralityCode )
//                              .filter( c -> !c.isEmpty() )
//                              .collect( Collectors.toSet() );
//         _bestCode = _mainUris.isEmpty() ? getBestLateralityCode( _allUris ) : getBestLateralityCode( _mainUris );
//         _uriRootsMap = UriUtil.mapUriRoots( _allUris );
//         _uriMentionCounts = UriScoreUtil.mapUriMentionCounts( _allMentions );
//         _allClassLevelMap = UriScoreUtil.createUriClassLevelMap( _allUris );
//      }
//   }
//
//
//
//
//   private NeoplasmAttribute createLateralityAttribute( final ConceptAggregate neoplasm,
//                                                        final Collection<ConceptAggregate> allConcepts,
//                                                        final Collection<ConceptAggregate> patientNeoplasms ) {
//      final FullAttributeStore neoplasmFullStore = new FullAttributeStore( neoplasm );
//
//      final FullAttributeStore patientFullStore = new FullAttributeStore( patientNeoplasms );
//
//      final List<Integer> features = createFeatures( neoplasm,
//                                                     neoplasmFullStore,
//                                                     patientFullStore );
//
//      final Map<EvidenceLevel, Collection<Mention>> evidence
//            = SpecificAttribute.mapEvidence( neoplasm, patientNeoplasms );
//
//      return SpecificAttribute.createAttribute( "laterality",
//                                                getBestLateralityCode(),
//                                                evidence,
//                                                features );
//   }
//
//
//
//   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
//                                         final FullAttributeStore neoplasmFullStore,
//                                         final FullAttributeStore patientFullStore ) {
//      final List<Integer> features = new ArrayList<>();
//
//      final String _bestUri = neoplasm.getUri();
//
//      //1.  !!!!!  Best URI  !!!!!
//      //    ======  CONCEPT  =====
//
//      final Collection<ConceptAggregate> bestInNeoplasmMainConcepts = getUriIsMain( _bestUri,
//                                                                                    neoplasmFullStore._concepts );
//      final Collection<ConceptAggregate> bestInNeoplasmAllConcepts = getUriInAny( _bestUri,
//                                                                                  neoplasmFullStore._concepts );
//      final Collection<ConceptAggregate> bestInPatientMainConcepts = getUriIsMain( _bestUri,
//                                                                                   patientFullStore._concepts );
//      final Collection<ConceptAggregate> bestInPatientAllConcepts = getUriInAny( _bestUri,
//                                                                                 patientFullStore._concepts );
//      addCollectionFeatures( features,
//                             neoplasmFullStore._concepts,
//                             patientFullStore._concepts );
//      addStandardFeatures( features, bestInNeoplasmMainConcepts,
//                           neoplasmFullStore._concepts,
//                           patientFullStore._concepts );
//      addStandardFeatures( features, bestInNeoplasmAllConcepts,
//                           neoplasmFullStore._concepts,
//                           patientFullStore._concepts );
//      addStandardFeatures( features, bestInPatientMainConcepts, patientFullStore._concepts );
//      addStandardFeatures( features, bestInPatientAllConcepts, patientFullStore._concepts );
//
//
//      //    ======  MENTION  =====
//
//      final Collection<Mention> bestInNeoplasmMentions
//            = neoplasmFullStore._allMentions.stream()
//                                            .filter( m -> m.getClassUri()
//                                                        .equals( _bestUri ) )
//                                            .collect( Collectors.toSet() );
//      final Collection<Mention> bestInPatientMentions
//            = patientFullStore._allMentions.stream()
//                                           .filter( m -> m.getClassUri()
//                                                       .equals( _bestUri ) )
//                                           .collect( Collectors.toSet() );
//      addCollectionFeatures( features,
//                             neoplasmFullStore._allMentions,
//                             patientFullStore._allMentions );
//      addStandardFeatures( features,
//                           bestInFirstMentions,
//                           neoplasmFullStore.,
//                           neoplasmFullStore._allMentions,
//                           patientFullStore._allMentions );
//      addStandardFeatures( features,
//                           bestInNeoplasmMentions,
//                           neoplasmFullStore._allMentions,
//                           patientFullStore._allMentions );
//      addStandardFeatures( features, bestInPatientMentions, patientFullStore._allMentions );
//
//
//      //    ======  URI  =====
//      addCollectionFeatures( features,
//                             neoplasmFullStore._mainUris,
//                             patientFullStore._mainUris );
//      addCollectionFeatures( features,
//                             neoplasmFullStore._allUris,
//                             patientFullStore._allUris );
//
//      addStandardFeatures( features, neoplasmFullStore._mainUris, neoplasmFullStore._allMentions );
//      addStandardFeatures( features, patientFullStore._mainUris, patientFullStore._allMentions );
//
//      addStandardFeatures( features, neoplasmFullStore._allUris, neoplasmFullStore._allMentions );
//      addStandardFeatures( features, patientFullStore._allUris, patientFullStore._allMentions );
//
//
//      //2.  !!!!!  URI Branch  !!!!!
//      //    ======  URI  =====
//      final Map<String, Collection<String>> patientAllUriRootsMap = patientFullStore._uriRootsMap;
//
//      final Map<String, Collection<String>> neoplasmAllUriRootsMap = neoplasmFullStore._uriRootsMap;
//
//      final Map<String, List<Mention>> neoplasmUriMentions = mapUriMentions( neoplasmFullStore._allMentions );
//      final Map<String, List<Mention>> patientSiteUriMentions = mapUriMentions( patientFullStore._allMentions );
//
////      final Map<String, Integer> highMentionBranchCounts = mapAllUriBranchMentionCounts( neoplasmHighUriMentions,
////                                                                                          neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> neoplasmMentionBranchCounts = mapAllUriBranchMentionCounts( neoplasmUriMentions,
//                                                                                             neoplasmAllUriRootsMap );
//      final Map<String, Integer> patientMentionBranchCounts = mapAllUriBranchMentionCounts( patientSiteUriMentions,
//                                                                                            patientAllUriRootsMap );
//      addCollectionFeatures( features,
////                             highMentionBranchCounts.keySet(),
//                             neoplasmMentionBranchCounts.keySet(),
//                             patientMentionBranchCounts.keySet() );
//
//
//      //    ======  CONCEPT  =====
//      final Map<String, Integer> bestNeoplasmMainConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInNeoplasmMainConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> bestPatientMainConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInPatientMainConcepts, neoplasmAllUriRootsMap );
//
//      final Map<String, Integer> bestNeoplasmAllConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInNeoplasmAllConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> bestPatientAllConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInPatientAllConcepts, neoplasmAllUriRootsMap );
//
//      final Map<String, Integer> neoplasmConceptBranchCounts
//            = mapUriBranchConceptCounts( neoplasmFullStore._concepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> patientConceptBranchCounts
//            = mapUriBranchConceptCounts( patientFullStore._concepts, neoplasmAllUriRootsMap );
//
//      final int bestNeoplasmMainConceptBranchCount = getBranchCountsSum( bestNeoplasmMainConceptBranchCounts );
//      final int bestPatientMainConceptBranchCount = getBranchCountsSum( bestPatientMainConceptBranchCounts );
//      final int bestNeoplasmAllConceptBranchCount = getBranchCountsSum( bestNeoplasmAllConceptBranchCounts );
//      final int bestPatientAllConceptBranchCount = getBranchCountsSum( bestPatientAllConceptBranchCounts );
//      final int neoplasmConceptBranchCount = getBranchCountsSum( neoplasmConceptBranchCounts );
//      final int patientConceptBranchCount = getBranchCountsSum( patientConceptBranchCounts );
//
//      addIntFeatures( features, neoplasmConceptBranchCount, patientConceptBranchCount );
//
//      addStandardFeatures( features,
//                           bestNeoplasmMainConceptBranchCount,
//                           neoplasmConceptBranchCount,
//                           patientConceptBranchCount );
//      addStandardFeatures( features,
//                           bestNeoplasmAllConceptBranchCount,
//                           neoplasmConceptBranchCount,
//                           patientConceptBranchCount );
//      addStandardFeatures( features, bestPatientMainConceptBranchCount, patientConceptBranchCount );
//      addStandardFeatures( features, bestPatientAllConceptBranchCount, patientConceptBranchCount );
//
//
//      //    ======  MENTION  =====
//      final Map<String, Integer> bestNeoplasmMainMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInNeoplasmMainConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> bestPatientMainMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInPatientMainConcepts, patientAllUriRootsMap );
//
//      final Map<String, Integer> bestNeoplasmAllMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInNeoplasmAllConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> bestPatientAllMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInPatientAllConcepts, patientAllUriRootsMap );
//
//      final int bestNeoplasmMainMentionBranchCount = getBranchCountsSum( bestNeoplasmMainMentionBranchCounts );
//      final int bestPatientMainMentionBranchCount = getBranchCountsSum( bestPatientMainMentionBranchCounts );
//      final int bestNeoplasmAllMentionBranchCount = getBranchCountsSum( bestNeoplasmAllMentionBranchCounts );
//      final int bestPatientAllMentionBranchCount = getBranchCountsSum( bestPatientAllMentionBranchCounts );
//      final int neoplasmMentionBranchCount = getBranchCountsSum( neoplasmMentionBranchCounts );
//      final int patientMentionBranchCount = getBranchCountsSum( patientMentionBranchCounts );
//
//      addIntFeatures( features, neoplasmMentionBranchCount, patientMentionBranchCount );
//
//      addStandardFeatures( features,
//                           bestNeoplasmMainMentionBranchCount,
//                           neoplasmMentionBranchCount,
//                           patientMentionBranchCount );
//      addStandardFeatures( features,
//                           bestNeoplasmAllMentionBranchCount,
//                           neoplasmMentionBranchCount,
//                           patientMentionBranchCount );
//      addStandardFeatures( features,
//                           bestPatientMainMentionBranchCount,
//                           patientMentionBranchCount );
//      addStandardFeatures( features,
//                           bestPatientAllMentionBranchCount,
//                           patientMentionBranchCount );
//
//
//      //3.  !!!!!  URI Depth  !!!!!
//      //    ======  URI  =====
//      final int neoplasmMainMaxDepth = neoplasmFullStore._mainUris.stream()
//                                                                  .mapToInt(
//                                                                        u -> neoplasmFullStore._allClassLevelMap.getOrDefault(
//                                                                              u, 0 ) )
//                                                                  .max()
//                                                                  .orElse( 0 );
//      final int neoplasmAllMaxDepth = neoplasmFullStore._allUris.stream()
//                                                                .mapToInt(
//                                                                      u -> neoplasmFullStore._allClassLevelMap.getOrDefault(
//                                                                            u, 0 ) )
//                                                                .max()
//                                                                .orElse( 0 );
//      final int patientMainMaxDepth = patientFullStore._mainUris.stream()
//                                                                .mapToInt(
//                                                                      u -> patientFullStore._allClassLevelMap.getOrDefault(
//                                                                            u, 0 ) )
//                                                                .max()
//                                                                .orElse( 0 );
//      final int patientAllMaxDepth = patientFullStore._allClassLevelMap.values()
//                                                                       .stream()
//                                                                       .mapToInt( i -> i )
//                                                                       .max()
//                                                                       .orElse( 0 );
//
//      addIntFeatures( features,
//                      neoplasmHighStore._classLevel * 2,
//                      neoplasmMainMaxDepth * 2,
//                      neoplasmAllMaxDepth * 2,
//                      patientMainMaxDepth * 2,
//                      patientAllMaxDepth * 2 );
//
//      addStandardFeatures( features,
//                           neoplasmHighStore._classLevel,
//                           neoplasmMainMaxDepth,
//                           neoplasmAllMaxDepth,
//                           patientMainMaxDepth,
//                           patientAllMaxDepth );
//
//
//      //5.  !!!!!  Runner-Up  !!!!!
//      //    ======  CONCEPT  =====
//      List<KeyValue<String, Double>> bestUriScores = Collections.emptyList();
//      final Map<String, Integer> neoplasmFullAllUriSums
//            = UriScoreUtil.mapUriSums( neoplasmFullStore._allUris,
//                                       neoplasmFullStore._uriRootsMap,
//                                       neoplasmFullStore._uriMentionCounts );
//      final Map<String, Double> neoplasmFullAllUriQuotientMap
//            = UriScoreUtil.mapUriQuotientsBB( neoplasmFullAllUriSums,
//                                              neoplasmFullStore._allUris.size() );  // All Uris?
//      final List<KeyValue<String, Double>> neoplasmFullAllUriQuotientList
//            = UriScoreUtil.listUriQuotients( neoplasmFullAllUriQuotientMap );  // All Uris?
//      bestUriScores
//            = UriScoreUtil.getBestUriScores( neoplasmFullAllUriQuotientList,
//                                             neoplasmFullStore._allClassLevelMap,
//                                             neoplasmFullStore._uriRootsMap );
//      final String bestUri = bestUriScores.get( bestUriScores.size() - 1 )
//                                          .getKey();
//
//      final double bestUriScore = bestUriScores.isEmpty()
//                                  ? 0
//                                  : bestUriScores.get( bestUriScores.size() - 1 )
//                                                 .getValue();
//
//      addDoubleDivisionFeature( features, 1, bestUriScore );
//      final boolean haveRunnerUp = bestUriScores.size() > 1;
//      double runnerUpScore = haveRunnerUp
//                             ? bestUriScores.get( bestUriScores.size() - 2 )
//                                            .getValue()
//                             : 0;
//      addDoubleDivisionFeature( features, 1, runnerUpScore );
//      addDoubleDivisionFeature( features, runnerUpScore, bestUriScore );
//
//      final String runnerUp = haveRunnerUp
//                              ? bestUriScores.get( bestUriScores.size() - 2 )
//                                             .getKey()
//                              : "";
//      final Collection<ConceptAggregate> runnerUpPatientMainConcepts
//            = haveRunnerUp
//              ? getUriIsMain( runnerUp, patientFullStore._concepts )
//              : Collections.emptyList();
//
//      final Collection<ConceptAggregate> runnerUpPatientAllConcepts
//            = haveRunnerUp
//              ? getUriInAny( runnerUp, patientFullStore._concepts )
//              : Collections.emptyList();
//
//      addStandardFeatures( features, runnerUpPatientMainConcepts, patientFullStore._concepts );
//      addStandardFeatures( features, runnerUpPatientAllConcepts, patientFullStore._concepts );
//
//      final Map<String, Integer> runnerUpPatientMainConceptBranchCounts
//            = mapUriBranchConceptCounts( runnerUpPatientMainConcepts, patientAllUriRootsMap );
//
//      final Map<String, Integer> runnerUpPatientAllConceptBranchCounts
//            = mapUriBranchConceptCounts( runnerUpPatientAllConcepts, patientAllUriRootsMap );
//
//      final int runnerUpPatientMainConceptBranchCount = getBranchCountsSum( runnerUpPatientMainConceptBranchCounts );
//
//      final int runnerUpPatientAllConceptBranchCount = getBranchCountsSum( runnerUpPatientAllConceptBranchCounts );
//
//      addIntFeatures( features,
//                      neoplasmConceptBranchCount,
//                      patientConceptBranchCount,
//                      bestNeoplasmMainConceptBranchCount,
//                      bestPatientMainConceptBranchCount );
//      addStandardFeatures( features, runnerUpPatientMainConceptBranchCount,
//                           patientConceptBranchCount,
//                           bestPatientMainConceptBranchCount );
//      addStandardFeatures( features, runnerUpPatientAllConceptBranchCount,
//                           patientConceptBranchCount,
//                           bestPatientMainConceptBranchCount );
//
//
//      //    ======  MENTION  =====
//      final Collection<Mention> runnerUpFirstMentions
//            = neoplasmHighStore._mentions.stream()
//                                         .filter( m -> m.getClassUri()
//                                                        .equals( runnerUp ) )
//                                         .collect( Collectors.toSet() );
//      final Collection<Mention> runnerUpNeoplasmMentions
//            = neoplasmFullStore._allMentions.stream()
//                                            .filter( m -> m.getClassUri()
//                                                        .equals( runnerUp ) )
//                                            .collect( Collectors.toSet() );
//      final Collection<Mention> runnerUpPatientMentions
//            = patientFullStore._allMentions.stream()
//                                           .filter( m -> m.getClassUri()
//                                                       .equals( runnerUp ) )
//                                           .collect( Collectors.toSet() );
//
//      addStandardFeatures( features,
//                           runnerUpFirstMentions,
//                           neoplasmHighStore._mentions,
//                           neoplasmFullStore._allMentions,
//                           patientFullStore._allMentions,
//                           bestInFirstMentions,
//                           bestInNeoplasmMentions,
//                           bestInPatientMentions );
//      addStandardFeatures( features,
//                           runnerUpNeoplasmMentions,
//                           neoplasmFullStore._allMentions,
//                           patientFullStore._allMentions,
//                           bestInNeoplasmMentions,
//                           bestInPatientMentions );
//      addStandardFeatures( features,
//                           runnerUpPatientMentions,
//                           patientFullStore._allMentions,
//                           bestInPatientMentions );
//
//      final Map<String, Integer> runnerUpPatientMainMentionBranchCounts
//            = mapUriBranchMentionCounts( runnerUpPatientMainConcepts, patientAllUriRootsMap );
//
//      final Map<String, Integer> runnerUpPatientAllMentionBranchCounts
//            = mapUriBranchMentionCounts( runnerUpPatientAllConcepts, patientAllUriRootsMap );
//
//      final int runnerUpPatientMainMentionBranchCount = getBranchCountsSum( runnerUpPatientMainMentionBranchCounts );
//      final int runnerUpPatientAllMentionBranchCount = getBranchCountsSum( runnerUpPatientAllMentionBranchCounts );
//
//      addStandardFeatures( features, runnerUpPatientMainMentionBranchCount, patientMentionBranchCount );
//      addStandardFeatures( features, runnerUpPatientAllMentionBranchCount, patientMentionBranchCount );
//
//
//      //    ======  URI  =====
//      final Collection<String> runnerUpAllUris
//            = haveRunnerUp
//              ? patientFullStore._concepts.stream()
//                                          .map( ConceptAggregate::getAllUris )
//                                          .filter( s -> s.contains( runnerUp ) )
//                                          .flatMap( Collection::stream )
//                                          .collect( Collectors.toSet() )
//              : Collections.emptyList();
//      addStandardFeatures( features,
//                           runnerUpAllUris,
//                           patientFullStore._allUris );
//
//      //3.  !!!!!  URI Depth  !!!!!
//      //    ======  URI  =====
//      final int runnerUpDepth = haveRunnerUp
//                                ? patientFullStore._allClassLevelMap.getOrDefault( runnerUp, 0 )
//                                : 0;
//      final int runnerUpMaxDepth
//            = runnerUpAllUris.stream()
//                             .mapToInt( u -> patientFullStore._allClassLevelMap.getOrDefault( u, 0 ) )
//                             .max()
//                             .orElse( 0 );
//
//      addIntFeatures( features, runnerUpDepth * 2, runnerUpMaxDepth * 2 );
//      addStandardFeatures( features, runnerUpDepth, neoplasmMainMaxDepth,
//                           neoplasmAllMaxDepth, patientMainMaxDepth, patientAllMaxDepth );
//      addStandardFeatures( features, runnerUpMaxDepth, neoplasmMainMaxDepth,
//                           neoplasmAllMaxDepth, patientMainMaxDepth, patientAllMaxDepth );
//
//
//      // Have Code
//      addBooleanFeatures( features, !neoplasmHighStore._highestCode.isEmpty() );
//      addBooleanFeatures( features, !neoplasmHighStore._highestCode.isEmpty()
//                                    && neoplasmHighStore._highestCode.equals( neoplasmFullStore._highestCode ) );
//      addBooleanFeatures( features, !neoplasmHighStore._highestCode.isEmpty()
//                                    && neoplasmHighStore._highestCode.equals( patientFullStore._highestCode ) );
//      addCollectionFeatures( features,
//                             neoplasmHighStore._morphCodes,
//                             neoplasmFullStore._morphCodes,
//                             patientFullStore._morphCodes );
//
//      _bestHistoCode = getBestHistology( _morphologyCodes );
//      addBooleanFeatures( features,
//                          _bestHistoCode.isEmpty(),
//                          _bestHistoCode.equals( getBestHistology( neoplasmHighStore._morphCodes ) ),
//                          _bestHistoCode.equals( getBestHistology( neoplasmFullStore._morphCodes ) ),
//                          _bestHistoCode.equals( getBestHistology( patientFullStore._morphCodes ) ) );
//
//      addBooleanFeatures( features, neoplasm.isNegated(), neoplasm.isUncertain(), neoplasm.isGeneric(),
//                          neoplasm.isConditional() );
//
//      LOGGER.info( "Features: " + features.size() );
//      return features;
//   }
//
//
//
//
//   static private Collection<ConceptAggregate> getLateralityConcepts( final ConceptAggregate concept ) {
//      return concept.getRelated( HAS_LATERALITY );
//   }
//
//   static private String getLateralityCode( final String uri ) {
//      switch ( uri ) {
//         case "Right" : return "1";
//         case "Left" : return "2";
//         case "Bilateral" : return "4";
//      }
//      return "";
//   }
//
//   static private String getBestLateralityCode( final Collection<String> uris ) {
//      if ( uris.isEmpty() ) {
//         return "";
//      }
//      return uris.stream()
//                .collect( Collectors.groupingBy( Function.identity(), Collectors.counting() ) )
//                .entrySet()
//                .stream()
//                .max( Comparator.comparingLong( Map.Entry::getValue ) )
//                .map( Map.Entry::getKey )
//                .map( Laterality::getLateralityCode )
//                 .orElse( "" );
//   }
//
//
//}
