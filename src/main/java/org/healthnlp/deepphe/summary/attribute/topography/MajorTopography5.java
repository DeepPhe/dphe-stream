package org.healthnlp.deepphe.summary.attribute.topography;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.EvidenceLevel.*;
import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.divisionInt0to10;

@Deprecated
final public class MajorTopography5 implements SpecificAttribute {

   static private final Logger LOGGER = Logger.getLogger( "MajorTopography5" );
   //  According to Chen, the best features from the previous run were:
   // [9, 24, 29, 32, 33, 40, 41, 43, 46, 47, 49, 51, 52, 53, 55, 58]  - actual row.
   // =4, 19, 24, 27, 28, 35, 36, 38, 41, 42, 44, 46, 47, 48, 50, 53   - feature number.


   private String _bestSiteUri = "";
   private Collection<String> _firstSiteUris;
   private Collection<String> _topographyCodes;
   final private NeoplasmAttribute _neoplasmAttribute;


   public MajorTopography5( final ConceptAggregate neoplasm,
                            final Collection<ConceptAggregate> allConcepts ) {
      _neoplasmAttribute = createMajorTopoAttribute( neoplasm, allConcepts );
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _neoplasmAttribute;
   }

   public String getMajorSiteUri() {
      return _bestSiteUri;
   }

   public Collection<String> getFirstSiteUris() {
      return _firstSiteUris;
   }

   public Collection<String> getTopographyCodes() {
      return _topographyCodes;
   }

   static private String getMajorTopoCode( final Collection<String> topographyCodes ) {
      if ( topographyCodes.isEmpty() ) {
//         LOGGER.info( "No sites, using C80." );
         return "C80";
      }
      final Function<String, String> getMajorCode = t -> {
         final int dot = t.indexOf( '.' );
         return dot > 0 ? t.substring( 0, dot ) : t;
      };
      return topographyCodes.stream()
                           .map( getMajorCode )
                           .distinct()
                           .sorted()
                           .collect( Collectors.joining( ";" ) );
   }

   private NeoplasmAttribute createMajorTopoAttribute( final ConceptAggregate neoplasm,
                                                     final Collection<ConceptAggregate> allConcepts ) {
      _firstSiteUris = getFirstSiteUris( neoplasm );
      _topographyCodes = _firstSiteUris.stream()
                                        .map( Neo4jOntologyConceptUtil::getIcdoTopoCode )
                                        .filter( t -> !t.isEmpty() )
                                        .collect( Collectors.toSet() );

      final Collection<ConceptAggregate> siteConcepts = neoplasm.getRelatedSites();
      final List<ConceptAggregate> allPatientSites = listPatientSiteConcepts( allConcepts );
      final Map<EvidenceLevel,Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( getFirstSites( neoplasm ), siteConcepts, allPatientSites );
      final List<Mention> directEvidence = new ArrayList<>( evidence.getOrDefault( DIRECT_EVIDENCE,
                                                                                   Collections.emptyList() ) );

      final List<Integer> features = createFeatures( neoplasm, _firstSiteUris, directEvidence, allConcepts );

      return SpecificAttribute.createAttribute( "topography_major",
                                                getMajorTopoCode( _topographyCodes ),
                                                directEvidence,
                                                new ArrayList<>( evidence.getOrDefault( INDIRECT_EVIDENCE,
                                                                                        Collections.emptyList() ) ),
                                                new ArrayList<>( evidence.getOrDefault( NOT_EVIDENCE,
                                                                                        Collections.emptyList() ) ),
                                                features );
   }

//   static private Map<EvidenceLevel,Collection<Mention>> mapEvidence( final Collection<String> firstSiteUris,
//                                                                final Collection<ConceptAggregate> siteConcepts,
//                                                                      final Collection<ConceptAggregate> allSiteConcepts ) {
//      final Map<EvidenceLevel,Collection<Mention>> evidenceMap = new HashMap<>();
//
//      final Function<ConceptAggregate,KeyValue<EvidenceLevel,Collection<Mention>>> splitMentions = c ->
//         firstSiteUris.contains( c.getUri() )
//         ? new KeyValue<>( DIRECT_EVIDENCE, c.getMentions() )
//         : new KeyValue<>( INDIRECT_EVIDENCE, c.getMentions() );
//
//      final Consumer<KeyValue<EvidenceLevel,Collection<Mention>>> placeMentions = kv ->
//            evidenceMap.computeIfAbsent( kv.getKey(), v -> new HashSet<>() ).addAll( kv.getValue() );
//
//      siteConcepts.stream()
//                  .map( splitMentions )
//                  .forEach( placeMentions );
//      return evidenceMap;
//   }

//   static private List<Mention> getDirectEvidence( final Collection<String> firstSiteUris,
//                                                   final Collection<ConceptAggregate> siteConcepts ) {
//      return siteConcepts.stream()
//                         .filter( c -> firstSiteUris.contains( c.getUri() ) )
//                         .map( ConceptAggregate::getMentions )
//                         .flatMap( Collection::stream )
//                         .collect( Collectors.toList() );
//   }
//
//   static private List<Mention> getIndirectEvidence( final Collection<ConceptAggregate> siteConcepts,
//                                                     final List<Mention> directEvidence ) {
//      final List<Mention> indirectEvidence = siteConcepts.stream()
//                                                         .map( ConceptAggregate::getMentions )
//                                                         .flatMap( Collection::stream )
//                                                         .collect( Collectors.toList() );
//      indirectEvidence.removeAll( directEvidence );
//      return indirectEvidence;
//   }






   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                          final Collection<String> firstSiteUris,
                                          final Collection<Mention> directEvidence,
                                          final Collection<ConceptAggregate> allConcepts ) {
      final List<Integer> features = new ArrayList<>( 5 );

      final List<Mention> allPatientSites = listPatientSiteMentions( allConcepts );
      final Collection<String> allSiteUris = allPatientSites.stream()
                                                            .map( Mention::getClassUri )
                                                            .collect( Collectors.toSet() );
      final Map<String,Collection<String>> allSitesRootsMap = UriUtil.mapUriRoots( allSiteUris );

      final Map<String,Collection<String>> firstSitesRootsMap = new HashMap<>( firstSiteUris.size() );
      firstSiteUris.forEach( u -> firstSitesRootsMap.put( u, allSitesRootsMap.getOrDefault( u, Collections.emptyList() ) ) );

      final Map<String,Integer> uriCountsMap = UriScoreUtil.mapUriMentionCounts( firstSiteUris, directEvidence );
      final Map<String,Integer> uriSums = UriScoreUtil.mapUriSums( firstSiteUris, firstSitesRootsMap, uriCountsMap );
      final int totalCounts = uriCountsMap.values().stream().mapToInt( i -> i ).sum();
      final Map<String,Double> uriQuotientMap = UriScoreUtil.mapUriQuotientsBB( uriSums, totalCounts );
      final List<KeyValue<String,Double>> uriQuotientList = UriScoreUtil.listUriQuotients( uriQuotientMap );
      final List<KeyValue<String,Double>> bestKeyValues = UriScoreUtil.getBestUriScores( uriQuotientList );
      final Map<String,Integer> classLevelMap = UriScoreUtil.createClassLevelMap( bestKeyValues );

      _bestSiteUri = UriScoreUtil.getBestUriScore( bestKeyValues, classLevelMap, firstSitesRootsMap ).getKey();

      //1. Number of exact site class mentions. Normalized over total site class mentions.  Rounded to 0-10.
      final int exactSiteMentionCount = uriCountsMap.getOrDefault( _bestSiteUri, 0 );
      LOGGER.info( "1. " + _bestSiteUri + "=" + exactSiteMentionCount + " "
                   + allPatientSites.stream().map( Mention::getClassUri ).collect( Collectors.joining( "," ) ) );
      features.add( getPrimaryToPatientMentions( exactSiteMentionCount, allPatientSites.size() ) );

      final Map<String,Integer> allSitesUriCountsMap = UriScoreUtil.mapUriMentionCounts( allSiteUris, allPatientSites );
      final Map<String,Integer> bestSitesUriSums = UriScoreUtil.mapBestUriSums( allSiteUris, allSitesRootsMap,
                                                                        allSitesUriCountsMap );
      final int totalSitesUriSums = bestSitesUriSums.values().stream().mapToInt( i -> i ).sum();
      LOGGER.info( "2. " +_bestSiteUri + "=" + uriSums.getOrDefault( _bestSiteUri, 0 ) + " "
                   + bestSitesUriSums.entrySet().stream().map( e -> e.getKey() + "=" + e.getValue() ).collect( Collectors.joining( "," ) ) );
      features.add( createFeature2( uriSums.getOrDefault( _bestSiteUri, 0 ), totalSitesUriSums ) );

      LOGGER.info( "3. " +_bestSiteUri + "=" + classLevelMap.getOrDefault( _bestSiteUri, 0 ) + " "
                   + classLevelMap.entrySet().stream().map( e -> e.getKey() + "=" + e.getValue() ).collect( Collectors.joining( "," ) ) );
      features.add( createFeature3( _bestSiteUri, classLevelMap ) );

      final Map<String,Integer> uriRelationCounts = mapSiteUriCounts( neoplasm );
      final int bestUriRelationCount = uriRelationCounts.getOrDefault( _bestSiteUri, 0 );
      LOGGER.info( "4. " +_bestSiteUri + "=" + bestUriRelationCount + " sum " + uriRelationCounts.values().stream().mapToInt( l -> l ).sum() + " "
                   + uriRelationCounts.entrySet().stream().map( e -> e.getKey() + "=" + e.getValue() ).collect( Collectors.joining( "," ) ) );
      features.add( createFeature4( bestUriRelationCount, uriRelationCounts ) );

      LOGGER.info( "5. " +_bestSiteUri + "=" + bestUriRelationCount + " max " + uriRelationCounts.values()
                                                                                                 .stream()
                                                                                                 .max( Integer::compareTo )
                                                                                                 .orElse( Integer.MAX_VALUE ) + " "
                   + uriRelationCounts.entrySet().stream().map( e -> e.getKey() + "=" + e.getValue() ).collect( Collectors.joining( "," ) ) );
      features.add( createFeature5( bestUriRelationCount, uriRelationCounts ) );

      return features;
   }

   static private List<ConceptAggregate> listPatientSiteConcepts( final Collection<ConceptAggregate> allConcepts ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      return allConcepts.stream()
                        .filter( c -> UriConstants.getLocationUris( graphDb ).contains( c.getUri() ) )
                        .collect( Collectors.toList() );
   }

   static private List<Mention> listPatientSiteMentions( final Collection<ConceptAggregate> allConcepts ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      return allConcepts.stream()
                        .filter( c -> UriConstants.getLocationUris( graphDb ).contains( c.getUri() ) )
                        .map( ConceptAggregate::getMentions )
                        .flatMap( Collection::stream )
                        .collect( Collectors.toList() );
   }

   static private int countExactSiteMentions( final String bestSiteUri,
                                              final List<Mention> directEvidence ) {
      return (int)directEvidence.stream()
                          .filter( m -> bestSiteUri.equals( m.getClassUri() ) )
                          .count();
   }




   //                The following features are neoplasm-independent.
   //                The following features involve site class and branch prevalence.

   /**
    * Number of exact site class mentions. Normalized over total site class mentions.
    * For instance, forearm (2) vs. abdomen (3) vs. finger (3) ... vs. upper_limb (4) vs. trunk (1).
    * Here the top exact class is upper_limb (4/13).
    * @param primaryMentionCount number of site mentions for the neoplasm that have exactly the best site uri.
    * @param patientMentionCount number of site mentions for the entire patient.
    * @return integer 1-10 representing ratio of neoplasm exact sites to all patient sites.
    */
   static private int getPrimaryToPatientMentions( final int primaryMentionCount,
                                                   final int patientMentionCount ) {
      // Chen, 1/19/2021  Correlation ranked #4 of 5
      return divisionInt0to10( primaryMentionCount, patientMentionCount );
   }

   /**
    * Number of site class branch mentions.  Normalized over total site class mentions for patient.
    * For instance, [forearm (2), upper_limb (4)]=(6) vs. [abdomen (3), trunk (1)]=(4) vs. [finger (3)]=(3).
    * Here the top class by branch is forearm (6/13).
    * Another reason to normalize over the total site mentions:
    * Consider 1 note with 20 site mentions, most populous = (17) vs. 5 notes (or 1 long note)
    * with 100 total site mentions, most populous = (37).
    * The ratios 17/20 vs. 37/100 are very different from the absolute 17 vs. 37.
    * Keep in mind that branches can overlap.
    * For instance, [forearm, upper_limb] (6) vs. [arm, upper_limb] (5) where upper_limb is common with 4 mentions.
    * @param exactSiteBranchMentionCount -
    * @param patientSiteBranchMentionCount -
    * @return -
    */
   static private int createFeature2( final int exactSiteBranchMentionCount,
                                      final int patientSiteBranchMentionCount ) {
      // Chen, 1/19/2021  Correlation ranked #5 of 5
      return divisionInt0to10( exactSiteBranchMentionCount, patientSiteBranchMentionCount );
   }


   //                The following feature involves class precision.
   //                - A precise class is more likely to be correct than an imprecise class.

   /**
    * Distance of exact site class from root.  Normalize over the furthest distance class.
    * For instance forearm {3} vs. abdomen {3} vs. finger {3}.
    * - Keep in mind that because of speed constraints we are
    * not walking the ontology through PART_OF (etc.) relations.  (finger P_O hand P_O upper_limb).
     * @return -
    */
   static private int createFeature3( final String bestUri, final Map<String,Integer> classLevelMap ) {

      // TODO --- This appears to be broken, always returning a value of 10.   Need to output some debug for all
      //  features.
      // Chen, 1/19/2021  Correlation ranked #2 of 5
      final int max = classLevelMap.values()
                                   .stream()
                                   .max( Integer::compareTo )
                                   .orElse( Integer.MAX_VALUE );
      return divisionInt0to10( classLevelMap.getOrDefault( bestUri, 0 ), max );
   }


   //                The following are neoplasm-dependent.  They account for relations.

   /**
    * Number of "HAS_SITE" relations between best site and neoplasm.
    * Normalized over the total "HAS_SITE" relation count for the neoplasm.
    * Consider that multiple neoplasm mentions can have the same site mention.
    * forearm <2>, abdomen <0>, finger <5>.  So, the top score would be finger <5/7>
    * @param bestUriRelationCounts -
    * @param uriRelationCounts -
    * @return -
    */
   static private int createFeature4( final int bestUriRelationCounts, final Map<String,Integer> uriRelationCounts ) {
      // Chen, 1/19/2021  Correlation ranked #1 of 5
      final int sum = uriRelationCounts.values().stream().mapToInt( l -> l ).sum();
      return divisionInt0to10( bestUriRelationCounts, sum );
   }


   /**
    * We now have most mentioned class upper_limb (4/13), most mentioned branch class forearm (6/13),
    * most 'precise' class tied at {3} and most related class branch finger <5>.
    * Notice that finger <5> relations outnumber branch mentions of finger (3).
    * The site of the neoplasm is (largely) determined by #5:
    * the number of times neoplasm branch mentions are related to site branch mentions.
    * This takes into account the different "HAS_SITE" type relations.
    *
    * Next nearest site related to neoplasm, normalized by winning site.  In this case forearm <2/5>.
    * We could normalize to the total relations <2/7>, but consider relations <3>,<2>,<2>.
    * I think that the score <2/3> is better than <2/7>.
    * @param bestUriRelationCounts -
    * @param uriRelationCounts -
    * @return -
    */
   static private int createFeature5( final int bestUriRelationCounts, final Map<String,Integer> uriRelationCounts ) {
      // Chen, 1/19/2021  Correlation ranked #3 of 5
      final int max = uriRelationCounts.values()
                                   .stream()
                                   .max( Integer::compareTo )
                                   .orElse( Integer.MAX_VALUE );
      return divisionInt0to10( bestUriRelationCounts, max );
   }




   static private Collection<ConceptAggregate> getFirstSites( final ConceptAggregate neoplasm ) {
      return getFirstRelatedConcepts( neoplasm,
                                  DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                                  DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
                                  DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
                                  Disease_Has_Associated_Region,
                                  Disease_Has_Associated_Cavity );
   }

   static private Collection<ConceptAggregate> getFirstRelatedConcepts( final ConceptAggregate conceptAggregate,
                                                              final String... relations ) {
      for ( String relation : relations ) {
         final Collection<ConceptAggregate> relatedSites = conceptAggregate.getRelated( relation );
          if ( relatedSites != null && !relatedSites.isEmpty() ) {
            return relatedSites;
         }
      }
      return Collections.emptyList();
   }




   static private Collection<String> getFirstSiteUris( final ConceptAggregate neoplasm ) {
      return getFirstRelatedUris( neoplasm,
                                  DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                                  DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
                                  DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
                                  Disease_Has_Associated_Region,
                                  Disease_Has_Associated_Cavity );
   }

   static private Collection<String> getFirstRelatedUris( final ConceptAggregate conceptAggregate,
                                                          final String... relations ) {
//      LOGGER.info( "Choosing best site for neoplasm based upon relation hierarchy." );
      for ( String relation : relations ) {
//         LOGGER.info( "   " + relation );
         final Collection<String> relatedUris = conceptAggregate.getRelatedUris( relation );
         if ( relatedUris != null && !relatedUris.isEmpty() ) {
//            LOGGER.info( "      " + String.join( ",", relatedUris ) );
            return relatedUris;
         }
      }
      return Collections.emptyList();
   }


   static private Map<String,Integer> mapSiteUriCounts( final ConceptAggregate neoplasm ) {
      return mapSiteUriCounts( neoplasm,
                               DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                               DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
                               DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
                               Disease_Has_Associated_Region,
                               Disease_Has_Associated_Cavity );
   }

   static private Map<String,Integer> mapSiteUriCounts( final ConceptAggregate conceptAggregate,
                                                          final String... relations ) {
//      LOGGER.info( "Choosing best site for neoplasm based upon relation hierarchy." );
      final Map<String,Integer> uriCounts = new HashMap<>();
      for ( String relation : relations ) {
//         LOGGER.info( "   " + relation );
         final Map<String,List<ConceptAggregate>> uriConceptMap = conceptAggregate
               .getRelated( relation )
               .stream()
               .collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
         uriConceptMap.forEach( (k,v) -> uriCounts.put( k, uriCounts.getOrDefault( k, 0 ) + v.size() ) );
      }
      return uriCounts;
   }



}
