package org.healthnlp.deepphe.nlp.uri;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/12/2023}
 */
public enum CustomUriRelations {
   INSTANCE;
   static public CustomUriRelations getInstance() {
      return INSTANCE;
   }

   private final Map<String, Collection<String>> NEOPLASM_RELATIONS = new HashMap<>();
   public Map<String,Collection<String>> getNeoplasmRelations( final String uri,
                                                                final GraphDatabaseService graphDb ) {
      if ( !UriConstants.getMassNeoplasmUris( graphDb ).contains( uri ) ) {
         return Collections.emptyMap();
      }
      if ( !NEOPLASM_RELATIONS.isEmpty() ) {
         return NEOPLASM_RELATIONS;
      }
      NEOPLASM_RELATIONS.putAll( CLOCKFACE_RELATIONS );
      NEOPLASM_RELATIONS.put( RelationConstants.HAS_QUADRANT, getQuadrantUris() );
      NEOPLASM_RELATIONS.putAll( getTnmRelations() );
      NEOPLASM_RELATIONS.put( RelationConstants.HAS_STAGE, getStageUris() );
      NEOPLASM_RELATIONS.put( RelationConstants.HAS_GRADE, getGradeUris() );
      NEOPLASM_RELATIONS.put( RelationConstants.HAS_BEHAVIOR, getBehaviorUris() );
//         HAS_ER_STATUS.equals( category ) || HAS_PR_STATUS.equals( category ) ||
//                     HAS_HER2_STATUS.equals( category )
//
      return NEOPLASM_RELATIONS;
   }

   static private final Map<String,Collection<String>> CLOCKFACE_RELATIONS = new HashMap<>( 6 );
   static {
      CLOCKFACE_RELATIONS.put( RelationConstants.HAS_CLOCKFACE, Collections.singletonList( UriConstants.CLOCKFACE ) );
   }

   private final Collection<String> QUADRANT_URIS = new HashSet<>();
   public Collection<String> getQuadrantUris() {
      if ( !QUADRANT_URIS.isEmpty() ) {
         return QUADRANT_URIS;
      }
      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT ) );
      QUADRANT_URIS.remove( UriConstants.QUADRANT );
      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Nipple" ) );
      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Areola" ) );
      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Central_Portion_Of_The_Breast" ) );
      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Subareolar_Region" ) );
      QUADRANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Axillary_Tail_Of_The_Breast" ) );
      return QUADRANT_URIS;
   }


   private final Map<String,Collection<String>> TNM_RELATIONS = new HashMap<>( 6 );
   public Map<String,Collection<String>> getTnmRelations() {
      if ( !TNM_RELATIONS.isEmpty() ) {
         return TNM_RELATIONS;
      }
      TNM_RELATIONS.put( RelationConstants.HAS_CLINICAL_T, getTnmBranch( "Generic_T" ) );
      TNM_RELATIONS.put( RelationConstants.HAS_CLINICAL_N, getTnmBranch( "Generic_N" ) );
      TNM_RELATIONS.put( RelationConstants.HAS_CLINICAL_M, getTnmBranch( "Generic_M" ) );
      TNM_RELATIONS.put( RelationConstants.HAS_PATHOLOGIC_T, getTnmBranch( "Pathologic_T" ) );
      TNM_RELATIONS.put( RelationConstants.HAS_PATHOLOGIC_N, getTnmBranch( "Pathologic_N" ) );
      TNM_RELATIONS.put( RelationConstants.HAS_PATHOLOGIC_M, getTnmBranch( "Pathologic_M" ) );
      return TNM_RELATIONS;
   }

   static private Collection<String> getTnmBranch( final String tnm_uri ) {
      final Collection<String> uris = Neo4jOntologyConceptUtil.getBranchUris( tnm_uri );
      final Collection<String> removals = uris.stream()
                                              .filter( u -> u.contains( "Category" ) )
                                              .collect( Collectors.toSet() );
      uris.removeAll( removals );
      uris.remove( tnm_uri );
      return uris;
   }

   private final Collection<String> STAGE_URIS = new HashSet<>();
   public Collection<String> getStageUris() {
      if ( !STAGE_URIS.isEmpty() ) {
         return STAGE_URIS;
      }
      // In Situ is also stage 0
      STAGE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "In_Situ" ) );
      STAGE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Stage_1" ) );
      STAGE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Stage_2" ) );
      STAGE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Stage_3" ) );
      STAGE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Stage_4" ) );
      // Metastatic is also stage 4
      STAGE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Metastatic" ) );
      STAGE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Stage_5" ) );
      return STAGE_URIS;
   }

   private final Collection<String> GRADE_URIS = new HashSet<>();
   public Collection<String> getGradeUris() {
      if ( !GRADE_URIS.isEmpty() ) {
         return GRADE_URIS;
      }
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Grade_X" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Grade_1" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Well_Differentiated" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Low_Grade" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Grade_2" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Moderately_Differentiated" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Intermediate_Grade" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Grade_3" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Poorly_Differentiated" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "High_Grade" ) );
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Grade_4" ) );
      // Anaplastic is Undifferentiated
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Anaplastic" ) );
      // 5 is on a different grade scale
      GRADE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Grade_5" ) );
      return GRADE_URIS;
   }

   private final Collection<String> BEHAVIOR_URIS = new HashSet<>();
   public Collection<String> getBehaviorUris() {
      if ( !BEHAVIOR_URIS.isEmpty() ) {
         return BEHAVIOR_URIS;
      }
      // https://training.seer.cancer.gov/coding/guidelines/morphology.html   0, 1, 2, 3, 6 below.
      // Registries do not use #6 (Invasive), so cast to 3.
      BEHAVIOR_URIS.addAll( Arrays.asList( "Benign",
                                           "Borderline", "Non_Malignant",
                                           "In_Situ", "Noninvasive", "Premalignant",
                                           "Malignant_Descriptor",
                                           "Invasive", "Metastatic" //"Locally_Metastatic", "Distantly_Metastatic",
                                           // Carcinoma, Adenocarcinoma aren't modifiers, but indicate malignancy.
//                                           "Carcinoma", "Adenocarcinoma"
                                         ) );
      return BEHAVIOR_URIS;
   }


}
