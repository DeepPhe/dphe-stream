package org.healthnlp.deepphe.summary.attribute.grade;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;

import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addBooleanFeatures;


final public class Grade implements SpecificAttribute {

   static private final Logger LOGGER = Logger.getLogger( "Grade" );

//   private int _bestGradeCode = -1;
   private String _bestGradeCode = "";
   private String _bestGrade = "";
   final private NeoplasmAttribute _grade;

   public Grade( final ConceptAggregate neoplasm,
                 final Collection<ConceptAggregate> allConcepts,
                 final Collection<ConceptAggregate> patientNeoplasms ) {
      _grade = createGradeAttribute( neoplasm, allConcepts, patientNeoplasms );
   }

   private NeoplasmAttribute createGradeAttribute( final ConceptAggregate neoplasm,
                                                        final Collection<ConceptAggregate> allConcepts,
                                                        final Collection<ConceptAggregate> patientNeoplasms ) {
      final GradeUriInfoVisitor uriInfoVisitor = new GradeUriInfoVisitor();
      final GradeInfoStore patientStore = new GradeInfoStore( patientNeoplasms, uriInfoVisitor );

      final GradeInfoStore neoplasmStore = new GradeInfoStore( neoplasm, uriInfoVisitor );

      final GradeInfoStore allConceptsStore = new GradeInfoStore( allConcepts, uriInfoVisitor );

      patientStore._codeInfoStore.init( patientStore._mainUriStore );
      neoplasmStore._codeInfoStore.init( neoplasmStore._mainUriStore );
      allConceptsStore._codeInfoStore.init( allConceptsStore._mainUriStore );

      _bestGrade = neoplasmStore._mainUriStore._bestUri;
      _bestGradeCode = neoplasmStore._codeInfoStore._bestGradeCode;

      final List<Integer> features = createFeatures( neoplasm,
                                                     allConcepts,
                                                     neoplasmStore,
                                                     patientStore,
                                                     allConceptsStore );

      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( Collections.singletonList( neoplasm ),
                                             patientStore._concepts,
                                             allConceptsStore._concepts );

      return SpecificAttribute.createAttribute( "grade",
                                                neoplasmStore._codeInfoStore._bestGradeCode,
                                                evidence,
                                                features );
   }

   public String getBestGrade() {
      return _bestGrade;
   }

   public String getBestGradeCode() {
      return _bestGradeCode;
//      < 0 ? "9" : _bestGradeCode + "";
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _grade;
   }


   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                         final Collection<ConceptAggregate> allConcepts,
                                         final AttributeInfoStore neoplasmStore,
                                         final AttributeInfoStore patientStore,
                                         final AttributeInfoStore allConceptStore ) {
      final List<Integer> features = new ArrayList<>();

      neoplasmStore.addGeneralFeatures( features );
      patientStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, patientStore );

      allConceptStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, allConceptStore );

      final boolean noGrade = neoplasmStore._mainUriStore._bestUri.isEmpty();
      addBooleanFeatures( features,
                          noGrade,
                          noGrade && !patientStore._mainUriStore._bestUri.isEmpty(),
                          noGrade && !allConceptStore._mainUriStore._bestUri.isEmpty(),
                          neoplasmStore._mainUriStore._bestUri.equals( patientStore._mainUriStore._bestUri ),
                          neoplasmStore._mainUriStore._bestUri.equals( allConceptStore._mainUriStore._bestUri ) );

      addBooleanFeatures( features,
                          neoplasm.isNegated(),
                          neoplasm.isUncertain(),
                          neoplasm.isGeneric(),
                          neoplasm.isConditional() );
      return features;
   }


//   private NeoplasmAttribute createGradeAttribute( final ConceptAggregate neoplasm,
//                                                    final Collection<ConceptAggregate> allConcepts ) {
//      final Map<Integer,List<ConceptAggregate>> neoplasmGradeConceptMap = getGradeConcepts( neoplasm );
//      final HighGradeStore neoplasmHighStore = new HighGradeStore( neoplasmGradeConceptMap );
//      final FullGradeStore neoplasmFullStore = new FullGradeStore( neoplasmGradeConceptMap );
//
//
//      final Map<Integer,Collection<ConceptAggregate>> patientGradeConceptMap = new HashMap<>();
//      for ( ConceptAggregate concept : allConcepts ) {
//         final Map<Integer,List<ConceptAggregate>> gradeConceptMap = getGradeConcepts( concept );
//         for ( Map.Entry<Integer,List<ConceptAggregate>> gradeConcepts : gradeConceptMap.entrySet() ) {
//            patientGradeConceptMap.computeIfAbsent( gradeConcepts.getKey(),
//                                                    g -> new HashSet<>() )
//                                  .addAll( gradeConcepts.getValue() );
//         }
//      }
////      final HighGradeStore patientHighStore = new HighGradeStore( patientGradeConceptMap );
//      final FullGradeStore patientFullStore = new FullGradeStore( patientGradeConceptMap );
//
//      final List<Integer> features = createFeatures( neoplasm,
//                                                     allConcepts,
//                                                     neoplasmHighStore,
//                                                     neoplasmFullStore,
//                                                     patientFullStore );
//
//      final Map<EvidenceLevel, Collection<Mention>> evidence
//            = SpecificAttribute.mapEvidence( neoplasmHighStore._concepts,
//                                             neoplasmFullStore._concepts,
//                                             patientFullStore._concepts );
//
//      return SpecificAttribute.createAttribute( "grade",
//                                                getBestGradeCode(),
//                                                   evidence,
//                                                   features );
//   }
//
//
//
//   static private class FullGradeStore {
//      final private List<Integer> _gradeCodes;
//      final private Collection<ConceptAggregate> _concepts;
//      final private Collection<Mention> _mentions;
//      final private Collection<String> _mainUris;
//      final private Collection<String> _allUris;
//      final private Map<String, Integer> _uriMentionCounts;
//      final private Map<String, Integer> _allClassLevelMap;
//      private FullGradeStore( final Map<Integer,? extends Collection<ConceptAggregate>> gradeConceptMap ) {
//         _gradeCodes = new ArrayList<>( gradeConceptMap.keySet() );
//         Collections.sort( _gradeCodes );
//         _concepts
//               = gradeConceptMap.values()
//                                .stream()
//                                .flatMap( Collection::stream )
//                                .collect( Collectors.toSet() );
//         _mentions = getMentions( _concepts );
//         _mainUris = getMainUris( _concepts );
//         _allUris = getAllUris( _concepts );
//         _uriMentionCounts = UriScoreUtil.mapUriMentionCounts( _mentions );
//         _allClassLevelMap = UriScoreUtil.createUriClassLevelMap( _allUris );
//      }
//   }
//
//   static private class HighGradeStore {
//      final private List<Integer> _gradeCodes;
//      final private int _highestCode;
//      final private Collection<ConceptAggregate> _concepts;
//      final private Collection<Mention> _mentions;
//      final private Collection<String> _mainUris;
//      final private Collection<String> _allUris;
//      final private Map<String, Integer> _uriMentionCounts;
//      final private Map<String, Integer> _allClassLevelMap;
//      private HighGradeStore( final Map<Integer,? extends Collection<ConceptAggregate>> gradeConceptMap ) {
//         _gradeCodes = new ArrayList<>( gradeConceptMap.keySet() );
//         Collections.sort( _gradeCodes );
//         _highestCode = _gradeCodes.isEmpty()
//                          ? -1
//                           : _gradeCodes.get( _gradeCodes.size()-1 );
//         _concepts = _gradeCodes.isEmpty()
//                               ? Collections.emptyList()
//                              : gradeConceptMap.get( _highestCode );
//         _mentions = getMentions( _concepts );
//         _mainUris = getMainUris( _concepts );
//         _allUris = getAllUris( _concepts );
//         _uriMentionCounts = UriScoreUtil.mapUriMentionCounts( _mentions );
//         _allClassLevelMap = UriScoreUtil.createUriClassLevelMap( _allUris );
//      }
//   }
//
//
//
//   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
//                                         final Collection<ConceptAggregate> allConcepts,
//                                         final HighGradeStore neoplasmHighStore,
//                                         final FullGradeStore neoplasmFullStore,
//                                         final FullGradeStore patientFullStore ) {
//      final List<Integer> features = new ArrayList<>( );
//
//
//      final Map<String,Collection<String>> patientAllRootsMap
//            = UriUtil.mapUriRoots( patientFullStore._allUris );
//
//      final Map<String,Collection<String>> neoplasmHighAllRootsMap = new HashMap<>();
//      neoplasmHighStore._allUris
//            .forEach( u ->  neoplasmHighAllRootsMap.put( u,
//                                                      patientAllRootsMap.getOrDefault( u,
//                                                                                       Collections.emptyList() ) ) );
//
//      List<KeyValue<String, Double>> bestUriScores = Collections.emptyList();
//      if ( !neoplasmHighStore._mainUris.isEmpty() ) {
//         final Map<String, Integer> neoplasmHighAllUriSums
//               = UriScoreUtil.mapUriSums( neoplasmHighStore._allUris,
//                                          neoplasmHighAllRootsMap,
//                                          neoplasmHighStore._uriMentionCounts );
//         final Map<String, Double> neoplasmHighAllUriQuotientMap
//               = UriScoreUtil.mapUriQuotientsBB( neoplasmHighAllUriSums,
//                                                 neoplasmHighStore._allUris.size() );  // All Uris?
//         final List<KeyValue<String, Double>> neoplasmHighAllUriQuotientList
//               = UriScoreUtil.listUriQuotients( neoplasmHighAllUriQuotientMap );  // All Uris?
//         bestUriScores
//               = UriScoreUtil.getBestUriScores( neoplasmHighAllUriQuotientList,
//                                                neoplasmHighStore._allClassLevelMap,
//                                                neoplasmHighAllRootsMap );
//         _bestGrade = bestUriScores.get( bestUriScores.size() - 1 )
//                                   .getKey();
//      } else {
//         _bestGrade = "";   // TODO  Use whole patient?
//      }
//      _bestGradeCode = neoplasmHighStore._highestCode;
//
//
//
//      //1.  !!!!!  Best URI  !!!!!
//      //    ======  CONCEPT  =====
//
//      final Collection<ConceptAggregate> bestInFirstMainConcepts = getIfUriIsMain( _bestGrade,
//                                                                                   neoplasmHighStore._concepts );
//      final Collection<ConceptAggregate> bestInFirstAllConcepts = getIfUriIsAny( _bestGrade,
//                                                                                 neoplasmHighStore._concepts );
//      final Collection<ConceptAggregate> bestInNeoplasmMainConcepts = getIfUriIsMain( _bestGrade,
//                                                                                      neoplasmFullStore._concepts );
//      final Collection<ConceptAggregate> bestInNeoplasmAllConcepts = getIfUriIsAny( _bestGrade,
//                                                                                    neoplasmFullStore._concepts );
//      final Collection<ConceptAggregate> bestInPatientMainConcepts = getIfUriIsMain( _bestGrade,
//                                                                                     patientFullStore._concepts );
//      final Collection<ConceptAggregate> bestInPatientAllConcepts = getIfUriIsAny( _bestGrade,
//                                                                                   patientFullStore._concepts );
//      addCollectionFeatures( features,
//                             neoplasmHighStore._concepts,
//                             neoplasmFullStore._concepts,
//                             patientFullStore._concepts );
//      addStandardFeatures( features,
//                           bestInFirstMainConcepts,
//                           neoplasmHighStore._concepts,
//                           neoplasmFullStore._concepts,
//                           patientFullStore._concepts );
//      addStandardFeatures( features,
//                           bestInFirstAllConcepts,
//                           neoplasmHighStore._concepts,
//                           neoplasmFullStore._concepts,
//                           patientFullStore._concepts );
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
//      final Collection<Mention> bestInFirstMentions
//            = neoplasmHighStore._mentions.stream()
//                               .filter( m -> m.getClassUri()
//                                              .equals( _bestGrade ) )
//                               .collect( Collectors.toSet() );
//      final Collection<Mention> bestInNeoplasmMentions
//            = neoplasmFullStore._mentions.stream()
//                                  .filter( m -> m.getClassUri()
//                                                 .equals( _bestGrade ) )
//                                  .collect( Collectors.toSet() );
//      final Collection<Mention> bestInPatientMentions
//            = patientFullStore._mentions.stream()
//                                 .filter( m -> m.getClassUri()
//                                                .equals( _bestGrade ) )
//                                 .collect( Collectors.toSet() );
//      addCollectionFeatures( features,
//                             neoplasmHighStore._mentions,
//                             neoplasmFullStore._mentions,
//                             patientFullStore._mentions );
//      addStandardFeatures( features,
//                           bestInFirstMentions,
//                           neoplasmHighStore._mentions,
//                           neoplasmFullStore._mentions,
//                           patientFullStore._mentions );
//      addStandardFeatures( features,
//                           bestInNeoplasmMentions,
//                           neoplasmFullStore._mentions,
//                           patientFullStore._mentions );
//      addStandardFeatures( features, bestInPatientMentions, patientFullStore._mentions );
//
//
//      //    ======  URI  =====
//      final Collection<String> bestAllUris = neoplasmHighStore._concepts.stream()
//                                                              .map( ConceptAggregate::getAllUris )
//                                                              .filter( s -> s.contains( _bestGrade ) )
//                                                              .flatMap( Collection::stream )
//                                                              .collect( Collectors.toSet() );
//      addCollectionFeatures( features,
//                             neoplasmHighStore._mainUris,
//                             neoplasmFullStore._mainUris,
//                             patientFullStore._mainUris );
//      addCollectionFeatures( features,
//                             neoplasmHighStore._allUris,
//                             neoplasmFullStore._allUris,
//                             patientFullStore._allUris );
//      addStandardFeatures( features,
//                           bestAllUris,
//                           neoplasmHighStore._allUris,
//                           neoplasmFullStore._allUris,
//                           patientFullStore._allUris );
//
//      addStandardFeatures( features, neoplasmHighStore._mainUris, neoplasmHighStore._mentions );
//      addStandardFeatures( features, neoplasmFullStore._mainUris, neoplasmFullStore._mentions );
//      addStandardFeatures( features, patientFullStore._mainUris, patientFullStore._mentions );
//
//      addStandardFeatures( features, neoplasmHighStore._allUris, neoplasmHighStore._mentions );
//      addStandardFeatures( features, neoplasmFullStore._allUris, neoplasmFullStore._mentions );
//      addStandardFeatures( features, patientFullStore._allUris, patientFullStore._mentions );
//
//
//      //2.  !!!!!  URI Branch  !!!!!
//      //    ======  URI  =====
//      final Map<String, Collection<String>> patientAllUriRootsMap
//            = UriUtil.mapUriRoots( patientFullStore._allUris );
//
//      final Map<String, Collection<String>> neoplasmAllUriRootsMap
//            = new HashMap<>( patientAllUriRootsMap );
//      neoplasmAllUriRootsMap.keySet()
//                                .retainAll( neoplasmFullStore._allUris );
//
//      final Map<String, Collection<String>> neoplasmHighAllUriRootsMap
//            = new HashMap<>( neoplasmAllUriRootsMap );
//      neoplasmHighAllUriRootsMap.keySet()
//                             .retainAll( neoplasmHighStore._allUris );
//
//      final Map<String, List<Mention>> neoplasmHighUriMentions = mapUriMentions( neoplasmHighStore._mentions );
//      final Map<String, List<Mention>> neoplasmUriMentions = mapUriMentions( neoplasmFullStore._mentions );
//      final Map<String, List<Mention>> patientSiteUriMentions = mapUriMentions( patientFullStore._mentions );
//
//      final Map<String, Integer> highMentionBranchCounts = mapAllUriBranchMentionCounts( neoplasmHighUriMentions,
//                                                                                          neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> neoplasmMentionBranchCounts = mapAllUriBranchMentionCounts( neoplasmUriMentions,
//                                                                                             neoplasmAllUriRootsMap );
//      final Map<String, Integer> patientMentionBranchCounts = mapAllUriBranchMentionCounts( patientSiteUriMentions,
//                                                                                            patientAllUriRootsMap );
//      addCollectionFeatures( features,
//                             highMentionBranchCounts.keySet(),
//                             neoplasmMentionBranchCounts.keySet(),
//                             patientMentionBranchCounts.keySet() );
//
//
//      //    ======  CONCEPT  =====
//      final Map<String, Integer> bestFirstMainConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInFirstMainConcepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> bestNeoplasmMainConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInNeoplasmMainConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> bestPatientMainConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInPatientMainConcepts, neoplasmAllUriRootsMap );
//
//      final Map<String, Integer> bestFirstAllConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInFirstAllConcepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> bestNeoplasmAllConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInNeoplasmAllConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> bestPatientAllConceptBranchCounts
//            = mapUriBranchConceptCounts( bestInPatientAllConcepts, neoplasmAllUriRootsMap );
//
//      final Map<String, Integer> firstConceptBranchCounts
//            = mapUriBranchConceptCounts( neoplasmHighStore._concepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> neoplasmConceptBranchCounts
//            = mapUriBranchConceptCounts( neoplasmFullStore._concepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> patientConceptBranchCounts
//            = mapUriBranchConceptCounts( patientFullStore._concepts, neoplasmAllUriRootsMap );
//
//      final int bestFirstMainConceptBranchCount = getBranchCountsSum( bestFirstMainConceptBranchCounts );
//      final int bestNeoplasmMainConceptBranchCount = getBranchCountsSum( bestNeoplasmMainConceptBranchCounts );
//      final int bestPatientMainConceptBranchCount = getBranchCountsSum( bestPatientMainConceptBranchCounts );
//      final int bestFirstAllConceptBranchCount = getBranchCountsSum( bestFirstAllConceptBranchCounts );
//      final int bestNeoplasmAllConceptBranchCount = getBranchCountsSum( bestNeoplasmAllConceptBranchCounts );
//      final int bestPatientAllConceptBranchCount = getBranchCountsSum( bestPatientAllConceptBranchCounts );
//      final int firstConceptBranchCount = getBranchCountsSum( firstConceptBranchCounts );
//      final int neoplasmConceptBranchCount = getBranchCountsSum( neoplasmConceptBranchCounts );
//      final int patientConceptBranchCount = getBranchCountsSum( patientConceptBranchCounts );
//
//      addStandardFeatures( features,
//                           bestFirstMainConceptBranchCount,
//                           firstConceptBranchCount,
//                           neoplasmConceptBranchCount,
//                           patientConceptBranchCount );
//      addStandardFeatures( features,
//                           bestFirstAllConceptBranchCount,
//                           firstConceptBranchCount,
//                           neoplasmConceptBranchCount,
//                           patientConceptBranchCount );
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
//
//
//
//      //    ======  MENTION  =====
//      final Map<String, Integer> bestFirstMainMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInFirstMainConcepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> bestNeoplasmMainMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInNeoplasmMainConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> bestPatientMainMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInPatientMainConcepts, patientAllUriRootsMap );
//
//      final Map<String, Integer> bestFirstAllMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInFirstAllConcepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> bestNeoplasmAllMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInNeoplasmAllConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> bestPatientAllMentionBranchCounts
//            = mapUriBranchMentionCounts( bestInPatientAllConcepts, patientAllUriRootsMap );
//
//      final int bestFirstMainMentionBranchCount = getBranchCountsSum( bestFirstMainMentionBranchCounts );
//      final int bestNeoplasmMainMentionBranchCount = getBranchCountsSum( bestNeoplasmMainMentionBranchCounts );
//      final int bestPatientMainMentionBranchCount = getBranchCountsSum( bestPatientMainMentionBranchCounts );
//      final int bestFirstAllMentionBranchCount = getBranchCountsSum( bestFirstAllMentionBranchCounts );
//      final int bestNeoplasmAllMentionBranchCount = getBranchCountsSum( bestNeoplasmAllMentionBranchCounts );
//      final int bestPatientAllMentionBranchCount = getBranchCountsSum( bestPatientAllMentionBranchCounts );
//      final int firstMentionBranchCount = getBranchCountsSum( highMentionBranchCounts );
//      final int neoplasmMentionBranchCount = getBranchCountsSum( neoplasmMentionBranchCounts );
//      final int patientMentionBranchCount = getBranchCountsSum( patientMentionBranchCounts );
//
//      addLargeIntFeatures( features, firstMentionBranchCount, neoplasmMentionBranchCount, patientMentionBranchCount );
//
//      addStandardFeatures( features,
//                           bestFirstMainMentionBranchCount,
//                           firstMentionBranchCount,
//                           neoplasmMentionBranchCount,
//                           patientMentionBranchCount );
//      addStandardFeatures( features,
//                           bestFirstAllMentionBranchCount,
//                           firstMentionBranchCount,
//                           neoplasmMentionBranchCount,
//                           patientMentionBranchCount );
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
//      final int bestDepth = neoplasmHighStore._allClassLevelMap.getOrDefault( _bestGrade, 0 );
//
//      final int bestMaxDepth = bestAllUris.stream()
//                                          .mapToInt( u -> neoplasmHighStore._allClassLevelMap.getOrDefault( u, 0 ) )
//                                          .max()
//                                          .orElse( 0 );
//      final int firstMainMaxDepth = neoplasmHighStore._mainUris.stream()
//                                                     .mapToInt( u -> neoplasmHighStore._allClassLevelMap.getOrDefault( u, 0 ) )
//                                                     .max()
//                                                     .orElse( 0 );
//      final int firstAllMaxDepth = neoplasmHighStore._allUris.stream()
//                                                   .mapToInt( u -> neoplasmHighStore._allClassLevelMap.getOrDefault( u, 0 ) )
//                                                   .max()
//                                                   .orElse( 0 );
//      final int neoplasmMainMaxDepth = neoplasmFullStore._mainUris.stream()
//                                                           .mapToInt( u -> neoplasmFullStore._allClassLevelMap.getOrDefault( u, 0 ) )
//                                                           .max()
//                                                           .orElse( 0 );
//      final int neoplasmAllMaxDepth = neoplasmFullStore._allUris.stream()
//                                                         .mapToInt( u -> neoplasmFullStore._allClassLevelMap.getOrDefault( u, 0 ) )
//                                                         .max()
//                                                         .orElse( 0 );
//      final int patientMainMaxDepth = patientFullStore._mainUris.stream()
//                                                         .mapToInt( u -> patientFullStore._allClassLevelMap.getOrDefault( u, 0 ) )
//                                                         .max()
//                                                         .orElse( 0 );
//      final int patientAllMaxDepth = patientFullStore._allClassLevelMap.values()
//                                                  .stream()
//                                                  .mapToInt( i -> i )
//                                                  .max()
//                                                  .orElse( 0 );
//
//      addLargeIntFeatures( features,
//                      bestDepth * 2,
//                      bestMaxDepth * 2,
//                      firstMainMaxDepth * 2,
//                      firstAllMaxDepth * 2,
//                      neoplasmMainMaxDepth * 2,
//                      neoplasmAllMaxDepth * 2,
//                      patientMainMaxDepth * 2,
//                      patientAllMaxDepth * 2 );
//
//      addStandardFeatures( features,
//                           bestDepth,
//                           firstMainMaxDepth,
//                           firstAllMaxDepth,
//                           neoplasmMainMaxDepth,
//                           neoplasmAllMaxDepth,
//                           patientMainMaxDepth,
//                           patientAllMaxDepth );
//
//      addStandardFeatures( features,
//                           bestMaxDepth,
//                           firstMainMaxDepth,
//                           firstAllMaxDepth,
//                           neoplasmMainMaxDepth,
//                           neoplasmAllMaxDepth,
//                           patientMainMaxDepth,
//                           patientAllMaxDepth );
//
//
//
//      //4.  !!!!!  Relation Count  !!!!!
//      final Collection<Collection<ConceptAggregate>> relations = getGradeRelations( neoplasm,
//                                                                                    neoplasmFullStore._concepts );
//      final Predicate<ConceptAggregate> hasBestUri = c -> c.getAllUris()
//                                                           .stream()
//                                                           .anyMatch( bestAllUris::contains );
//      final Predicate<Collection<ConceptAggregate>> setHasBestUri = c -> c.stream()
//                                                                          .anyMatch( hasBestUri );
//      final int bestRelationCount = (int) relations.stream()
//                                                   .filter( setHasBestUri )
//                                                   .count();
//      final int allSiteRelationCount = relations.size();
//      final int patientRelationCount = (int) allConcepts
//            .stream()
//            .map( c -> getGradeRelations( c, patientFullStore._concepts) )
//            .count();
//
//      addLargeIntFeatures( features, allSiteRelationCount, patientRelationCount );
//      addStandardFeatures( features, bestRelationCount, allSiteRelationCount, patientRelationCount );
//
//
//      //5.  !!!!!  Runner-Up  !!!!!
//      //    ======  CONCEPT  =====
//      final double bestUriScore = bestUriScores.isEmpty()
//                                  ? 0
//                                  : bestUriScores.get( bestUriScores.size() - 1 )
//                                                 .getValue();
//      addDoubleDivisionFeature( features, 1, bestUriScore );
//      final boolean haveRunnerUp = bestUriScores.size() > 1;
//      double runnerUpScore = haveRunnerUp
//                             ? bestUriScores.get( bestUriScores.size() - 2 )
//                                            .getValue()
//                             : 0;
//      addDoubleDivisionFeature( features, 1, runnerUpScore );
//      addDoubleDivisionFeature( features, runnerUpScore, bestUriScore );
//
//      final String runnerUp = haveRunnerUp ? bestUriScores.get( bestUriScores.size()-2 )
//                                                          .getKey() : "";
//      final Collection<ConceptAggregate> runnerUpInFirstMainConcepts
//            = haveRunnerUp ? getIfUriIsMain( runnerUp, neoplasmHighStore._concepts ) : Collections.emptyList();
//      final Collection<ConceptAggregate> runnerUpNeoplasmMainConcepts
//            = haveRunnerUp ? getIfUriIsMain( runnerUp, neoplasmFullStore._concepts ) : Collections.emptyList();
//      final Collection<ConceptAggregate> runnerUpPatientMainConcepts
//            = haveRunnerUp ? getIfUriIsMain( runnerUp, patientFullStore._concepts ) : Collections.emptyList();
//
//      final Collection<ConceptAggregate> runnerUpInFirstAllConcepts
//            = haveRunnerUp ? getIfUriIsAny( runnerUp, neoplasmHighStore._concepts ) : Collections.emptyList();
//      final Collection<ConceptAggregate> runnerUpNeoplasmAllConcepts
//            = haveRunnerUp ? getIfUriIsAny( runnerUp, neoplasmFullStore._concepts ) : Collections.emptyList();
//      final Collection<ConceptAggregate> runnerUpPatientAllConcepts
//            = haveRunnerUp ? getIfUriIsAny( runnerUp, patientFullStore._concepts ) : Collections.emptyList();
//
//      addStandardFeatures( features, runnerUpInFirstMainConcepts,
//                           neoplasmHighStore._concepts,
//                           neoplasmFullStore._concepts,
//                           patientFullStore._concepts );
//      addStandardFeatures( features, runnerUpInFirstAllConcepts,
//                           neoplasmHighStore._concepts,
//                           neoplasmFullStore._concepts,
//                           patientFullStore._concepts );
//      addStandardFeatures( features, runnerUpNeoplasmMainConcepts,
//                           neoplasmFullStore._concepts,
//                           patientFullStore._concepts );
//      addStandardFeatures( features, runnerUpNeoplasmAllConcepts,
//                           neoplasmFullStore._concepts,
//                           patientFullStore._concepts );
//      addStandardFeatures( features, runnerUpPatientMainConcepts, patientFullStore._concepts );
//      addStandardFeatures( features, runnerUpPatientAllConcepts, patientFullStore._concepts );
//
//      final Map<String, Integer> runnerUpFirstMainConceptBranchCounts
//            = mapUriBranchConceptCounts( runnerUpInFirstMainConcepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> runnerUpNeoplasmMainConceptBranchCounts
//            = mapUriBranchConceptCounts( runnerUpNeoplasmMainConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> runnerUpPatientMainConceptBranchCounts
//            = mapUriBranchConceptCounts( runnerUpPatientMainConcepts, patientAllUriRootsMap );
//
//      final Map<String, Integer> runnerUpFirstAllConceptBranchCounts
//            = mapUriBranchConceptCounts( runnerUpInFirstAllConcepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> runnerUpNeoplasmAllConceptBranchCounts
//            = mapUriBranchConceptCounts( runnerUpNeoplasmAllConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> runnerUpPatientAllConceptBranchCounts
//            = mapUriBranchConceptCounts( runnerUpPatientAllConcepts, patientAllUriRootsMap );
//
//      final int runnerUpFirstMainConceptBranchCount = getBranchCountsSum( runnerUpFirstMainConceptBranchCounts );
//      final int runnerUpNeoplasmMainConceptBranchCount = getBranchCountsSum( runnerUpNeoplasmMainConceptBranchCounts );
//      final int runnerUpPatientMainConceptBranchCount = getBranchCountsSum( runnerUpPatientMainConceptBranchCounts );
//
//      final int runnerUpFirstAllConceptBranchCount = getBranchCountsSum( runnerUpFirstAllConceptBranchCounts );
//      final int runnerUpNeoplasmAllConceptBranchCount = getBranchCountsSum( runnerUpNeoplasmAllConceptBranchCounts );
//      final int runnerUpPatientAllConceptBranchCount = getBranchCountsSum( runnerUpPatientAllConceptBranchCounts );
//
//      addStandardFeatures( features, runnerUpFirstMainConceptBranchCount,
//                           firstConceptBranchCount,
//                           neoplasmConceptBranchCount,
//                           patientConceptBranchCount,
//                           bestFirstMainConceptBranchCount,
//                           bestNeoplasmMainConceptBranchCount,
//                           bestPatientMainConceptBranchCount );
//      addStandardFeatures( features, runnerUpFirstAllConceptBranchCount,
//                           firstConceptBranchCount,
//                           neoplasmConceptBranchCount,
//                           patientConceptBranchCount,
//                           bestFirstMainConceptBranchCount,
//                           bestNeoplasmMainConceptBranchCount,
//                           bestPatientMainConceptBranchCount );
//      addStandardFeatures( features, runnerUpNeoplasmMainConceptBranchCount,
//                           neoplasmConceptBranchCount,
//                           patientConceptBranchCount,
//                           bestNeoplasmMainConceptBranchCount,
//                           bestPatientMainConceptBranchCount );
//      addStandardFeatures( features, runnerUpNeoplasmAllConceptBranchCount,
//                           neoplasmConceptBranchCount,
//                           patientConceptBranchCount,
//                           bestNeoplasmMainConceptBranchCount,
//                           bestPatientMainConceptBranchCount );
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
//                               .filter( m -> m.getClassUri()
//                                              .equals( runnerUp ) )
//                               .collect( Collectors.toSet() );
//      final Collection<Mention> runnerUpNeoplasmMentions
//            = neoplasmFullStore._mentions.stream()
//                                  .filter( m -> m.getClassUri()
//                                                 .equals( runnerUp ) )
//                                  .collect( Collectors.toSet() );
//      final Collection<Mention> runnerUpPatientMentions
//            = patientFullStore._mentions.stream()
//                                 .filter( m -> m.getClassUri()
//                                                .equals( runnerUp ) )
//                                 .collect( Collectors.toSet() );
//
//      addStandardFeatures( features,
//                           runnerUpFirstMentions,
//                           neoplasmHighStore._mentions,
//                           neoplasmFullStore._mentions,
//                           patientFullStore._mentions,
//                           bestInFirstMentions,
//                           bestInNeoplasmMentions,
//                           bestInPatientMentions );
//      addStandardFeatures( features,
//                           runnerUpNeoplasmMentions,
//                           neoplasmFullStore._mentions,
//                           patientFullStore._mentions,
//                           bestInNeoplasmMentions,
//                           bestInPatientMentions );
//      addStandardFeatures( features,
//                           runnerUpPatientMentions,
//                           patientFullStore._mentions,
//                           bestInPatientMentions );
//
//      final Map<String, Integer> runnerUpFirstMainMentionBranchCounts
//            = mapUriBranchMentionCounts( runnerUpInFirstMainConcepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> runnerUpNeoplasmMainMentionBranchCounts
//            = mapUriBranchMentionCounts( runnerUpNeoplasmMainConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> runnerUpPatientMainMentionBranchCounts
//            = mapUriBranchMentionCounts( runnerUpPatientMainConcepts, patientAllUriRootsMap );
//
//      final Map<String, Integer> runnerUpFirstAllMentionBranchCounts
//            = mapUriBranchMentionCounts( runnerUpInFirstAllConcepts, neoplasmHighAllUriRootsMap );
//      final Map<String, Integer> runnerUpNeoplasmAllMentionBranchCounts
//            = mapUriBranchMentionCounts( runnerUpNeoplasmAllConcepts, neoplasmAllUriRootsMap );
//      final Map<String, Integer> runnerUpPatientAllMentionBranchCounts
//            = mapUriBranchMentionCounts( runnerUpPatientAllConcepts, patientAllUriRootsMap );
//
//      final int runnerUpFirstMainMentionBranchCount = getBranchCountsSum( runnerUpFirstMainMentionBranchCounts );
//      final int runnerUpNeoplasmMainMentionBranchCount = getBranchCountsSum( runnerUpNeoplasmMainMentionBranchCounts );
//      final int runnerUpPatientMainMentionBranchCount = getBranchCountsSum( runnerUpPatientMainMentionBranchCounts );
//      final int runnerUpFirstAllMentionBranchCount = getBranchCountsSum( runnerUpFirstAllMentionBranchCounts );
//      final int runnerUpNeoplasmAllMentionBranchCount = getBranchCountsSum( runnerUpNeoplasmAllMentionBranchCounts );
//      final int runnerUpPatientAllMentionBranchCount = getBranchCountsSum( runnerUpPatientAllMentionBranchCounts );
//
//      addStandardFeatures( features, runnerUpFirstMainMentionBranchCount,
//                           firstMentionBranchCount, neoplasmMentionBranchCount, patientMentionBranchCount );
//      addStandardFeatures( features, runnerUpFirstAllMentionBranchCount,
//                           firstMentionBranchCount, neoplasmMentionBranchCount, patientMentionBranchCount );
//      addStandardFeatures( features, runnerUpNeoplasmMainMentionBranchCount,
//                           neoplasmMentionBranchCount, patientMentionBranchCount );
//      addStandardFeatures( features, runnerUpNeoplasmAllMentionBranchCount,
//                           neoplasmMentionBranchCount, patientMentionBranchCount );
//      addStandardFeatures( features, runnerUpPatientMainMentionBranchCount, patientMentionBranchCount );
//      addStandardFeatures( features, runnerUpPatientAllMentionBranchCount, patientMentionBranchCount );
//
//
//
//
//      //    ======  URI  =====
//      final Collection<String> runnerUpAllUris
//            = haveRunnerUp ? neoplasmHighStore._concepts.stream()
//                                              .map( ConceptAggregate::getAllUris )
//                                              .filter( s -> s.contains( runnerUp ) )
//                                              .flatMap( Collection::stream )
//                                              .collect( Collectors.toSet() )
//                           : Collections.emptyList();
//      addStandardFeatures( features,
//                           runnerUpAllUris,
//                           neoplasmHighStore._allUris,
//                           neoplasmFullStore._allUris,
//                           patientFullStore._allUris );
//
//      //3.  !!!!!  URI Depth  !!!!!
//      //    ======  URI  =====
//      final int runnerUpDepth = haveRunnerUp
//                                ? neoplasmHighStore._allClassLevelMap.getOrDefault( runnerUp, 0 )
//                                : 0;
//      final int runnerUpMaxDepth
//            = runnerUpAllUris.stream()
//                             .mapToInt( u ->  neoplasmHighStore._allClassLevelMap.getOrDefault( u, 0 ) )
//                             .max()
//                             .orElse( 0 );
//
//      addLargeIntFeatures( features, runnerUpDepth * 2, runnerUpMaxDepth * 2 );
//      addStandardFeatures( features, runnerUpDepth, firstMainMaxDepth, firstAllMaxDepth, neoplasmMainMaxDepth,
//                           neoplasmAllMaxDepth, patientMainMaxDepth, patientAllMaxDepth );
//      addStandardFeatures( features, runnerUpMaxDepth, firstMainMaxDepth, firstAllMaxDepth, neoplasmMainMaxDepth,
//                           neoplasmAllMaxDepth, patientMainMaxDepth, patientAllMaxDepth );
//
//
//
//
//
//
//      // Have Code
//      addBooleanFeatures( features, _bestGradeCode >= 0 );
//      addCollectionFeatures( features, neoplasmFullStore._gradeCodes, patientFullStore._gradeCodes );
//
//      addBooleanFeatures( features, neoplasm.isNegated(), neoplasm.isUncertain(), neoplasm.isGeneric(),
//                          neoplasm.isConditional() );
//      addLargeIntFeatures( features, neoplasm.getMentions().size() );
//
//
//      LOGGER.info( "Features: " + features.size() );
//      return features;
//   }








//   static private Collection<Collection<ConceptAggregate>> getGradeRelations( final ConceptAggregate neoplasm,
//                                                                         final Collection<ConceptAggregate> knownGrades ) {
//      final Collection<String> GRADE_RELATIONS
//            = Arrays.asList( HAS_GLEASON_SCORE, DISEASE_IS_GRADE, DISEASE_HAS_FINDING );
//      final Function<Collection<ConceptAggregate>,Collection<ConceptAggregate>> trimToGrades
//            = c -> { c.retainAll( knownGrades ); return c; };
//      return neoplasm.getRelatedConceptMap()
//                    .entrySet()
//                    .stream()
//                    .filter( e -> GRADE_RELATIONS.contains( e.getKey() ) )
//                    .map( Map.Entry::getValue )
//                    .map( trimToGrades )
//                    .filter( c -> !c.isEmpty() )
//                    .collect( Collectors.toList() );
//   }
//
//
//
//   static private Map<Integer,List<ConceptAggregate>> getGradeConcepts( final ConceptAggregate neoplasm ) {
//      final Collection<ConceptAggregate> gradeConcepts
//            = neoplasm.getRelated( HAS_GLEASON_SCORE, DISEASE_IS_GRADE, DISEASE_HAS_FINDING );
//      final Map<Integer,List<ConceptAggregate>> gradeMap
//            = gradeConcepts.stream()
//                           .collect( Collectors.groupingBy( Grade::getGradeNumber ) );
//      gradeMap.remove( -1 );
//      return gradeMap;
//   }
//
//
//
//   static private int getGradeNumber( final ConceptAggregate grade ) {
//      final String uri = grade.getUri();
//      if ( uri.startsWith( "Gleason_Score_" ) ) {
//         if ( uri.endsWith( "6" ) ) {
//            return 1;
//         } else if ( uri.endsWith( "7" ) ) {
//            return 2;
//         } else if ( uri.endsWith( "8" )
//                     || uri.endsWith( "9" )
//                     || uri.endsWith( "10" ) ) {
//            return 3;
//         } else {
//            return -1;
//         }
//      } else if ( uri.equals( "Grade_1" )
//                  || uri.equals( "Low_Grade" )
//                  || uri.equals( "Low_Grade_Malignant_Neoplasm" )
//                  || uri.equals( "Well_Differentiated" ) ) {
//         return 1;
//      } else if ( uri.equals( "Grade_2" )
//                  || uri.equals( "Intermediate_Grade" )
//                  || uri.equals( "Intermediate_Grade_Malignant_Neoplasm" )
//                  || uri.equals( "Moderately_Differentiated" ) ) {
//         return 2;
//      } else if ( uri.equals( "Grade_3" )
//                  || uri.equals( "High_Grade" )
//                  || uri.equals( "High_Grade_Malignant_Neoplasm" )
//                  || uri.equals( "Poorly_Differentiated" ) ) {
//         return 3;
//      } else if ( uri.equals( "Grade_4" )
//                  || uri.equals( "Undifferentiated" ) ) {
//         // todo add "anaplastic"
////            LOGGER.info( "Have an Undifferentiated, adding its Grade Equivalent (4) to possible ICDO Grades." );
//         return 4;
//      } else if ( uri.equals( "Grade_5" ) ) {
//         return 5;
//      }
//      return -1;
//   }
//
//
//
//
//   static private Map<String,Integer> mapGradeUriCounts( final Map<Integer,List<ConceptAggregate>> gradeCodeMap ) {
//      final List<Integer> gradeCodes = gradeCodeMap.keySet()
//                                                   .stream()
//                                                   .sorted()
//                                                   .collect( Collectors.toList() );
//      final Map<String,Integer> uriCounts = new HashMap<>();
//      for ( int code : gradeCodes ) {
//         final Map<String,List<ConceptAggregate>> uriConceptMap = gradeCodeMap
//               .get( code )
//               .stream()
//               .collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
//         uriConceptMap.forEach( (k,v) -> uriCounts.put( k, uriCounts.getOrDefault( k, 0 ) + v.size() ) );
//      }
//      return uriCounts;
//   }


}


