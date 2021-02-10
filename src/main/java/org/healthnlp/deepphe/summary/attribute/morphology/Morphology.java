package org.healthnlp.deepphe.summary.attribute.morphology;

import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.TopoMorphValidator;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.*;
import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.*;
import static org.healthnlp.deepphe.summary.attribute.util.UriMapUtil.*;


final public class Morphology implements SpecificAttribute {

   static private final Logger LOGGER = Logger.getLogger( "Morphology" );

//   private String _bestMorphCode;
   private Collection<String> _morphologyCodes;
   private String _bestHistoCode = "";
   private String _bestBehaveCode = "";
   private String _bestUri = "";
   final private NeoplasmAttribute _histology;
   final private NeoplasmAttribute _behavior;

   static public final Map<String,List<String>> CONFUSION = new HashMap<>();
   static public final Map<String,List<String>> REAL_CONFUSION = new HashMap<>();
   static public final Map<String,Integer> SYS_COUNTS = new HashMap<>();
   static public final Map<String,Integer> GOLD_COUNTS = new HashMap<>();
   static private int HIT = 0;
   static private int MISS = 0;

   static private final Map<String,String> GOLD_MAP = parseGold();


   static private Map<String,String> parseGold() {
      final Map<String,String> map = new HashMap<>();
      final String goldPath = "C:\\Spiffy\\data\\dphe_data\\kcr\\CombinedKcrGold.bsv";
      try ( BufferedReader reader = new BufferedReader( new FileReader( goldPath ) ) ){
         String line = reader.readLine();
         while ( line != null ) {
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length > 8 ) {
               map.put( splits[ 0 ], splits[ 7 ] );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
      }
      return map;
   }


   public Morphology( final ConceptAggregate neoplasm,
                      final Collection<ConceptAggregate> allConcepts,
                      final Collection<ConceptAggregate> patientNeoplasms,
                      final Collection<String> validTopoMorphs,
                      final String topographyCode ) {
      _histology = createHistoAttribute( neoplasm, allConcepts, patientNeoplasms, validTopoMorphs, topographyCode );
      _behavior = createBehaviorAttribute();
   }

   private NeoplasmAttribute createBehaviorAttribute() {
      final List<Integer> behaviorFeatures = new ArrayList<>( _histology.getConfidenceFeatures() );
      behaviorFeatures.addAll( createBehaviorFeatures() );
      return SpecificAttribute.createAttribute( "behavior",
                                                getBestBehaveCode(),
                                                _histology.getDirectEvidence(),
                                                _histology.getIndirectEvidence(),
                                                _histology.getNotEvidence(),
                                                behaviorFeatures );
   }




   public String getBestHistoCode() {
      return _bestHistoCode.isEmpty() ? "8000" : _bestHistoCode;
   }

   public String getBestBehaveCode() {
      return _bestBehaveCode.isEmpty() ? "3" : _bestBehaveCode;
   }




   public NeoplasmAttribute toNeoplasmAttribute() {
      return _histology;
   }

   public NeoplasmAttribute getBehaviorAttribute() {
      return _behavior;
   }


   private NeoplasmAttribute createHistoAttribute( final ConceptAggregate neoplasm,
                                                    final Collection<ConceptAggregate> allConcepts,
                                                   final Collection<ConceptAggregate> patientNeoplasms,
                                                   final Collection<String> validTopoMorphs,
                                                   final String topographyCode ) {
      final HighMorphStore neoplasmHighStore = new HighMorphStore( neoplasm, validTopoMorphs );

//      final HighGradeStore patientHighStore = new HighGradeStore( patientNeoplasms, validTopoMorphs );
      final FullMorphStore patientFullStore = new FullMorphStore( patientNeoplasms, validTopoMorphs );

      final FullMorphStore neoplasmFullStore = new FullMorphStore( neoplasm, validTopoMorphs );

      final List<Integer> features = createFeatures( neoplasm,
                                                     allConcepts,
                                                     neoplasmHighStore,
                                                     neoplasmFullStore,
                                                     patientFullStore );

      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( Collections.singletonList( neoplasm ),
                                             neoplasmFullStore._concepts,
                                             patientFullStore._concepts );

      final String bestMorph = neoplasmFullStore._bestMorphCode;
      final String bestHisto = bestMorph.isEmpty() ? "8000" : bestMorph.substring( 0,4 );
      final String goldHisto = GOLD_MAP.getOrDefault( neoplasm.getPatientId(),"??" );
      SYS_COUNTS.put( bestHisto, SYS_COUNTS.getOrDefault( bestHisto, 0 )+1 );
      GOLD_COUNTS.put( goldHisto, GOLD_COUNTS.getOrDefault( goldHisto, 0 )+1 );
      if ( bestHisto.equals( goldHisto ) ) {
         HIT++;
      } else {
         MISS++;
         CONFUSION.computeIfAbsent( bestHisto, c -> new ArrayList<>() ).add( goldHisto );
         final Collection<String> allMorphCodes = getAllMorphCodes( Collections.singletonList( neoplasm ),
                                                                    validTopoMorphs );
         String available = "false";
         if ( allMorphCodes.stream().anyMatch( c -> goldHisto.equals( c.substring( 0,4 ) ) ) ) {
            available = "true";
            REAL_CONFUSION.computeIfAbsent( bestHisto, c -> new ArrayList<>() ).add( goldHisto );
         }
         try ( Writer writer = new FileWriter( "C:/Spiffy/output/morphs.txt", true ) ) {
            writer.write( "\n\n" + neoplasm.getPatientId() + " MORPH : " + bestHisto
                          + " " + topographyCode + " " + neoplasm.getUri()
                          + " " + goldHisto + " " + available + "\n" );
            writer.write( QUOTIENT_TEXT );
            writer.write( URI_ONTO_TEXT );
            writer.write( URI_EXCT_TEXT );
            writer.write( URI_BROD_TEXT );
            writer.write( HIT_COUNT_TEXT );

//            final Collection<ConceptAggregate> singleton = Collections.singletonList( neoplasm );
//            final Collection<String> mainUris = getMainUris( singleton );
//            final Collection<String> nonMainUris = getNonMainUris( singleton, mainUris );
//            writer.write( "\nMain Onto: 4 " + getUrisOntoMorphCodes( mainUris )
//                          + "\nMain Exct: 4 " + getUrisExactMorphCodes( mainUris )
//                          + "\nMain Brod: 3 " + getUrisBroadMorphCodes( mainUris )
////                          + "\nMain Root: 2 " + getMainRootsMorphCodes( singleton, validTopoMorphs )
//                          + "\nAll  Onto: 3 " + getUrisOntoMorphCodes( nonMainUris )
//                          + "\nAll  Exct: 3 " + getUrisExactMorphCodes( nonMainUris )
//                          + "\nAll  Brod: 2 " + getUrisBroadMorphCodes( nonMainUris ) + "\n" );
//                          + "\nAll  Root: 1 " + getAllRootsMorphCodes( singleton, validTopoMorphs ) + "\n" );
//            final Map<Integer,Collection<String>> hitCounts = getHitCounts( singleton, validTopoMorphs );
//            final String counts =
//                  hitCounts.entrySet().stream().map( (e) -> " " + e.getKey() + " " + e.getValue() + " " ).collect(
//                  Collectors.joining( "," ) );
//            writer.write( "Counts " + counts + "\n" );

//            final Map<String,Integer> uriStrengths = getUriStrengths( singleton );
//            final Collection<String> allUris = neoplasm.getAllUris();
//            writer.write( "\nXct Onto: 4 " + getUrisOntoMorphCodeMap( allUris, uriStrengths )
//                          + "\nXct Exct: 4 " + getUrisExactMorphCodeMap( allUris, uriStrengths )
//                          + "\nXct Brod: 3 " + getUrisBroadMorphCodeMap( allUris, uriStrengths ) + "\n" );
            writer.write( neoplasm.toText() + "\n");
         } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

      return SpecificAttribute.createAttribute( "histology",
                                                getBestHistoCode(),
                                                   evidence,
                                                   features );
   }



   static private class FullMorphStore {
      final private List<String> _mainMorphCodes;
      final private List<String> _allMorphCodes;
      final private String _bestMorphCode;
      final private Collection<ConceptAggregate> _concepts;
      final private Collection<Mention> _mentions;
      final private Collection<String> _mainUris;
      final private Collection<String> _allUris;
      final private Map<String, Integer> _uriMentionCounts;
      final private Map<String, Integer> _allClassLevelMap;
      final private Map<String,Collection<String>> _uriRootsMap;

      private FullMorphStore( final ConceptAggregate neoplasm,
                              final Collection<String> validTopoMorphs ) {
         this( Collections.singletonList( neoplasm ), validTopoMorphs );
      }
      private FullMorphStore( final Collection<ConceptAggregate> neoplasms,
                              final Collection<String> validTopoMorphs ) {
         _mainMorphCodes = getMainMorphCodes( neoplasms, validTopoMorphs );
         _allMorphCodes = getAllMorphCodes( neoplasms, validTopoMorphs );
         _bestMorphCode = getBestMorphCode( neoplasms, validTopoMorphs );
         _concepts = neoplasms;
         _mentions = getMentions( _concepts );
         _mainUris = getMainUris( _concepts );
         _allUris = getAllUris( _concepts );
         _uriRootsMap = UriUtil.mapUriRoots( _allUris );
         _uriMentionCounts = UriScoreUtil.mapUriMentionCounts( _mentions );
         _allClassLevelMap = UriScoreUtil.createUriClassLevelMap( _allUris );
      }
   }



   static private class HighMorphStore {
      final private List<String> _morphCodes;
      final private String _bestCode;
      final private Collection<Mention> _mentions;
      final private String _highUri;
      final private int _classLevel;
      final private Collection<String> _uriRoots;
      private HighMorphStore( final ConceptAggregate neoplasm,
                              final Collection<String> validTopoMorphs ) {
         _highUri = neoplasm.getUri();
         _uriRoots = Neo4jOntologyConceptUtil.getRootUris( _highUri );
         _morphCodes = getHighMorphCodes( _highUri, _uriRoots, validTopoMorphs );
         _bestCode = "";//getBestMorphCode( Collections.singletonList( neoplasm ), validTopoMorphs );
         _mentions = neoplasm.getMentions()
                             .stream()
                             .filter( m -> _highUri.equals( m.getClassUri() ) )
                             .collect( Collectors.toList() );
         _classLevel = Neo4jOntologyConceptUtil.getClassLevel( _highUri );
      }
   }


   static private final HistoComparator HISTO_COMPARATOR = new HistoComparator();


//            final Collection<String> ontos = new HashSet<>( getAllUrisOntoMorphCodes( neoplasm ) );
////         trimMorphCodes( ontos, validTopoMorphs );
//         final List<String> ontosList = new ArrayList<>( ontos );
//         Collections.sort( ontosList );
//         writer.write( neoplasm.getPatientId() + " ONTOS : " + ontosList + "\n" );
//
//         final String prim1 = TopoMorphValidator.getInstance().getPrimaryMorphCode( neoplasm.getUri() );
//         final Collection<String> prims = neoplasm.getAllUris()
//                                                  .stream()
//                                                  .map( TopoMorphValidator.getInstance()::getPrimaryMorphCode )
//                                                  .filter( s -> !s.isEmpty() )
//                                                  .sorted()
//                                                  .collect( Collectors.toList() );
////         trimMorphCodes( prims, validTopoMorphs );
//         writer.write( neoplasm.getPatientId() + " PRIMS : " + prim1 + "   " + prims + "\n" );
//
//         final Collection<String> table = new HashSet<>( getAllUrisTableMorphCodes( neoplasm ) );
////         trimMorphCodes( table, validTopoMorphs );
//         final List<String> tableList = new ArrayList<>( table );
//         Collections.sort( tableList );
//         writer.write( neoplasm.getPatientId() + " TABLE : " + tableList + "\n" );
//
//         final Collection<String> rooty = new HashSet<>( getAllRootsTableMorphCodes( neoplasm ) );
////         trimMorphCodes( rooty, validTopoMorphs );
//         final List<String> rootyList = new ArrayList<>( rooty );
//         Collections.sort( rootyList );


//   static private Collection<String> getMainOntoMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                        final Collection<String> validTopoMorphs ) {
//      final Collection<String> ontoMorphCodes = neoplasms.stream()
//                                                         .map( Morphology::getMainUrisOntoMorphCodes )
//                                                         .flatMap( Collection::stream )
//                                                         .collect( Collectors.toSet() );
//      trimMorphCodes( ontoMorphCodes, validTopoMorphs );
//      return ontoMorphCodes;
//   }
//
//   static private Collection<String> getAllOntoMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                      final Collection<String> validTopoMorphs ) {
//      final Collection<String> ontoMorphCodes = neoplasms.stream()
//                                                         .map( Morphology::getAllUrisOntoMorphCodes )
//                                                         .flatMap( Collection::stream )
//                                                         .collect( Collectors.toSet() );
//      trimMorphCodes( ontoMorphCodes, validTopoMorphs );
//      return ontoMorphCodes;
//   }
//
//
//   static private Collection<String> getMainBroadMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                             final Collection<String> validTopoMorphs ) {
//      final Collection<String> primaryMorphCodes =
//            neoplasms.stream()
//                     .map( ConceptAggregate::getUri )
//                     .distinct()
//                     .map( TopoMorphValidator.getInstance()::getBroadMorphCode )
//                     .flatMap( Collection::stream )
//                     .filter( s -> !s.isEmpty() )
//                     .collect( Collectors.toSet() );
//      trimMorphCodes( primaryMorphCodes, validTopoMorphs );
//      return primaryMorphCodes;
//   }
//
//
//
//   static private Collection<String> getAllBroadMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                            final Collection<String> validTopoMorphs ) {
//      final Collection<String> broadMorphCodes =
//            neoplasms.stream()
//                     .map( ConceptAggregate::getAllUris )
//                     .flatMap( Collection::stream )
//                     .map( TopoMorphValidator.getInstance()::getBroadMorphCode )
//                     .flatMap( Collection::stream )
//                     .filter( s -> !s.isEmpty() )
//                     .collect( Collectors.toSet() );
//      trimMorphCodes( broadMorphCodes, validTopoMorphs );
//      return broadMorphCodes;
//   }
//
//   static private Collection<String> getMainTableMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                         final Collection<String> validTopoMorphs ) {
//      final Collection<String> tableMorphCodes =
//            neoplasms.stream()
//                     .map( Morphology::getMainUrisTableMorphCodes )
//                     .collect( Collectors.toSet() );
//      trimMorphCodes( tableMorphCodes, validTopoMorphs );
//      return tableMorphCodes;
//   }
//
//   static private Collection<String> getAllTableMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                            final Collection<String> validTopoMorphs ) {
//      final Collection<String> tableMorphCodes =
//            neoplasms.stream()
//                     .map( Morphology::getAllUrisTableMorphCodes )
//                     .flatMap( Collection::stream )
//                     .collect( Collectors.toSet() );
//      trimMorphCodes( tableMorphCodes, validTopoMorphs );
//      return tableMorphCodes;
//   }
//
//   static private Collection<String> getAllRootsMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                            final Collection<String> validTopoMorphs ) {
//      final Collection<String> rootMorphCodes =
//            neoplasms.stream()
//                     .map( Morphology::getAllRootsTableMorphCodes )
//                     .flatMap( Collection::stream )
//                     .collect( Collectors.toSet() );
//      trimMorphCodes( rootMorphCodes, validTopoMorphs );
//      return rootMorphCodes;
//   }
//
//   static private Collection<String> getMainRootsMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                         final Collection<String> validTopoMorphs ) {
//      final Collection<String> rootMorphCodes =
//            neoplasms.stream()
//                     .map( Morphology::getMainRootsTableMorphCodes )
//                     .flatMap( Collection::stream )
//                     .collect( Collectors.toSet() );
//      trimMorphCodes( rootMorphCodes, validTopoMorphs );
//      return rootMorphCodes;
//   }


   static private Map<String,Integer> getMultiHistoCodeMap( final Collection<String> codes ) {
      final Map<String,Collection<String>> histoMorphMap = new HashMap<>();
      for ( String code : codes ) {
         histoMorphMap.computeIfAbsent( code.substring( 0,4 ), t -> new ArrayList<>() ).add( code );
      }
      return codes.stream()
                   .collect( Collectors.toMap( Function.identity(),
                                               c -> histoMorphMap.get( c.substring( 0,4 ) ).size() ) );
   }


   static private Collection<String> getMultiHistoCodes( final Collection<String> codes ) {
      final Map<String,Collection<String>> histoMorphMap = new HashMap<>();
      for ( String code : codes ) {
         histoMorphMap.computeIfAbsent( code.substring( 0,4 ), t -> new ArrayList<>() ).add( code );
      }
      return histoMorphMap.values()
                           .stream()
                           .filter( v -> v.size() > 1 )
                           .flatMap( Collection::stream )
                           .collect( Collectors.toSet() );
   }

   static private Collection<String> getBehavingCodes( final Collection<String> codes ) {
      return codes.stream()
                 .filter( m -> !m.endsWith( "/3" ) )
                 .collect( Collectors.toList() );
   }

//   static private Collection<String> getUriPrimaryCodes( final Collection<ConceptAggregate> neoplasms ) {
//      return neoplasms.stream()
//                      .map( ConceptAggregate::getUri )
//                      .map( TopoMorphValidator.getInstance()::getPrimaryMorphCode )
//                      .collect( Collectors.toSet() );
//   }


   static private Collection<String> getCommonCodes( final Collection<String> codes1,
                                                     final Collection<String> codes2 ) {
      final List<String> overlapCodes = new ArrayList<>( codes1 );
      overlapCodes.retainAll( codes2 );
      return overlapCodes;
   }


   static private String getBestMorphCode( final Collection<ConceptAggregate> neoplasms,
                                           final Collection<String> validTopoMorphs ) {
      final Map<Integer,Collection<String>> hitCounts = getHitCounts( neoplasms, validTopoMorphs );
      if ( hitCounts.isEmpty() ) {
         return "";
      }
      final List<Integer> counts = new ArrayList<>( hitCounts.keySet() );
      Collections.sort( counts );
      return hitCounts.get( counts.get( counts.size()-1 ) )
                      .stream()
                      .max( HISTO_COMPARATOR )
                      .orElse( "" );
   }


   static private Map<Integer,Collection<String>> getHitCounts_6( final Collection<ConceptAggregate> neoplasms,
                                      final Collection<String> validTopoMorphs ) {
      final Collection<String> mainUris = getMainUris( neoplasms );
      final Collection<String> nonMainUris = getNonMainUris( neoplasms, mainUris );
      final Collection<String> mainOntoCodes = getUrisOntoMorphCodes( mainUris );
      final Collection<String> mainExactCodes = getUrisExactMorphCodes( mainUris );
      final Collection<String> mainBroadCodes = getUrisBroadMorphCodes( mainUris );
      final Collection<String> otherOntoCodes = getUrisOntoMorphCodes( nonMainUris );
      final Collection<String> otherExactCodes = getUrisExactMorphCodes( nonMainUris );
      final Collection<String> otherBroadCodes = getUrisBroadMorphCodes( nonMainUris );
      if ( mainOntoCodes.isEmpty() && mainExactCodes.isEmpty() && mainBroadCodes.isEmpty()
           && otherOntoCodes.isEmpty()  && otherExactCodes.isEmpty() && otherBroadCodes.isEmpty() ) {
         return Collections.emptyMap();
      }
      mainBroadCodes.removeAll( mainExactCodes );
      otherBroadCodes.removeAll( otherExactCodes );

      final Collection<String> mainCodes = new HashSet<>( mainOntoCodes );
      mainCodes.addAll( mainExactCodes );
      mainCodes.addAll( mainBroadCodes );
      final Collection<String> otherCodes = new HashSet<>( otherOntoCodes );
      otherCodes.addAll( otherExactCodes );
      otherCodes.addAll( otherBroadCodes );
//      final Collection<String> mainMultiHistCodes = getMultiHistoCodes( mainCodes );
//      final Collection<String> mainBehavingCodes = getBehavingCodes( mainCodes );
//      final Collection<String> otherMultiHistCodes = getMultiHistoCodes( otherCodes );
//      final Collection<String> otherBehavingCodes = getBehavingCodes( otherCodes );
      final Collection<String> allCodes = new HashSet<>( mainCodes );
      allCodes.addAll( otherCodes );
      final Map<Integer,Collection<String>> hitCounts = new HashMap<>();
      for ( String code : allCodes ) {
         int count = 0;
         count += mainOntoCodes.contains( code ) ? 4 : 0;
         count += mainExactCodes.contains( code ) ? 4 : 0;
         count += mainBroadCodes.contains( code ) ? 3 : 0;
//         count += mainRootCodes.contains( code ) ? 2 : 0;
//         count += mainMultiHistCodes.contains( code.substring( 0,4 ) ) ? 1 : 0;
//         count += mainBehavingCodes.contains( code ) ? 1 : 0;
         count += otherOntoCodes.contains( code ) ? 3 : 0;
         count += otherExactCodes.contains( code ) ? 3 : 0;
         count += otherBroadCodes.contains( code ) ? 2 : 0;
//         count += allRootCodes.contains( code ) ? 1 : 0;
//         count += otherMultiHistCodes.contains( code.substring( 0,4 ) ) ? 1 : 0;
//         count += otherBehavingCodes.contains( code ) ? 1 : 0;
         // Reduce score for lower codes
         count -= code.startsWith( "800" ) ? 10 : 0;
         count -= code.startsWith( "801" ) ? 6 : 0;
         hitCounts.computeIfAbsent( count, c -> new HashSet<>() ).add( code );
      }
      return hitCounts;
  }

  static private String HIT_COUNT_TEXT = "";

   static private Map<Integer,Collection<String>> getHitCounts( final Collection<ConceptAggregate> neoplasms,
                                                                final Collection<String> validTopoMorphs ) {
      final Map<String,Integer> uriStrengths = getUriStrengths( neoplasms );
      final Collection<String> allUris = new HashSet<>(uriStrengths.keySet() );
      final Map<String,Integer> ontoStrengths = getUrisOntoMorphCodeMap( allUris, uriStrengths );
      final Map<String,Integer> exactStrengths = getUrisExactMorphCodeMap( allUris, uriStrengths );
      final Map<String,Integer> broadStrengths = getUrisBroadMorphCodeMap( allUris, uriStrengths );
      final Collection<String> allCodes = new HashSet<>( ontoStrengths.keySet() );
      allCodes.addAll( exactStrengths.keySet() );
      allCodes.addAll( broadStrengths.keySet() );
      trimMorphCodes( allCodes, validTopoMorphs );
      final Map<String,Integer> multiCodesMap = getMultiHistoCodeMap( allCodes );

      final Map<Integer,Collection<String>> hitCounts = new HashMap<>();
      for ( String code : allCodes ) {
         int count = 0;
         count += ontoStrengths.getOrDefault( code, 0 );
         count += exactStrengths.getOrDefault( code, 0 );
         count += broadStrengths.getOrDefault( code, 0 );
         count += multiCodesMap.getOrDefault( code, 0 )*15;
         if ( code.startsWith( "801" ) ) {
            count /= 40;
         }
         hitCounts.computeIfAbsent( count, c -> new HashSet<>() ).add( code );
      }
                  HIT_COUNT_TEXT = "";
                  ontoStrengths.keySet().retainAll( allCodes );
                  exactStrengths.keySet().retainAll( allCodes );
                  broadStrengths.keySet().retainAll( allCodes );
                  HIT_COUNT_TEXT += "OntoStrengths: " + ontoStrengths.entrySet().stream()
                                                                .map( e -> e.getKey() +" " + e.getValue() )
                                                                .collect(  Collectors.joining(",") ) + "\n";
                  HIT_COUNT_TEXT += "ExctStrengths: " + exactStrengths.entrySet().stream()
                                                                .map( e -> e.getKey() +" " + e.getValue() )
                                                                .collect(  Collectors.joining(",") ) + "\n";
                  HIT_COUNT_TEXT += "BrodStrengths: " + broadStrengths.entrySet().stream()
                                                                .map( e -> e.getKey() +" " + e.getValue() )
                                                                .collect(  Collectors.joining(",") ) + "\n";
                  HIT_COUNT_TEXT += "   Hit Counts: " + hitCounts.entrySet().stream()
                                                                .map( e -> e.getKey() +" " + e.getValue() )
                                                                .collect(  Collectors.joining(",") ) + "\n";
      return hitCounts;
   }



   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   MAIN
   //
   /////////////////////////////////////////////////////////////////////////////////////////



   static private List<String> getMainMorphCodes( final Collection<ConceptAggregate> neoplasms,
                                                 final Collection<String> validTopoMorphs ) {
      final Collection<String> mainUris = getMainUris( neoplasms );
      final Collection<String> mainOntoCodes = getUrisOntoMorphCodes( mainUris );
      final Collection<String> mainExactCodes = getUrisExactMorphCodes( mainUris );
      final Collection<String> mainBroadCodes = getUrisBroadMorphCodes( mainUris );
      final Collection<String> mainCodes = new HashSet<>( mainOntoCodes );
      mainCodes.addAll( mainExactCodes );
      mainCodes.addAll( mainBroadCodes );
      trimMorphCodes( mainCodes, validTopoMorphs );
      final List<String> codes = new ArrayList<>( mainCodes );
      codes.sort( new HistoComparator() );
      return codes;
   }

   static private List<String> getOtherMorphCodes( final Collection<ConceptAggregate> neoplasms,
                                                  final Collection<String> validTopoMorphs ) {
      final Collection<String> mainUris = getMainUris( neoplasms );
      final Collection<String> nonMainUris = getNonMainUris( neoplasms, mainUris );
      final Collection<String> otherOntoCodes = getUrisOntoMorphCodes( nonMainUris );
      final Collection<String> otherExactCodes = getUrisExactMorphCodes( nonMainUris );
      final Collection<String> otherBroadCodes = getUrisBroadMorphCodes( nonMainUris );
      final Collection<String> otherCodes = new HashSet<>( otherOntoCodes );
      otherCodes.addAll( otherExactCodes );
      otherCodes.addAll( otherBroadCodes );
      trimMorphCodes( otherCodes, validTopoMorphs );
      final List<String> codes = new ArrayList<>( otherCodes );
      codes.sort( new HistoComparator() );
      return codes;
   }

   static private List<String> getAllMorphCodes( final Collection<ConceptAggregate> neoplasms,
                                                   final Collection<String> validTopoMorphs ) {
      final Collection<String> allUris = getAllUris( neoplasms );
      final Collection<String> allOntoCodes = getUrisOntoMorphCodes( allUris );
      final Collection<String> allExactCodes = getUrisExactMorphCodes( allUris );
      final Collection<String> allBroadCodes = getUrisBroadMorphCodes( allUris );
      final Collection<String> allCodes = new HashSet<>( allOntoCodes );
      allCodes.addAll( allExactCodes );
      allCodes.addAll( allBroadCodes );
      trimMorphCodes( allCodes, validTopoMorphs );
      final List<String> codes = new ArrayList<>( allCodes );
      codes.sort( new HistoComparator() );
      return codes;
   }




//   static private List<String> getMainMorphCodes( final ConceptAggregate neoplasm,
//                                                 final Collection<String> validTopoMorphs) {
//      final Collection<String> morphs = new HashSet<>( getMainUrisOntoMorphCodes( neoplasm ) );
//      trimMorphCodes( morphs, validTopoMorphs );
//      if ( morphs.isEmpty() ) {
//         morphs.add( getMainUrisTableMorphCodes( neoplasm ) );
//         trimMorphCodes( morphs, validTopoMorphs );
//         if ( morphs.isEmpty() ) {
//            morphs.addAll( getMainRootsTableMorphCodes( neoplasm ) );
//            trimMorphCodes( morphs, validTopoMorphs );
//         }
//      }
//      final List<String> morphList = new ArrayList<>( morphs );
//      Collections.sort( morphList );
//      return morphList;
//   }
//
//   static private List<String> getMainUrisOntoMorphCodes( final ConceptAggregate neoplasm ) {
//      return getOntoMorphCodes( neoplasm.getUri() ).stream()
//                                                   .filter( m -> !m.startsWith( "8000" ) )
//                                                   .filter( m -> !m.isEmpty() )
//                                                   .distinct()
//                                                   .sorted()
//                                                   .collect( Collectors.toList() );
//   }

//   static private String getMainUrisTableMorphCodes( final ConceptAggregate neoplasm ) {
//      return TopoMorphValidator.getInstance().getExactMorphCode( neoplasm.getUri() );
//   }
//
//   static private List<String> getMainRootsTableMorphCodes( final ConceptAggregate neoplasm ) {
//      return neoplasm.getUriRootsMap().getOrDefault( neoplasm.getUri(), Collections.emptyList() )
//                     .stream()
//                     .map( TopoMorphValidator.getInstance()::getExactMorphCode )
//                     .filter( s -> !s.isEmpty() )
//                     .filter( m -> !m.startsWith( "8000" ) )
//                     .distinct()
//                     .sorted()
//                     .collect( Collectors.toList() );
//   }


   /////////////////////////////////////////////////////////////////////////////////////////
   //
   //                   ALL
   //
   /////////////////////////////////////////////////////////////////////////////////////////



//   static private List<String> getAllMorphCodes( final Collection<ConceptAggregate> neoplasms,
//                                                 final Collection<String> validTopoMorphs ) {
//      final List<String> morphs = neoplasms.stream()
//                        .map( n -> getAllMorphCodes( n, validTopoMorphs ) )
//                      .flatMap( Collection::stream )
//                      .distinct()
//                      .sorted()
//                     .collect( Collectors.toList() );
//      trimMorphCodes( morphs, validTopoMorphs );
//      morphs.sort( new HistoComparator() );
//      return morphs;
//   }
//
//   static private List<String> getAllMorphCodes( final ConceptAggregate neoplasm,
//                                                 final Collection<String> validTopoMorphs) {
//      final Collection<String> morphs = new HashSet<>( getAllUrisOntoMorphCodes( neoplasm ) );
//      trimMorphCodes( morphs, validTopoMorphs );
//      if ( morphs.isEmpty() ) {
//         morphs.addAll( getAllUrisTableMorphCodes( neoplasm ) );
//         trimMorphCodes( morphs, validTopoMorphs );
//         if ( morphs.isEmpty() ) {
//            morphs.addAll( getAllRootsTableMorphCodes( neoplasm ) );
//            trimMorphCodes( morphs, validTopoMorphs );
//         }
//      }
//      final List<String> morphList = new ArrayList<>( morphs );
//      Collections.sort( morphList );
//      return morphList;
//   }

   static private String QUOTIENT_TEXT = "";

   static private Map<String,Integer> getUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final List<KeyValue<String, Double>> uriQuotients = neoplasms.stream()
                                                                   .map( n -> UriScoreUtil.mapUriQuotients( n.getAllUris(),
                                                                                                            n.getUriRootsMap(),
                                                                                                            n.getMentions() ) )
                                                                   .flatMap( Collection::stream )
                                                                   .collect( Collectors.toList() );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         final int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         final int strength = (int)Math.ceil( quotients.getValue()*100 );
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
                  QUOTIENT_TEXT = "Quotients: " + uriQuotients.stream()
                                                            .map( kv -> kv.getKey() +" " + kv.getValue() )
                                                            .collect(  Collectors.joining(",") ) + "\n";
                 QUOTIENT_TEXT +=  "Strengths: " + uriStrengths.entrySet().stream()
                        .map( e -> e.getKey() +" " + e.getValue() )
                        .collect(  Collectors.joining(",") ) + "\n";
      return uriStrengths;
   }


   // Allows for uris that have tied quotients
   static private Collection<String> getMainUris( final Collection<ConceptAggregate> neoplasms ) {
      final Map<String,Integer> uriStrengths = getUriStrengths( neoplasms );
      double max = uriStrengths.values().stream()
                               .mapToInt( i -> i )
                               .max()
                               .orElse( -1 );
//      final List<KeyValue<String, Double>> uriQuotients = neoplasms.stream()
//               .map( n -> UriScoreUtil.mapUriQuotients( n.getAllUris(),
//                                                        n.getUriRootsMap(),
//                                                        n.getMentions() ) )
//               .flatMap( Collection::stream )
//               .collect( Collectors.toList() );
//      double max = uriQuotients.stream()
//                               .mapToDouble( KeyValue::getValue )
//                               .max()
//                               .orElse( -1 );
      if ( max < 0 ) {
         return Collections.emptyList();
      }
//      return uriQuotients.stream()
//                         .filter( kv -> kv.getValue() >= max )
//                         .map( KeyValue::getKey )
//                         .collect( Collectors.toSet() );
//      return neoplasms.stream()
//                      .map( ConceptAggregate::getUri )
//                      .collect( Collectors.toSet() );
      return uriStrengths.entrySet().stream()
                         .filter( e -> e.getValue() == max )
                         .map( Map.Entry::getKey )
                         .collect( Collectors.toSet() );
   }

   static private Collection<String> getNonMainUris( final Collection<ConceptAggregate> neoplasms ) {
      return getNonMainUris( neoplasms, getMainUris( neoplasms ) );
   }

   static private Collection<String> getNonMainUris( final Collection<ConceptAggregate> neoplasms,
                                                     final Collection<String> mainUris ) {
      final Collection<String> allUris = getAllUris( neoplasms );
      allUris.removeAll( mainUris );
      return allUris;
   }

   static private Collection<String> getAllUris( final Collection<ConceptAggregate> neoplasms ) {
      return neoplasms.stream()
                      .map( ConceptAggregate::getAllUris )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toSet() );
   }



   static private List<String> getUrisOntoMorphCodes( final Collection<String> uris ) {
      return uris.stream()
                     .map( Morphology::getOntoMorphCodes )
                     .flatMap( Collection::stream )
                     .filter( m -> !m.startsWith( "800" ) )
                 .filter( m -> !m.isEmpty() )
                 .distinct()
                     .sorted()
                     .collect( Collectors.toList() );
   }

   static private String URI_ONTO_TEXT = "";
   static private String URI_BROD_TEXT = "";
   static private String URI_EXCT_TEXT = "";

   static private Map<String,Integer> getUrisOntoMorphCodeMap( final Collection<String> uris,
                                                                        final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      URI_ONTO_TEXT = "";
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
         final Collection<String> codes = getOntoMorphCodes( uri );
                      URI_ONTO_TEXT += codes.isEmpty() ? "" : "Onto " + uri + " " + codes + "\n";
         for ( String code : codes ) {
            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
            ontoMorphStrengths.put( code, previousStrength + strength );
         }
      }
      return ontoMorphStrengths;
   }


   static private List<String> getUrisBroadMorphCodes( final Collection<String> uris ) {
      return uris.stream()
                 .map( TopoMorphValidator.getInstance()::getBroadMorphCode )
                 .flatMap( Collection::stream )
                 .filter( m -> !m.startsWith( "800" ) )
                 .filter( m -> !m.isEmpty() )
                 .distinct()
                 .sorted()
                 .collect( Collectors.toList() );
   }

   static private Map<String,Integer> getUrisBroadMorphCodeMap( final Collection<String> uris,
                                                               final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      URI_BROD_TEXT = "";
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
         final Collection<String> codes = TopoMorphValidator.getInstance().getBroadMorphCode( uri );
                       URI_BROD_TEXT += codes.isEmpty() ? "" : "Broad " + uri + " " + codes + "\n";
         for ( String code : codes ) {
            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
            ontoMorphStrengths.put( code, previousStrength + strength );
         }
      }
      return ontoMorphStrengths;
   }


   static private List<String> getUrisExactMorphCodes( final Collection<String> uris ) {
      return uris.stream()
                 .map( TopoMorphValidator.getInstance()::getExactMorphCode )
                 .filter( m -> !m.startsWith( "800" ) )
                 .filter( m -> !m.isEmpty() )
                 .distinct()
                 .sorted()
                 .collect( Collectors.toList() );
   }

   static private Map<String,Integer> getUrisExactMorphCodeMap( final Collection<String> uris,
                                                                final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      URI_EXCT_TEXT = "";
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
         final String code = TopoMorphValidator.getInstance().getExactMorphCode( uri );
                          URI_EXCT_TEXT += code.isEmpty() ? "" : "Exact " + uri + " " + code + "\n";
         final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
         ontoMorphStrengths.put( code, previousStrength + strength );
      }
      return ontoMorphStrengths;
   }



//   static private List<String> getAllUrisOntoMorphCodes( final ConceptAggregate neoplasm ) {
//      return neoplasm.getAllUris()
//                      .stream()
//                      .map( Morphology::getOntoMorphCodes )
//                      .flatMap( Collection::stream )
//                     .filter( m -> !m.startsWith( "8000" ) )
//                     .distinct()
//                      .sorted()
//                      .collect( Collectors.toList() );
//   }
//
//   static private List<String> getAllUrisBroadCodes( final ConceptAggregate neoplasm ) {
//      return neoplasm.getAllUris()
//                       .stream()
//                       .map( TopoMorphValidator.getInstance()::getBroadMorphCode )
//                     .flatMap( Collection::stream )
//                       .filter( s -> !s.isEmpty() )
//                     .distinct()
//                     .sorted()
//                       .collect( Collectors.toList() );
//   }



//   static private List<String> getAllUrisTableMorphCodes( final ConceptAggregate neoplasm ) {
//      return neoplasm.getAllUris()
//                     .stream()
//                     .map( TopoMorphValidator.getInstance()::getExactMorphCode )
//                     .filter( s -> !s.isEmpty() )
//                     .distinct()
//                     .sorted()
//                     .collect( Collectors.toList() );
//   }
//
//   static private List<String> getAllRootsTableMorphCodes( final ConceptAggregate neoplasm ) {
//      return neoplasm.getUriRootsMap().values()
//                     .stream()
//                     .flatMap( Collection::stream )
//                     .distinct()
//                     .map( TopoMorphValidator.getInstance()::getExactMorphCode )
//                     .filter( s -> !s.isEmpty() )
//                     .filter( m -> !m.startsWith( "8000" ) )
//                     .distinct()
//                     .sorted()
//                     .collect( Collectors.toList() );
//   }


   static private List<String> getHighMorphCodes( final String uri,
                                                  final Collection<String> uriRoots,
                                                  final Collection<String> validTopoMorphs ) {
      final Collection<String> morphs = new HashSet<>( getOntoMorphCodes( uri ) );
      morphs.addAll( getTableMorphCodes( uri, uriRoots ) );
      trimMorphCodes( morphs, validTopoMorphs );
      final List<String> morphList = new ArrayList<>( morphs );
//      Collections.sort( morphList );
      morphList.sort( new HistoComparator() );
      return morphList;
   }

   static private void trimMorphCodes( final Collection<String> morphs,
                                              final Collection<String> validTopoMorphs ) {
      morphs.remove( "" );
      final Collection<String> removals = morphs.stream()
                                                .filter( m -> m.startsWith( "800" ) )
                                                .collect( Collectors.toSet() );
      morphs.removeAll( removals );
//      if ( morphs.size() > 1 && !validTopoMorphs.isEmpty() ) {
//         morphs.retainAll( validTopoMorphs );
//      }
   }


   static private List<String> getOntoMorphCodes( final String uri ) {
      return Neo4jOntologyConceptUtil.getIcdoCodes( uri ).stream()
                                     .filter( i -> !i.startsWith( "C" ) )
                                     .filter( i -> !i.contains( "-" ) )
                                     .filter( i -> i.length() > 3 )
                                     .distinct()
                                     .sorted()
                                     .collect( Collectors.toList() );
   }

   static private List<String> getTableMorphCodes( final String uri,
                                                   final Collection<String> uriRoots ) {
      final Collection<String> uris = new HashSet<>( uriRoots );
      uris.add( uri );
      return getTableMorphCodes( uris );
   }

   static private List<String> getTableMorphCodes( final Collection<String> uris ) {
      return uris.stream()
                 .map( TopoMorphValidator.getInstance()::getExactMorphCode )
                 .distinct()
                 .sorted()
                 .collect( Collectors.toList() );
   }





   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                         final Collection<ConceptAggregate> allConcepts,
                                         final HighMorphStore neoplasmHighStore,
                                         final FullMorphStore neoplasmFullStore,
                                         final FullMorphStore patientFullStore ) {
      final List<Integer> features = new ArrayList<>();

      if ( !neoplasmHighStore._highUri.isEmpty() ) {
         _bestUri = neoplasmHighStore._highUri;
      } else {
         _bestUri = "";   // TODO  Use whole patient?
      }
//      _morphologyCodes = neoplasmHighStore._morphCodes.isEmpty()
//                         ? neoplasmFullStore._morphCodes
//                         : neoplasmHighStore._morphCodes;

      _morphologyCodes = neoplasmFullStore._allMorphCodes;

      //1.  !!!!!  Best URI  !!!!!
      //    ======  CONCEPT  =====

      final Collection<ConceptAggregate> bestInNeoplasmMainConcepts = getUriIsMain( _bestUri,
                                                                                    neoplasmFullStore._concepts );
      final Collection<ConceptAggregate> bestInNeoplasmAllConcepts = getUriInAny( _bestUri,
                                                                                  neoplasmFullStore._concepts );
      final Collection<ConceptAggregate> bestInPatientMainConcepts = getUriIsMain( _bestUri,
                                                                                   patientFullStore._concepts );
      final Collection<ConceptAggregate> bestInPatientAllConcepts = getUriInAny( _bestUri,
                                                                                 patientFullStore._concepts );
      addCollectionFeatures( features,
                             neoplasmFullStore._concepts,
                             patientFullStore._concepts );
      addStandardFeatures( features, bestInNeoplasmMainConcepts,
                           neoplasmFullStore._concepts,
                           patientFullStore._concepts );
      addStandardFeatures( features, bestInNeoplasmAllConcepts,
                           neoplasmFullStore._concepts,
                           patientFullStore._concepts );
      addStandardFeatures( features, bestInPatientMainConcepts, patientFullStore._concepts );
      addStandardFeatures( features, bestInPatientAllConcepts, patientFullStore._concepts );


      //    ======  MENTION  =====

      final Collection<Mention> bestInFirstMentions
            = neoplasmHighStore._mentions.stream()
                                         .filter( m -> m.getClassUri()
                                                        .equals( _bestUri ) )
                                         .collect( Collectors.toSet() );
      final Collection<Mention> bestInNeoplasmMentions
            = neoplasmFullStore._mentions.stream()
                                         .filter( m -> m.getClassUri()
                                                        .equals( _bestUri ) )
                                         .collect( Collectors.toSet() );
      final Collection<Mention> bestInPatientMentions
            = patientFullStore._mentions.stream()
                                        .filter( m -> m.getClassUri()
                                                       .equals( _bestUri ) )
                                        .collect( Collectors.toSet() );
      addCollectionFeatures( features,
                             neoplasmHighStore._mentions,
                             neoplasmFullStore._mentions,
                             patientFullStore._mentions );
      addStandardFeatures( features,
                           bestInFirstMentions,
                           neoplasmHighStore._mentions,
                           neoplasmFullStore._mentions,
                           patientFullStore._mentions );
      addStandardFeatures( features,
                           bestInNeoplasmMentions,
                           neoplasmFullStore._mentions,
                           patientFullStore._mentions );
      addStandardFeatures( features, bestInPatientMentions, patientFullStore._mentions );


      //    ======  URI  =====
      addCollectionFeatures( features,
                             neoplasmFullStore._mainUris,
                             patientFullStore._mainUris );
      addCollectionFeatures( features,
                             neoplasmFullStore._allUris,
                             patientFullStore._allUris );

      addStandardFeatures( features, neoplasmFullStore._mainUris, neoplasmFullStore._mentions );
      addStandardFeatures( features, patientFullStore._mainUris, patientFullStore._mentions );

      addStandardFeatures( features, neoplasmFullStore._allUris, neoplasmFullStore._mentions );
      addStandardFeatures( features, patientFullStore._allUris, patientFullStore._mentions );


      //2.  !!!!!  URI Branch  !!!!!
      //    ======  URI  =====
      final Map<String, Collection<String>> patientAllUriRootsMap = patientFullStore._uriRootsMap;

      final Map<String, Collection<String>> neoplasmAllUriRootsMap = neoplasmFullStore._uriRootsMap;

      final Map<String, List<Mention>> neoplasmUriMentions = mapUriMentions( neoplasmFullStore._mentions );
      final Map<String, List<Mention>> patientSiteUriMentions = mapUriMentions( patientFullStore._mentions );

//      final Map<String, Integer> highMentionBranchCounts = mapAllUriBranchMentionCounts( neoplasmHighUriMentions,
//                                                                                          neoplasmHighAllUriRootsMap );
      final Map<String, Integer> neoplasmMentionBranchCounts = mapAllUriBranchMentionCounts( neoplasmUriMentions,
                                                                                             neoplasmAllUriRootsMap );
      final Map<String, Integer> patientMentionBranchCounts = mapAllUriBranchMentionCounts( patientSiteUriMentions,
                                                                                            patientAllUriRootsMap );
      addCollectionFeatures( features,
//                             highMentionBranchCounts.keySet(),
                             neoplasmMentionBranchCounts.keySet(),
                             patientMentionBranchCounts.keySet() );


      //    ======  CONCEPT  =====
      final Map<String, Integer> bestNeoplasmMainConceptBranchCounts
            = mapUriBranchConceptCounts( bestInNeoplasmMainConcepts, neoplasmAllUriRootsMap );
      final Map<String, Integer> bestPatientMainConceptBranchCounts
            = mapUriBranchConceptCounts( bestInPatientMainConcepts, neoplasmAllUriRootsMap );

      final Map<String, Integer> bestNeoplasmAllConceptBranchCounts
            = mapUriBranchConceptCounts( bestInNeoplasmAllConcepts, neoplasmAllUriRootsMap );
      final Map<String, Integer> bestPatientAllConceptBranchCounts
            = mapUriBranchConceptCounts( bestInPatientAllConcepts, neoplasmAllUriRootsMap );

      final Map<String, Integer> neoplasmConceptBranchCounts
            = mapUriBranchConceptCounts( neoplasmFullStore._concepts, neoplasmAllUriRootsMap );
      final Map<String, Integer> patientConceptBranchCounts
            = mapUriBranchConceptCounts( patientFullStore._concepts, neoplasmAllUriRootsMap );

      final int bestNeoplasmMainConceptBranchCount = getBranchCountsSum( bestNeoplasmMainConceptBranchCounts );
      final int bestPatientMainConceptBranchCount = getBranchCountsSum( bestPatientMainConceptBranchCounts );
      final int bestNeoplasmAllConceptBranchCount = getBranchCountsSum( bestNeoplasmAllConceptBranchCounts );
      final int bestPatientAllConceptBranchCount = getBranchCountsSum( bestPatientAllConceptBranchCounts );
      final int neoplasmConceptBranchCount = getBranchCountsSum( neoplasmConceptBranchCounts );
      final int patientConceptBranchCount = getBranchCountsSum( patientConceptBranchCounts );

      addIntFeatures( features, neoplasmConceptBranchCount, patientConceptBranchCount );

      addStandardFeatures( features,
                           bestNeoplasmMainConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount );
      addStandardFeatures( features,
                           bestNeoplasmAllConceptBranchCount,
                           neoplasmConceptBranchCount,
                           patientConceptBranchCount );
      addStandardFeatures( features, bestPatientMainConceptBranchCount, patientConceptBranchCount );
      addStandardFeatures( features, bestPatientAllConceptBranchCount, patientConceptBranchCount );


      //    ======  MENTION  =====
      final Map<String, Integer> bestNeoplasmMainMentionBranchCounts
            = mapUriBranchMentionCounts( bestInNeoplasmMainConcepts, neoplasmAllUriRootsMap );
      final Map<String, Integer> bestPatientMainMentionBranchCounts
            = mapUriBranchMentionCounts( bestInPatientMainConcepts, patientAllUriRootsMap );

      final Map<String, Integer> bestNeoplasmAllMentionBranchCounts
            = mapUriBranchMentionCounts( bestInNeoplasmAllConcepts, neoplasmAllUriRootsMap );
      final Map<String, Integer> bestPatientAllMentionBranchCounts
            = mapUriBranchMentionCounts( bestInPatientAllConcepts, patientAllUriRootsMap );

      final int bestNeoplasmMainMentionBranchCount = getBranchCountsSum( bestNeoplasmMainMentionBranchCounts );
      final int bestPatientMainMentionBranchCount = getBranchCountsSum( bestPatientMainMentionBranchCounts );
      final int bestNeoplasmAllMentionBranchCount = getBranchCountsSum( bestNeoplasmAllMentionBranchCounts );
      final int bestPatientAllMentionBranchCount = getBranchCountsSum( bestPatientAllMentionBranchCounts );
      final int neoplasmMentionBranchCount = getBranchCountsSum( neoplasmMentionBranchCounts );
      final int patientMentionBranchCount = getBranchCountsSum( patientMentionBranchCounts );

      addIntFeatures( features, neoplasmMentionBranchCount, patientMentionBranchCount );

      addStandardFeatures( features,
                           bestNeoplasmMainMentionBranchCount,
                           neoplasmMentionBranchCount,
                           patientMentionBranchCount );
      addStandardFeatures( features,
                           bestNeoplasmAllMentionBranchCount,
                           neoplasmMentionBranchCount,
                           patientMentionBranchCount );
      addStandardFeatures( features,
                           bestPatientMainMentionBranchCount,
                           patientMentionBranchCount );
      addStandardFeatures( features,
                           bestPatientAllMentionBranchCount,
                           patientMentionBranchCount );


      //3.  !!!!!  URI Depth  !!!!!
      //    ======  URI  =====
      final int neoplasmMainMaxDepth = neoplasmFullStore._mainUris.stream()
                                                                  .mapToInt(
                                                                        u -> neoplasmFullStore._allClassLevelMap.getOrDefault(
                                                                              u, 0 ) )
                                                                  .max()
                                                                  .orElse( 0 );
      final int neoplasmAllMaxDepth = neoplasmFullStore._allUris.stream()
                                                                .mapToInt(
                                                                      u -> neoplasmFullStore._allClassLevelMap.getOrDefault(
                                                                            u, 0 ) )
                                                                .max()
                                                                .orElse( 0 );
      final int patientMainMaxDepth = patientFullStore._mainUris.stream()
                                                                .mapToInt(
                                                                      u -> patientFullStore._allClassLevelMap.getOrDefault(
                                                                            u, 0 ) )
                                                                .max()
                                                                .orElse( 0 );
      final int patientAllMaxDepth = patientFullStore._allClassLevelMap.values()
                                                                       .stream()
                                                                       .mapToInt( i -> i )
                                                                       .max()
                                                                       .orElse( 0 );

      addIntFeatures( features,
                      neoplasmHighStore._classLevel * 2,
                      neoplasmMainMaxDepth * 2,
                      neoplasmAllMaxDepth * 2,
                      patientMainMaxDepth * 2,
                      patientAllMaxDepth * 2 );

      addStandardFeatures( features,
                           neoplasmHighStore._classLevel,
                           neoplasmMainMaxDepth,
                           neoplasmAllMaxDepth,
                           patientMainMaxDepth,
                           patientAllMaxDepth );


      //5.  !!!!!  Runner-Up  !!!!!
      //    ======  CONCEPT  =====
      List<KeyValue<String, Double>> bestUriScores = Collections.emptyList();
         final Map<String, Integer> neoplasmFullAllUriSums
               = UriScoreUtil.mapUriSums( neoplasmFullStore._allUris,
                                          neoplasmFullStore._uriRootsMap,
                                          neoplasmFullStore._uriMentionCounts );
         final Map<String, Double> neoplasmFullAllUriQuotientMap
               = UriScoreUtil.mapUriQuotientsBB( neoplasmFullAllUriSums,
                                                 neoplasmFullStore._allUris.size() );  // All Uris?
         final List<KeyValue<String, Double>> neoplasmFullAllUriQuotientList
               = UriScoreUtil.listUriQuotients( neoplasmFullAllUriQuotientMap );  // All Uris?
         bestUriScores
               = UriScoreUtil.getBestUriScores( neoplasmFullAllUriQuotientList,
                                                neoplasmFullStore._allClassLevelMap,
                                                neoplasmFullStore._uriRootsMap );
         final String bestUri = bestUriScores.get( bestUriScores.size() - 1 )
                                             .getKey();

         final double bestUriScore = bestUriScores.isEmpty()
                                     ? 0
                                     : bestUriScores.get( bestUriScores.size() - 1 )
                                                    .getValue();

         addDoubleDivisionFeature( features, 1, bestUriScore );
         final boolean haveRunnerUp = bestUriScores.size() > 1;
         double runnerUpScore = haveRunnerUp
                                ? bestUriScores.get( bestUriScores.size() - 2 )
                                               .getValue()
                                : 0;
         addDoubleDivisionFeature( features, 1, runnerUpScore );
         addDoubleDivisionFeature( features, runnerUpScore, bestUriScore );

         final String runnerUp = haveRunnerUp
                                 ? bestUriScores.get( bestUriScores.size() - 2 )
                                                .getKey()
                                 : "";
         final Collection<ConceptAggregate> runnerUpPatientMainConcepts
               = haveRunnerUp
                 ? getUriIsMain( runnerUp, patientFullStore._concepts )
                 : Collections.emptyList();

         final Collection<ConceptAggregate> runnerUpPatientAllConcepts
               = haveRunnerUp
                 ? getUriInAny( runnerUp, patientFullStore._concepts )
                 : Collections.emptyList();

         addStandardFeatures( features, runnerUpPatientMainConcepts, patientFullStore._concepts );
         addStandardFeatures( features, runnerUpPatientAllConcepts, patientFullStore._concepts );

         final Map<String, Integer> runnerUpPatientMainConceptBranchCounts
               = mapUriBranchConceptCounts( runnerUpPatientMainConcepts, patientAllUriRootsMap );

         final Map<String, Integer> runnerUpPatientAllConceptBranchCounts
               = mapUriBranchConceptCounts( runnerUpPatientAllConcepts, patientAllUriRootsMap );

         final int runnerUpPatientMainConceptBranchCount = getBranchCountsSum( runnerUpPatientMainConceptBranchCounts );

         final int runnerUpPatientAllConceptBranchCount = getBranchCountsSum( runnerUpPatientAllConceptBranchCounts );

         addIntFeatures( features,
                         neoplasmConceptBranchCount,
                         patientConceptBranchCount,
                         bestNeoplasmMainConceptBranchCount,
                         bestPatientMainConceptBranchCount );
         addStandardFeatures( features, runnerUpPatientMainConceptBranchCount,
                              patientConceptBranchCount,
                              bestPatientMainConceptBranchCount );
         addStandardFeatures( features, runnerUpPatientAllConceptBranchCount,
                              patientConceptBranchCount,
                              bestPatientMainConceptBranchCount );


         //    ======  MENTION  =====
         final Collection<Mention> runnerUpFirstMentions
               = neoplasmHighStore._mentions.stream()
                                            .filter( m -> m.getClassUri()
                                                           .equals( runnerUp ) )
                                            .collect( Collectors.toSet() );
         final Collection<Mention> runnerUpNeoplasmMentions
               = neoplasmFullStore._mentions.stream()
                                            .filter( m -> m.getClassUri()
                                                           .equals( runnerUp ) )
                                            .collect( Collectors.toSet() );
         final Collection<Mention> runnerUpPatientMentions
               = patientFullStore._mentions.stream()
                                           .filter( m -> m.getClassUri()
                                                          .equals( runnerUp ) )
                                           .collect( Collectors.toSet() );

         addStandardFeatures( features,
                              runnerUpFirstMentions,
                              neoplasmHighStore._mentions,
                              neoplasmFullStore._mentions,
                              patientFullStore._mentions,
                              bestInFirstMentions,
                              bestInNeoplasmMentions,
                              bestInPatientMentions );
         addStandardFeatures( features,
                              runnerUpNeoplasmMentions,
                              neoplasmFullStore._mentions,
                              patientFullStore._mentions,
                              bestInNeoplasmMentions,
                              bestInPatientMentions );
         addStandardFeatures( features,
                              runnerUpPatientMentions,
                              patientFullStore._mentions,
                              bestInPatientMentions );

         final Map<String, Integer> runnerUpPatientMainMentionBranchCounts
               = mapUriBranchMentionCounts( runnerUpPatientMainConcepts, patientAllUriRootsMap );

         final Map<String, Integer> runnerUpPatientAllMentionBranchCounts
               = mapUriBranchMentionCounts( runnerUpPatientAllConcepts, patientAllUriRootsMap );

         final int runnerUpPatientMainMentionBranchCount = getBranchCountsSum( runnerUpPatientMainMentionBranchCounts );
         final int runnerUpPatientAllMentionBranchCount = getBranchCountsSum( runnerUpPatientAllMentionBranchCounts );

         addStandardFeatures( features, runnerUpPatientMainMentionBranchCount, patientMentionBranchCount );
         addStandardFeatures( features, runnerUpPatientAllMentionBranchCount, patientMentionBranchCount );


         //    ======  URI  =====
         final Collection<String> runnerUpAllUris
               = haveRunnerUp
                 ? patientFullStore._concepts.stream()
                                             .map( ConceptAggregate::getAllUris )
                                             .filter( s -> s.contains( runnerUp ) )
                                             .flatMap( Collection::stream )
                                             .collect( Collectors.toSet() )
                 : Collections.emptyList();
         addStandardFeatures( features,
                              runnerUpAllUris,
                              patientFullStore._allUris );

         //3.  !!!!!  URI Depth  !!!!!
         //    ======  URI  =====
         final int runnerUpDepth = haveRunnerUp
                                   ? patientFullStore._allClassLevelMap.getOrDefault( runnerUp, 0 )
                                   : 0;
         final int runnerUpMaxDepth
               = runnerUpAllUris.stream()
                                .mapToInt( u -> patientFullStore._allClassLevelMap.getOrDefault( u, 0 ) )
                                .max()
                                .orElse( 0 );

         addIntFeatures( features, runnerUpDepth * 2, runnerUpMaxDepth * 2 );
         addStandardFeatures( features, runnerUpDepth, neoplasmMainMaxDepth,
                              neoplasmAllMaxDepth, patientMainMaxDepth, patientAllMaxDepth );
         addStandardFeatures( features, runnerUpMaxDepth, neoplasmMainMaxDepth,
                              neoplasmAllMaxDepth, patientMainMaxDepth, patientAllMaxDepth );


         // Have Code
         addBooleanFeatures( features, !neoplasmHighStore._bestCode.isEmpty() );
         addBooleanFeatures( features, !neoplasmHighStore._bestCode.isEmpty()
                                    && neoplasmHighStore._bestCode.equals( neoplasmFullStore._bestMorphCode ) );
      addBooleanFeatures( features, !neoplasmHighStore._bestCode.isEmpty()
                                    && neoplasmHighStore._bestCode.equals( patientFullStore._bestMorphCode ) );
      addCollectionFeatures( features,
                             neoplasmHighStore._morphCodes,
                             neoplasmFullStore._allMorphCodes,
                             patientFullStore._allMorphCodes );

      final String bestMorphology = neoplasmFullStore._bestMorphCode;
      _bestHistoCode = bestMorphology.isEmpty()
                       ? getBestHistology( _morphologyCodes )
                       : bestMorphology.substring( 0, 4 );
//      _bestHistoCode = getBestHistology( _morphologyCodes );
      addBooleanFeatures( features,
                          _bestHistoCode.isEmpty(),
                          _bestHistoCode.equals( getBestHistology( neoplasmHighStore._morphCodes ) ),
                          _bestHistoCode.equals( getBestHistology( neoplasmFullStore._allMorphCodes ) ),
                          _bestHistoCode.equals( getBestHistology( patientFullStore._allMorphCodes ) ) );

      addBooleanFeatures( features, neoplasm.isNegated(), neoplasm.isUncertain(), neoplasm.isGeneric(),
                             neoplasm.isConditional() );

         LOGGER.info( "Features: " + features.size() );
         return features;
   }


   private List<Integer> createBehaviorFeatures() {
      final List<Integer> features = new ArrayList<>( 2 );
      _bestBehaveCode = getBestBehavior( _morphologyCodes );
      addBooleanFeatures( features, _bestBehaveCode.isEmpty() );
      return features;
   }

   static private final Function<String, String> getHisto
         = m -> {
      final int i = m.indexOf( "/" );
      return i > 0 ? m.substring( 0, i ) : m;
   };
   static private final Function<String, String> getBehave
         = m -> {
      final int i = m.indexOf( "/" );
      return i > 0 ? m.substring( i + 1 ) : "";
   };


   static public String getBestHistology( final Collection<String> morphs ) {
//      LOGGER.info( "Getting Best Histology from Morphology codes " + String.join( ",", morphs ) );
      final HistoComparator comparator = new HistoComparator();

//      LOGGER.info( "The preferred histology is the first of the following OR the first in numerically sorted order:" );
//      LOGGER.info( "8071 8070 8520 8575 8500 8503 8260 8250 8140 8480 8046 8041 8240 8012 8000 8010" );
//      LOGGER.info( "This ordering came from the best overall fit to gold annotations." );

      return morphs.stream()
                   .map( getHisto )
                   .filter( h -> !h.isEmpty() )
//                   .max( String.CASE_INSENSITIVE_ORDER )
                   .max( comparator )
                   .orElse( "8000" );
   }



   // TODO - use?
   static private final class HistoComparator implements Comparator<String> {
      public int compare( final String histo1, final String histo2 ) {
         final List<String> HISTO_ORDER
//               = Arrays.asList( "8070", "8520", "8503", "8500", "8260", "8250", "8140", "8480", "8046", "8000", "8010" );
//               = Arrays.asList( "8071", "8070", "8520", "8575", "8500", "8503", "8260", "8250", "8140", "8480",
//                                "8046", "8041", "8240", "8012", "8000", "8010" );
//               = Arrays.asList( "804", "848", "814", "824", "825", "826", "850", "875", "852", "807" );
               = Arrays.asList( "807", "814", "804", "848", "824", "825", "826", "850", "875", "852" );
         if ( histo1.equals( histo2 ) ) {
            return 0;
         }
         final String sub1 = histo1.substring( 0, 3 );
         final String sub2 = histo2.substring( 0, 3 );
         if ( !sub1.equals( sub2 ) ) {
            for ( String order : HISTO_ORDER ) {
               if ( sub1.equals( order ) ) {
                  return 1;
               } else if ( sub2.equals( order ) ) {
                  return -1;
               }
            }
         }
         return String.CASE_INSENSITIVE_ORDER.compare( histo1, histo2 );
      }
   }


   // Should be 3 (instead of 2) : 2
   static private String getBestBehavior( final Collection<String> morphs ) {
//      LOGGER.info( "Behavior comes from Histology." );
      final String histo = getBestHistology( morphs );
      if ( histo.isEmpty() ) {
         return "";
      }
      final List<String> behaves = morphs.stream()
                                         .filter( m -> m.startsWith( histo ) )
                                         .map( getBehave )
                                         .filter( b -> !b.isEmpty() )
                                         .distinct()
                                         .sorted()
                                         .collect( Collectors.toList() );
      if ( behaves.isEmpty() ) {
         return "";
      }
      if ( behaves.size() == 1 ) {
//         LOGGER.info( "Only one possible behavior." );
         return behaves.get( 0 );
      }
      if ( behaves.size() == 2 && behaves.contains( "2" ) && behaves.contains( "3" ) ) {
//         LOGGER.info( "Only Behaviors 2 and 3, and Behavior of 3 trumps a behavior of 2." );
         return "3";
      }
//      LOGGER.info( "Removing Behavior 3 (if present) in favor of other highest value." );
      behaves.remove( "3" );
      return behaves.get( behaves.size() - 1 );
   }


}


