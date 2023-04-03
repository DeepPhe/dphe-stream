package org.healthnlp.deepphe.summary.attribute.cr.topo_major;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/25/2023}
 */
public class TopoMajorNormalizer extends AbstractAttributeNormalizer {


   static private final Map<String,String> TOPO_MAJOR_MAP = new HashMap<>();
   static private final Map<String,Collection<String>> TOPO_MAJOR_MAP_FULL = new HashMap<>();


   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      if ( TOPO_MAJOR_MAP.isEmpty() ) {
         fillTopoMajorMaps();
      }
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "TopoMajor best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      final ConfidenceGroup<CrConceptAggregate> confidenceGroup = new ConfidenceGroup<>( aggregates );
      final Map<String,Long> countMap = confidenceGroup.getBest()
                                                           .stream()
                                                            .map( CrConceptAggregate::getUri )
                                                           .map( this::getCodes )
                                                           .flatMap( Collection::stream )
                                                           .map( c -> c.substring( 0,3 ) )
                                                           .collect( Collectors.groupingBy( Function.identity(),
                                                                                            Collectors.counting() ) );
      final List<String> codeList = new ArrayList<>( countMap.keySet() );
      codeList.sort( Comparator.reverseOrder() );
      final String bestCode = codeList.get( 0 );
      if ( bestCode.isEmpty() ) {
         return "";
      }
      long bestCount = countMap.get( bestCode );
      setBestCodesCount( (int)bestCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( countMap.size() );
      NeoplasmSummaryCreator.addDebug( "TopoMajorNormalizer "
                                       + countMap.entrySet().stream()
                                                 .map( e -> e.getKey() + ":" + e.getValue() )
                                                 .collect( Collectors.joining(",") ) + " = "
                                       + bestCode + "\n");
      return bestCode;
   }

   public String getCode( final String uri ) {
      return String.join( ";", getCodes( uri ) );
   }

   public List<String> getCodes( final String uri ) {
      final List<String> codes = new ArrayList<>();
      final Collection<String> allTableCodes = TOPO_MAJOR_MAP_FULL.get( uri );
      if ( allTableCodes != null ) {
         codes.forEach( c -> allTableCodes.add( c.substring( 0,3 ) ) );
//         codes.addAll( allTableCodes );
      }
      final String tableCode = TOPO_MAJOR_MAP.get( uri );
      if ( tableCode != null ) {
//         codes.add( tableCode );
         codes.add( tableCode.substring( 0, 3 ) );
      }
      final String ontoCode = Neo4jOntologyConceptUtil.getIcdoTopoCode( uri );
      if ( !ontoCode.isEmpty() && !ontoCode.contains( "-" ) ) {
//         codes.add( ontoCode );
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
               TOPO_MAJOR_MAP.put( splits[ 1 ], splits[ 0 ] );
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


}
