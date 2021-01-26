package org.healthnlp.deepphe.summary.attribute.grade;

import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.EvidenceLevel.*;
import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addIntFeatures;

final public class Grade implements SpecificAttribute {

   final private Map<Integer,List<ConceptAggregate>> _gradeCodeMap = new HashMap<>();
   private int _bestGradeCode = -1;
   private String _bestGradeUri = "";
   final private NeoplasmAttribute _neoplasmAttribute;

   public Grade( final ConceptAggregate neoplasm,
                 final Collection<ConceptAggregate> allConcepts ) {
      _neoplasmAttribute = createGradeAttribute( neoplasm, allConcepts );
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _neoplasmAttribute;
   }

   private NeoplasmAttribute createGradeAttribute( final ConceptAggregate neoplasm,
                                                    final Collection<ConceptAggregate> allConcepts ) {
      final List<Mention> allPatientGrades = getPatientGradeMentions( allConcepts );
      _gradeCodeMap.putAll( getGradeConcepts( neoplasm ) );
      if ( _gradeCodeMap.isEmpty() ) {
         return createDefaultGrade( allPatientGrades );
      }
      _bestGradeCode = _gradeCodeMap.keySet()
                                   .stream()
                                   .max( Integer::compareTo )
                                   .orElse( -1 );
      if ( _bestGradeCode < 0 ) {
         return createDefaultGrade( allPatientGrades );
      }
      final Collection<ConceptAggregate> neoplasmGrades = _gradeCodeMap.values()
                                                                       .stream()
                                                                       .flatMap( Collection::stream )
                                                                       .collect( Collectors.toSet() );
      final Map<EvidenceLevel,Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( _gradeCodeMap.get( _bestGradeCode ),
                                             neoplasmGrades,
                                             getPatientGradeConcepts( allConcepts ) );
      final List<Mention> directEvidence = new ArrayList<>( evidence.getOrDefault( DIRECT_EVIDENCE,
                                                                                   Collections.emptyList() ) );



      final List<Integer> features = createFeatures( neoplasm, evidence, allConcepts );

      return SpecificAttribute.createAttribute( "grade",
                                                _bestGradeCode +"",
                                                directEvidence,
                                                new ArrayList<>( evidence.getOrDefault( INDIRECT_EVIDENCE,
                                                                                        Collections.emptyList() ) ),
                                                new ArrayList<>( evidence.getOrDefault( NOT_EVIDENCE,
                                                                                        Collections.emptyList() ) ),
                                                features );
   }







   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                         final Map<EvidenceLevel,Collection<Mention>> evidence,
                                         final Collection<ConceptAggregate> allConcepts ) {
      final List<Integer> features = new ArrayList<>( 5 );
      final Collection<Mention> directEvidence = evidence.get( DIRECT_EVIDENCE );
      final Collection<String> firstSiteUris = directEvidence.stream()
                                                              .map( Mention::getClassUri )
                                                              .collect( Collectors.toSet() );
      final List<Mention> allPatientGrades = new ArrayList<>( evidence.get( NOT_EVIDENCE ) );
      final Collection<String> allGradeUris = allPatientGrades.stream()
                                                            .map( Mention::getClassUri )
                                                            .collect( Collectors.toSet() );
      final Map<String,Collection<String>> allGradesRootsMap = UriUtil.mapUriRoots( allGradeUris );

      final Map<String,Collection<String>> firstGradesRootsMap = new HashMap<>( firstSiteUris.size() );
      firstSiteUris.forEach( u -> firstGradesRootsMap.put( u, allGradesRootsMap.getOrDefault( u, Collections.emptyList() ) ) );

      final Map<String,Integer> uriCountsMap = UriScoreUtil.mapUriMentionCounts( firstSiteUris, directEvidence );
      final Map<String,Integer> uriSums = UriScoreUtil.mapUriSums( firstSiteUris, firstGradesRootsMap, uriCountsMap );
      final int totalCounts = uriCountsMap.values().stream().mapToInt( i -> i ).sum();
      final Map<String,Double> uriQuotientMap = UriScoreUtil.mapUriQuotientsBB( uriSums, totalCounts );
      final List<KeyValue<String,Double>> uriQuotientList = UriScoreUtil.listUriQuotients( uriQuotientMap );
      final List<KeyValue<String,Double>> bestKeyValues = UriScoreUtil.getBestUriScores( uriQuotientList );
      final Map<String,Integer> classLevelMap = UriScoreUtil.createClassLevelMap( bestKeyValues );

      _bestGradeUri = UriScoreUtil.getBestUriScore( bestKeyValues, classLevelMap, firstGradesRootsMap ).getKey();

      final int exactGradeMentionCount = _gradeCodeMap.get( _bestGradeCode )
                                                       .stream()
                                                       .map( ConceptAggregate::getMentions )
                                                      .mapToInt( Collection::size )
                                                       .sum();

      //1. Number of exact grade class mentions. Normalized over total site class mentions.  Rounded to 0-10.
      addIntFeatures( features, exactGradeMentionCount, allGradeUris.size() );

      final Map<String,Integer> allSitesUriCountsMap = UriScoreUtil.mapUriMentionCounts( allGradeUris, allPatientGrades );
      final Map<String,Integer> bestSitesUriSums = UriScoreUtil.mapBestUriSums( allGradeUris, allGradesRootsMap,
                                                                                allSitesUriCountsMap );
//      final int totalSitesUriSums = bestSitesUriSums.values().stream().mapToInt( i -> i ).sum();
//      features.add( createFeature2( uriSums.getOrDefault( _bestGradeUri, 0 ), totalSitesUriSums ) );
//
//      features.add( createFeature3( _bestGradeUri, classLevelMap ) );
//
//      final Map<String,Integer> uriRelationCounts = mapGradeUriCounts( _gradeCodeMap );
//      final int bestUriRelationCount = uriRelationCounts.getOrDefault( _bestGradeUri, 0 );
//      features.add( createFeature4( bestUriRelationCount, uriRelationCounts ) );
//
//      features.add( createFeature5( bestUriRelationCount, uriRelationCounts ) );

      return features;
   }











   static private Map<Integer,List<ConceptAggregate>> getGradeConcepts( final ConceptAggregate neoplasm ) {
      final Collection<ConceptAggregate> gradeConcepts
            = neoplasm.getRelated( HAS_GLEASON_SCORE, DISEASE_IS_GRADE, DISEASE_HAS_FINDING );
      final Map<Integer,List<ConceptAggregate>> gradeMap
            = gradeConcepts.stream()
                           .collect( Collectors.groupingBy( Grade::getGradeNumber ) );
      gradeMap.remove( -1 );
      return gradeMap;
   }

   static private Collection<ConceptAggregate> getPatientGradeConcepts(
         final Collection<ConceptAggregate> allConcepts ) {
      return allConcepts.stream()
                        .filter( c -> getGradeNumber( c ) > 0 )
                        .collect( Collectors.toSet() );
   }

   static private List<Mention> getPatientGradeMentions(
         final Collection<ConceptAggregate> allConcepts ) {
      return allConcepts.stream()
                        .filter( c -> getGradeNumber( c ) > 0 )
                        .map( ConceptAggregate::getMentions )
                        .flatMap( Collection::stream )
                        .distinct()
                        .collect( Collectors.toList() );
   }


   static private int getGradeNumber( final ConceptAggregate grade ) {
      final String uri = grade.getUri();
      if ( uri.startsWith( "Gleason_Score_" ) ) {
         if ( uri.endsWith( "6" ) ) {
            return 1;
         } else if ( uri.endsWith( "7" ) ) {
            return 2;
         } else if ( uri.endsWith( "8" )
                     || uri.endsWith( "9" )
                     || uri.endsWith( "10" ) ) {
            return 3;
         } else {
            return -1;
         }
      } else if ( uri.equals( "Grade_1" )
                  || uri.equals( "Low_Grade" )
                  || uri.equals( "Low_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Well_Differentiated" ) ) {
         return 1;
      } else if ( uri.equals( "Grade_2" )
                  || uri.equals( "Intermediate_Grade" )
                  || uri.equals( "Intermediate_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Moderately_Differentiated" ) ) {
         return 2;
      } else if ( uri.equals( "Grade_3" )
                  || uri.equals( "High_Grade" )
                  || uri.equals( "High_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Poorly_Differentiated" ) ) {
         return 3;
      } else if ( uri.equals( "Grade_4" )
                  || uri.equals( "Undifferentiated" ) ) {
         // todo add "anaplastic"
//            LOGGER.info( "Have an Undifferentiated, adding its Grade Equivalent (4) to possible ICDO Grades." );
         return 4;
      } else if ( uri.equals( "Grade_5" ) ) {
         return 5;
      }
      return -1;
   }

   static private NeoplasmAttribute createDefaultGrade( final List<Mention> allPatientGrades ) {
      return SpecificAttribute.createAttribute( "grade",
                                                "9",
                                                Collections.emptyList(),
                                                Collections.emptyList(),
                                                allPatientGrades,
                                                Arrays.asList( 0,0,0,0,0 ) );
   }




   static private Map<String,Integer> mapGradeUriCounts( final Map<Integer,List<ConceptAggregate>> gradeCodeMap ) {
      final List<Integer> gradeCodes = gradeCodeMap.keySet()
                                                   .stream()
                                                   .sorted()
                                                   .collect( Collectors.toList() );
      final Map<String,Integer> uriCounts = new HashMap<>();
      for ( int code : gradeCodes ) {
         final Map<String,List<ConceptAggregate>> uriConceptMap = gradeCodeMap
               .get( code )
               .stream()
               .collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
         uriConceptMap.forEach( (k,v) -> uriCounts.put( k, uriCounts.getOrDefault( k, 0 ) + v.size() ) );
      }
      return uriCounts;
   }


}


