package org.healthnlp.deepphe.summary.attribute.cr.topo_major;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/25/2023}
 */
public class TopoMajorNormalizer extends AbstractAttributeNormalizer {


   static private final Map<String,String> URI_MAJOR_MINOR_MAP = new HashMap<>();
   static private final Map<String,Collection<String>> TOPO_MAJOR_MAP_FULL = new HashMap<>();
   static private final String UNDETERMINED = "C80";
   static private final String ILL_DEFINED = "C76";
   static private final String SKIN = "C44";
   static private final String BODY_TISSUE = "C49";

   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      if ( URI_MAJOR_MINOR_MAP.isEmpty() ) {
         fillTopoMajorMaps();
      }
      super.init( infoCollector, dependencies );
//      NeoplasmSummaryCreator.addDebug( "TopoMajor best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getDefaultTextCode() {
      return UNDETERMINED;
   }

   public String getBestCode( final AttributeInfoCollector infoCollector ) {
      return getBestTextCode( infoCollector.getAllRelations() );
   }


   public Map<String,Double> createTextCodeConfidenceMap( final Collection<ConceptAggregateRelation> relations ) {
      final Map<String,Double>  confidenceMap = new HashMap<>();
      for ( ConceptAggregateRelation relation : relations ) {
         final Collection<String> codes = getTextCodes( relation.getTarget().getUri() );
         for ( String code : codes ) {
            final double confidence = confidenceMap.getOrDefault( code, 0d );
            confidenceMap.put( code, confidence + relation.getConfidence() );
         }
         NeoplasmSummaryCreator.addDebug( "TopoMajorNormalizer.createTextCodeConfidenceMap 1 "
                                          + relation.getTarget().getUri() + " "
                                          + relation.getConfidence() + " " + String.join( ",", codes )
                                          + "\n" );
      }
      if ( !confidenceMap.isEmpty() && haveSpecificCodes( confidenceMap ) ) {
         return getSpecificCodes( confidenceMap );
      }
      confidenceMap.clear();
      for ( ConceptAggregateRelation relation : relations ) {
         final Collection<String> codes = relation.getTarget()
                                                  .getAllUris()
                                                  .stream()
                                                  .map( this::getTextCodes )
                                                  .flatMap( Collection::stream )
                                                  .collect( Collectors.toSet() );
         for ( String code : codes ) {
            final double confidence = confidenceMap.getOrDefault( code, 0d );
            confidenceMap.put( code, confidence + relation.getConfidence() );
         }
         NeoplasmSummaryCreator.addDebug( "TopoMajorNormalizer.createTextCodeConfidenceMap 2 "
                                          + relation.getTarget().getUri() + " "
                                          + relation.getConfidence() + " " + String.join( ",", codes )
                                          + "\n" );
      }
      if ( !confidenceMap.isEmpty() && haveSpecificCodes( confidenceMap ) ) {
         return getSpecificCodes( confidenceMap );
      }
      confidenceMap.clear();
      confidenceMap.put( UNDETERMINED, 0.1 );
      return confidenceMap;
   }



//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         return UNDETERMINED;
//      }
//      setAllCodesCount( aggregates.size() );
//      final ConfidenceGroup<CrConceptAggregate> confidenceGroup = new ConfidenceGroup<>( aggregates );
//      final Map<String,Long> countMap = confidenceGroup.getBest()
//                                                           .stream()
//                                                            .map( CrConceptAggregate::getUri )
//                                                           .map( this::getCodes )
//                                                           .flatMap( Collection::stream )
//                                                           .map( c -> c.substring( 0,3 ) )
//                                                           .collect( Collectors.groupingBy( Function.identity(),
//                                                                                            Collectors.counting() ) );
//      if ( !countMap.isEmpty() ) {
//         final String code = getBestCode( countMap );
//         if ( !code.isEmpty() && isSpecificCode( code ) ) {
//            return code;
//         }
//      }
//      final Map<String,Long> countsMap = confidenceGroup.getBest()
//                                                       .stream()
//                                                       .map( CrConceptAggregate::getAllUris )
//                                                      .flatMap( Collection::stream )
//                                                       .map( this::getCodes )
//                                                       .flatMap( Collection::stream )
//                                                       .map( c -> c.substring( 0,3 ) )
//                                                       .collect( Collectors.groupingBy( Function.identity(),
//                                                                                        Collectors.counting() ) );
//      if ( !countsMap.isEmpty() ) {
//         final String code = getBestCode( countsMap );
//         if ( !code.isEmpty() && isSpecificCode( code ) ) {
//            return code;
//         }
//      }
//      final Map<String,Long> countMap2 = confidenceGroup.getNext()
//                                                       .stream()
//                                                       .map( CrConceptAggregate::getUri )
//                                                       .map( this::getCodes )
//                                                       .flatMap( Collection::stream )
//                                                       .map( c -> c.substring( 0,3 ) )
//                                                       .collect( Collectors.groupingBy( Function.identity(),
//                                                                                        Collectors.counting() ) );
//      if ( !countMap2.isEmpty() ) {
//         final String code = getBestCode( countMap2 );
//         if ( !code.isEmpty() ) {
//            return code;
//         }
//      }
//      NeoplasmSummaryCreator.addDebug( "No codes for " + confidenceGroup.getTopmost()
//                                                                        .stream()
//                                                                        .map( CrConceptAggregate::getUri )
//                                                                        .collect( Collectors.joining(",") ) + "\n" );
//      return UNDETERMINED;
//   }
//
//   private String getBestCode( final Map<String,Long> countMap ) {
////      final List<String> codeList = new ArrayList<>( countMap.keySet() );
//      final List<String> codeList = new ArrayList<>( getBestCodes( countMap ) );
//      codeList.sort( Comparator.reverseOrder() );
//      final String bestCode = codeList.get( 0 );
//      if ( bestCode.isEmpty() ) {
//         return "";
//      }
//      final long bestCount = countMap.get( bestCode );
//      setBestCodesCount( (int)bestCount );
////      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( countMap.size() );
//      NeoplasmSummaryCreator.addDebug( "TopoMajorNormalizer "
//                                       + countMap.entrySet().stream()
//                                                 .map( e -> e.getKey() + ":" + e.getValue() )
//                                                 .collect( Collectors.joining(",") ) + " = "
//                                       + bestCode + "\n");
//      return bestCode;
//   }


   public String getTextCode( final String uri ) {
      return String.join( ";", getTextCodes( uri ) );
   }


   public Collection<String> getTextCodes( final String uri ) {
      final Collection<String> codes = new HashSet<>();
      final Collection<String> allTableCodes = TOPO_MAJOR_MAP_FULL.get( uri );
      if ( allTableCodes != null ) {
         allTableCodes.forEach( c -> codes.add( c.substring( 0,3 ) ) );
      }
      final String tableCode = URI_MAJOR_MINOR_MAP.get( uri );
      if ( tableCode != null ) {
         codes.add( tableCode.substring( 0, 3 ) );
      }
      final String ontoCode = Neo4jOntologyConceptUtil.getIcdoTopoCode( uri );
      if ( !ontoCode.isEmpty() && !ontoCode.contains( "-" ) ) {
         codes.add( ontoCode.substring( 0, 3 ) );
      }
      return codes;
   }


   static private void fillTopoMajorMaps() {
      try {
         final File topoMajorFile = FileLocator.getFile( "org/healthnlp/deepphe/icdo/DpheMajorSites.bsv" );
         try ( BufferedReader reader = new BufferedReader( new FileReader( topoMajorFile ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               if ( line.isEmpty() || line.startsWith( "//" ) ) {
                  line = reader.readLine();
                  continue;
               }
               final String[] splits = StringUtil.fastSplit( line, '|' );
               // URI : Code
               URI_MAJOR_MINOR_MAP.put( splits[ 1 ], splits[ 0 ] );
               final String code = splits[ 0 ];
               final String uri = splits[ 1 ];
               TOPO_MAJOR_MAP_FULL.computeIfAbsent( uri, c -> new HashSet<>() ).add( code );
               Neo4jOntologyConceptUtil.getBranchUris( uri )
                                       .forEach( u -> TOPO_MAJOR_MAP_FULL
                                             .computeIfAbsent( uri, c -> new HashSet<>() ).add( code ) );
               line = reader.readLine();
            }
         }
      } catch ( IOException ioE ) {
         Logger.getLogger( "TopoMajorNormalizer" ).error( ioE.getMessage() );
      }
   }

   static private final Collection<String> UNSPECIFIC_CODES = new HashSet<>( Arrays.asList(
         UNDETERMINED, ILL_DEFINED, SKIN, BODY_TISSUE ) );

   static private boolean isSpecificCode( final String code ) {
      return !UNSPECIFIC_CODES.contains( code );
   }

   static private boolean haveSpecificCodes( final Map<String,Double> confidenceMap ) {
       return confidenceMap.keySet().stream().anyMatch( TopoMajorNormalizer::isSpecificCode );
   }

   static private Map<String,Double> getSpecificCodes( final Map<String,Double> confidenceMap ) {
       confidenceMap.keySet().removeAll( UNSPECIFIC_CODES );
       return confidenceMap;
   }

}
