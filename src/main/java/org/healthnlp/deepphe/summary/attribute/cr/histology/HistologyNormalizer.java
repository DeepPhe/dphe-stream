package org.healthnlp.deepphe.summary.attribute.cr.histology;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.TopoMorphValidator;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class HistologyNormalizer extends AbstractAttributeNormalizer {

   private Map<String,Integer> _uriStrengths;

   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      // HistologyInfoCollector.getAggregates includes neoplasm aggregate.
      _uriStrengths = getAttributeUriStrengths( infoCollector );
      NeoplasmSummaryCreator.addDebug( "Histology = " + infoCollector.getBestAggregates()
                                                                     .stream()
                                                                     .map( CrConceptAggregate::getUri )
                                                                     .collect( Collectors.joining(",") ) +"\n");
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "Histology best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getCode( final String uri ) {
      return getHistologyCode( uri, _uriStrengths );
   }

   protected void fillEvidenceMap( final AttributeInfoCollector infoCollector,
                                   final Map<String,String> dependencies ) {
      useAllEvidenceMap( infoCollector, dependencies );
   }

   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      final Map<String,Long> codeCountMap = createCodeCountMap( aggregates );
      String bestCode = "";
      long bestCodesCount = 0;
      for ( Map.Entry<String,Long> codeCount : codeCountMap.entrySet() ) {
         final long count = codeCount.getValue();
         if ( getInt( codeCount.getKey() ) > getInt( bestCode ) ) {
            bestCode = codeCount.getKey();
            bestCodesCount = count;
         }
      }
      setBestCodesCount( (int)bestCodesCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( codeCountMap.size() );
      NeoplasmSummaryCreator.addDebug( "HistologyNormalizer "
                                       + codeCountMap.entrySet().stream()
                                                 .map( e -> e.getKey() + ":" + e.getValue() )
                                                 .collect( Collectors.joining(",") ) + " = "
                                       + bestCode +"\n");
      return bestCode;
   }



   static private String getHistologyCode( final String uri,
                                        final Map<String,Integer> uriStrengths ) {
      final List<String> ontoMorphCodeList = getUriOntoMorphCodes( uri );
      final List<String> broadMorphCodeList = getUriBroadMorphCodes( uri );
      final List<String> exactMorphCodeList = getUriExactMorphCodes( uri );

      final String bestMorphCode = getBestMorphCode( ontoMorphCodeList,
                                                     broadMorphCodeList,
                                                     exactMorphCodeList,
                                                     uriStrengths );
      if ( !bestMorphCode.isEmpty() ) {
         return bestMorphCode.substring( 0, 4 );
      }
      final Collection<String> ontoMorphCodes = new HashSet<>( ontoMorphCodeList );
      final Collection<String> broadMorphCodes = new HashSet<>( broadMorphCodeList );
      final Collection<String> exactMorphCodes = new HashSet<>( exactMorphCodeList );
      final List<String> sortedMorphCodes = getSortedMorphCodes( ontoMorphCodes,
                                                                 broadMorphCodes,
                                                                 exactMorphCodes );
      return getBestHistology( sortedMorphCodes );
   }

   static protected List<String> getOntoMorphCodes( final String uri ) {
      return Neo4jOntologyConceptUtil.getIcdoCodes( uri ).stream()
                                     .filter( i -> !i.startsWith( "C" ) )
                                     .filter( i -> !i.contains( "-" ) )
                                     .filter( i -> i.length() > 3 )
                                     .distinct()
                                     .sorted()
                                     .collect( Collectors.toList() );
   }


   static private List<String> getUriOntoMorphCodes( final String uri ) {
      return getOntoMorphCodes( uri )
            .stream()
            .filter( m -> !m.startsWith( "800" ) )
            .filter( m -> !m.isEmpty() )
            .distinct()
            .sorted()
            .collect( Collectors.toList() );
   }

   // TODO - TopoMorphValidator.getInstance().getBroadHistoCode( uri )
   // TODO   always returns an empty list.
   static private List<String> getUriBroadMorphCodes( final String uri ) {
      return Collections.emptyList();
//         return TopoMorphValidator.getInstance().getBroadHistoCode( uri )
//                                 .stream()
//                                 .filter( m -> !m.startsWith( "800" ) )
//                                 .filter( m -> !m.isEmpty() )
//                                 .distinct()
//                                 .sorted()
//                                 .map( c -> c + "/3" )
//                                 .collect( Collectors.toList() );
   }

   static private List<String> getUriExactMorphCodes( final String uri ) {
      final String exactCode = TopoMorphValidator.getInstance().getExactMorphCode( uri );
      if ( exactCode.isEmpty() || exactCode.startsWith( "800" ) ) {
         return Collections.emptyList();
      }
      return Collections.singletonList( exactCode );
   }


   static private List<String> getSortedMorphCodes( final Collection<String> uriOntoMorphCodes,
                                                    final Collection<String> uriBroadMorphCodes,
                                                    final Collection<String> uriExactMorphCodes ) {
      final Collection<String> allCodes = new HashSet<>( uriOntoMorphCodes );
      allCodes.addAll( uriBroadMorphCodes );
      allCodes.addAll( uriExactMorphCodes );
      trimMorphCodes( allCodes );
      final List<String> codes = new ArrayList<>( allCodes );
      codes.sort( HISTO_COMPARATOR );
      return codes;
   }

   static private String getBestMorphCode( final List<String> uriOntoMorphCodes,
                                           final List<String> uriBroadMorphCodes,
                                           final List<String> uriExactMorphCodes,
                                           final Map<String,Integer> uriStrengths ) {
      final Map<Integer,Collection<String>> hitCounts = getHitCounts( uriOntoMorphCodes,
                                                                      uriBroadMorphCodes,
                                                                      uriExactMorphCodes,
                                                                      uriStrengths );
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

   static private Map<Integer,Collection<String>> getHitCounts( final List<String> uriOntoMorphCodeList,
                                                                final List<String> uriBroadMorphCodeList,
                                                                final List<String> uriExactMorphCodeList,
                                                                final Map<String,Integer> uriStrengths ) {
      final Collection<String> allUris = new HashSet<>( uriStrengths.keySet() );
      final Map<String,Integer> ontoStrengths = getUrisOntoMorphCodeStrengthMap( allUris, uriOntoMorphCodeList,
                                                                                 uriStrengths );
      final Map<String,Integer> broadStrengths = getUrisBroadMorphCodeStrengthMap( allUris, uriBroadMorphCodeList,
                                                                                   uriStrengths );
      final Map<String,Integer> exactStrengths = getUrisExactMorphCodeStrengthMap( allUris, uriExactMorphCodeList,
                                                                                   uriStrengths );
      final Collection<String> allCodes = new HashSet<>( ontoStrengths.keySet() );
      allCodes.addAll( exactStrengths.keySet() );
      allCodes.addAll( broadStrengths.keySet() );
      trimMorphCodes( allCodes );
      final Map<String,Integer> multiCodesMap = getMultiHistoCodeMap( allCodes );

      final Map<Integer,Collection<String>> hitCounts = new HashMap<>();
      for ( String code : allCodes ) {
         int count = 0;
         count += ontoStrengths.getOrDefault( code, 0 );
         count += exactStrengths.getOrDefault( code, 0 );
         count += broadStrengths.getOrDefault( code, 0 );
//         final int broadCount = broadStrengths.getOrDefault( code, 0 );
//         if ( count > 0 ) {
//            // Try to prevent a less 'strong' uri code from overpowering a stronger uri code just because it appears
//            // in broad
//            count += broadCount / 2;
//         } else {
//            count += broadCount;
//         }
         count += multiCodesMap.getOrDefault( code, 0 );
         if ( code.startsWith( "801" ) ) {
            count = 1;
         }
         hitCounts.computeIfAbsent( count, c -> new HashSet<>() ).add( code );
      }

      ontoStrengths.keySet().retainAll( allCodes );
      exactStrengths.keySet().retainAll( allCodes );
      broadStrengths.keySet().retainAll( allCodes );
      NeoplasmSummaryCreator.addDebug( "\n    Hit Counts: " + hitCounts.entrySet().stream()
                                                                       .map( e -> e.getKey() +" " + e.getValue() )
                                                                       .collect(  Collectors.joining(",") ) + "\n" );
      return hitCounts;
   }


   static private Map<String,Integer> getUrisOntoMorphCodeStrengthMap( final Collection<String> uris,
                                                                       final List<String> uriOntoMorphCodeList,
                                                                       final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
//         final Collection<String> codes = getOntoMorphCodes( uri );
         NeoplasmSummaryCreator.addDebug( uriOntoMorphCodeList.isEmpty()
                                          ? "" : ("  Onto " + uri + " " + uriOntoMorphCodeList + "\n") );
         for ( String code : uriOntoMorphCodeList ) {
//            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
//            ontoMorphStrengths.put( code, previousStrength + strength );
            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
         }
      }
      return ontoMorphStrengths;
   }


   static private Map<String,Integer> getUrisBroadMorphCodeStrengthMap( final Collection<String> uris,
                                                                        final List<String> uriBroadMorphCodeList,
                                                                        final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
//         final Collection<String> codes = TopoMorphValidator.getInstance().getBroadMorphCode( uri );
         NeoplasmSummaryCreator.addDebug( uriBroadMorphCodeList.isEmpty()
                                          ? "" : ("  Broad " + uri + " " + uriBroadMorphCodeList + "\n") );
         for ( String code : uriBroadMorphCodeList ) {
//            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
//            ontoMorphStrengths.put( code, previousStrength + strength );
            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
         }
      }
      return ontoMorphStrengths;
   }


   static private Map<String,Integer> getUrisExactMorphCodeStrengthMap( final Collection<String> uris,
                                                                        final List<String> uriExactMorphCodeList,
                                                                        final Map<String,Integer> uriStrengths ) {
      final Map<String,Integer> ontoMorphStrengths = new HashMap<>();
      for ( String uri : uris ) {
         final int strength = uriStrengths.get( uri );
//         final String code = TopoMorphValidator.getInstance().getExactMorphCode( uri );
         NeoplasmSummaryCreator.addDebug( uriExactMorphCodeList.isEmpty()
                                          ? "" : ("  Exact " + uri + " " + uriExactMorphCodeList + "\n") );
         for ( String code : uriExactMorphCodeList ) {
//            final int previousStrength = ontoMorphStrengths.getOrDefault( code, 0 );
//            ontoMorphStrengths.put( code, previousStrength + strength );
            ontoMorphStrengths.compute( code, (k, v) -> (v == null) ? strength : v+strength );
         }
      }
      return ontoMorphStrengths;
   }


   static private Map<String,Integer> getMultiHistoCodeMap( final Collection<String> codes ) {
      final Map<String,Collection<String>> histoMorphMap = new HashMap<>();
      for ( String code : codes ) {
         histoMorphMap.computeIfAbsent( code.substring( 0,4 ), t -> new ArrayList<>() ).add( code );
      }
      return codes.stream()
                  .collect( Collectors.toMap( Function.identity(),
                                              c -> histoMorphMap.get( c.substring( 0,4 ) ).size() ) );
   }

   static private void trimMorphCodes( final Collection<String> morphs  ) {
      morphs.remove( "" );
      final Collection<String> removals = morphs.stream()
                                                .filter( m -> m.startsWith( "800" ) )
                                                .collect( Collectors.toSet() );
      morphs.removeAll( removals );
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

   static private final HistoComparator
         HISTO_COMPARATOR = new HistoComparator();

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
         if ( sub1.equals( "801" ) ) {
            return -1;
         } else if ( sub2.equals( "801" ) ) {
            return 1;
         }
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



   private Map<String,Integer> getAttributeUriStrengths( final AttributeInfoCollector attributeInfoCollector ) {
      final Collection<String> exactUris = attributeInfoCollector.getBestUris();
      final Collection<CrConceptAggregate> aggregates = attributeInfoCollector.getBestAggregates();
      // Switched from getAllUris to uris for affirmed mentions 3/23/2021
      final Collection<Mention> allMentions = aggregates.stream()
                                                        .map( CrConceptAggregate::getMentions )
                                                        .flatMap( Collection::stream )
                                                        .filter( m -> ( !m.isNegated()
                                                                        || exactUris.contains( m.getClassUri() ) ) )
                                                        .collect( Collectors.toSet() );
      final Collection<String> allUris = allMentions.stream()
                                                    .map( Mention::getClassUri )
                                                    .collect( Collectors.toSet() );
      final Map<String,Collection<String>> allUriRoots = new HashMap<>();
      for ( CrConceptAggregate attribute : aggregates ) {
         allUriRoots.putAll( attribute.getUriRootsMap() );
      }
      final List<KeyValue<String, Double>> uriQuotients = UriScoreUtil.mapUriQuotients( allUris,
                                                                                        allUriRoots,
                                                                                        allMentions );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         int strength = (int)Math.ceil( quotients.getValue() * 100 );
         if ( exactUris.contains( quotients.getKey() ) ) {
            strength += 25;
         }
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
//      UriInfoVisitor.applySectionAttributeUriStrengths( aggregates, uriStrengths );
//      UriInfoVisitor.applyHistoryAttributeUriStrengths( aggregates, uriStrengths );
      return uriStrengths;
   }


   static private int getInt( final String value ) {
      try {
         return Integer.parseInt( value );
      } catch ( NumberFormatException nfE ) {
         return 0;
      }
   }


}
