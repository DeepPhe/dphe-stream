package org.healthnlp.deepphe.summary.engine;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.summary.attribute.DefaultAttribute;
import org.healthnlp.deepphe.summary.attribute.behavior.BehaviorCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.behavior.BehaviorUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.biomarker.Biomarker;
import org.healthnlp.deepphe.summary.attribute.grade.GradeCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.grade.GradeUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.histology.Histology;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.histology.HistologyUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.laterality.LateralUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.laterality.LateralityCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.stage.StageCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.stage.StageUriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.M_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.N_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.T_UriInfoVisitor;
import org.healthnlp.deepphe.summary.attribute.tnm.TnmCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.topography.Topography;
import org.healthnlp.deepphe.summary.attribute.topography.minor.TopoMinorCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.topography.minor.TopoMinorUriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
import static org.healthnlp.deepphe.neo4j.constant.UriConstants.CLOCKFACE;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
final public class NeoplasmSummaryCreator {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmSummaryCreator" );

   static public final StringBuilder DEBUG_SB = new StringBuilder();


   private NeoplasmSummaryCreator() {}


   static public NeoplasmSummary createNeoplasmSummary( final ConceptAggregate neoplasm,
                                                        final Collection<ConceptAggregate> allConcepts,
                                                        final boolean registryOnly ) {
      DEBUG_SB.append( "=======================================================================\n" )
              .append( neoplasm.getPatientId() )
              .append( "\n" );
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> massNeoplasmUris = UriConstants.getMassNeoplasmUris( graphDb );
      final Predicate<ConceptAggregate> isNeoplasm = c -> c.getAllUris()
                                                           .stream()
                                                           .anyMatch( massNeoplasmUris::contains );
      final Collection<ConceptAggregate> patientNeoplasms
            = allConcepts.stream()
                         .filter( isNeoplasm )
                         .collect( Collectors.toList() );

      final NeoplasmSummary summary = new NeoplasmSummary();
      summary.setId( neoplasm.getUri() + "_" + System.currentTimeMillis() );
      summary.setClassUri( neoplasm.getUri() );
      final List<NeoplasmAttribute> attributes = new ArrayList<>();

      final String topoCode = addTopography( neoplasm, summary, attributes, allConcepts );
      copyWithUriAsValue( attributes, "topography_major", "location" );
      final String lateralityCode = addLateralityCode( neoplasm, summary, attributes, allConcepts, patientNeoplasms, topoCode );
      copyWithUriAsValue( attributes, "laterality_code", "laterality" );
      addTopoMinor( neoplasm, summary, attributes, allConcepts, patientNeoplasms, topoCode, lateralityCode );
      addBrCaClockface( neoplasm, attributes );
      addQuadrant( neoplasm, attributes );
      addHistology( neoplasm, summary, attributes, allConcepts, patientNeoplasms, topoCode );
      copyWithUriAsValue( attributes, "morphology", "diagnosis" );
      addBehavior( neoplasm, summary, attributes, allConcepts, patientNeoplasms, registryOnly );
      // TODO cancer_type
      addTumorType( neoplasm, attributes );
      // TODO topo_morphed
//      createWithValue( "topo_morphed", "generated", "false", attributes );
      // TODO extent
      createWithValue( "historic", "historic",
                       neoplasm.inPatientHistory() ? "historic" : "current", attributes );
      createWithValue( "calcifications", "calcifications",
                       neoplasm.getRelated( HAS_CALCIFICATION ).isEmpty()
                       ? "false" : "true", attributes );
      addGrade( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addStage( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmT( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmN( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addTnmM( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      // TODO tumor_size, tumor_size_procedure
      addBiomarkers( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      // TODO treatment (hasTreatment)

      summary.setAttributes( attributes );
      return summary;
   }

   static private String addTopography( final ConceptAggregate neoplasm,
                                      final NeoplasmSummary summary,
                                      final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts ) {
      final Topography topography = new Topography( neoplasm, allConcepts );
      final NeoplasmAttribute majorTopoAttr = topography.toNeoplasmAttribute();
      attributes.add( majorTopoAttr );

      // TODO as NeoplasmAttribute from DefaultAttribute

      return majorTopoAttr.getValue() + "3";
   }

   static private void addTopoMinor( final ConceptAggregate neoplasm,
                                    final NeoplasmSummary summary,
                                    final List<NeoplasmAttribute> attributes,
                                    final Collection<ConceptAggregate> allConcepts,
                                    final Collection<ConceptAggregate> patientNeoplasms,
                                     final String topographyMajor,
                                     final String lateralityCode ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      dependencies.put( "laterality_code", lateralityCode );
      final DefaultAttribute<TopoMinorUriInfoVisitor, TopoMinorCodeInfoStore> topoMinor
            = new DefaultAttribute<>( "topography_minor",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      TopoMinorUriInfoVisitor::new,
                                      TopoMinorCodeInfoStore::new,
                                      dependencies );
      attributes.add( topoMinor.toNeoplasmAttribute() );
   }

   static private void addBrCaClockface( final ConceptAggregate neoplasm, final List<NeoplasmAttribute> attributes ) {
      final String clockface = neoplasm.getRelated( HAS_CLOCKFACE ).stream()
              .map( ConceptAggregate::getUri )
              .distinct()
            .collect( Collectors.joining( ";" ) );
      if ( !clockface.isEmpty() ) {
         createWithValue( "clockface", CLOCKFACE, clockface, attributes );
      }
   }

   static private Collection<String> QUADRANT_URIS;
   static private void initQuadrantUris() {
      if ( QUADRANT_URIS != null ) {
         return;
      }
      QUADRANT_URIS = Neo4jOntologyConceptUtil.getBranchUris( UriConstants.QUADRANT );
   }

   static private void addQuadrant( final ConceptAggregate neoplasm, final List<NeoplasmAttribute> attributes ) {
      initQuadrantUris();
      final Collection<String> quadrants = new HashSet<>();
      neoplasm.getRelated( HAS_QUADRANT ).stream()
              .map( ConceptAggregate::getUri )
              .forEach( quadrants::add );
      neoplasm.getRelatedSites().stream()
              .map( ConceptAggregate::getUri )
              .filter( QUADRANT_URIS::contains )
              .forEach( quadrants::add );
      if ( !quadrants.isEmpty() ) {
         createWithValue( "quadrant", UriConstants.QUADRANT, String.join( ";", quadrants ), attributes );
      }
   }

   static private void addTumorType( final ConceptAggregate neoplasm, final List<NeoplasmAttribute> attributes ) {
      final String tumorType = neoplasm.getRelated( HAS_TUMOR_TYPE ).stream()
                                       .map( ConceptAggregate::getUri )
                                       .distinct()
                                       .collect( Collectors.joining( ";" ) );
      if ( !tumorType.isEmpty() ) {
         createWithValue( "tumor_type", "tumor_type", tumorType, attributes );
      }
   }


   static private void addHistology( final ConceptAggregate neoplasm,
                                     final NeoplasmSummary summary,
                                     final List<NeoplasmAttribute> attributes,
                                     final Collection<ConceptAggregate> allConcepts,
                                     final Collection<ConceptAggregate> patientNeoplasms,
                                     final String topographyCode ) {
      final DefaultAttribute<HistologyUriInfoVisitor, HistologyCodeInfoStore> histology
            = new Histology( neoplasm,
                             allConcepts,
                             patientNeoplasms );
      attributes.add( histology.toNeoplasmAttribute() );
   }


   static private void addBehavior( final ConceptAggregate neoplasm,
                                      final NeoplasmSummary summary,
                                      final List<NeoplasmAttribute> attributes,
                                      final Collection<ConceptAggregate> allConcepts,
                                      final Collection<ConceptAggregate> patientNeoplasms,
                                    final boolean registryOnly ) {
      final DefaultAttribute<BehaviorUriInfoVisitor, BehaviorCodeInfoStore> behavior
            = new DefaultAttribute<>( "behavior",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      BehaviorUriInfoVisitor::new,
                                      BehaviorCodeInfoStore::new,
                                      Collections.emptyMap() );
      final NeoplasmAttribute attribute = behavior.toNeoplasmAttribute();
      if ( registryOnly && attribute.getValue().equals( "6" ) ) {
         attribute.setValue( "3" );
      }
      attributes.add( attribute );
   }


   static private String addLateralityCode( final ConceptAggregate neoplasm,
                                            final NeoplasmSummary summary,
                                            final List<NeoplasmAttribute> attributes,
                                            final Collection<ConceptAggregate> allConcepts,
                                            final Collection<ConceptAggregate> patientNeoplasms,
                                            final String topographyMajor ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      final DefaultAttribute<LateralUriInfoVisitor, LateralityCodeInfoStore> lateralityCode
            = new DefaultAttribute<>( "laterality_code",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      LateralUriInfoVisitor::new,
                                      LateralityCodeInfoStore::new,
                                      dependencies );
      attributes.add( lateralityCode.toNeoplasmAttribute() );
      return lateralityCode.getBestCode();
   }


   static private void addGrade( final ConceptAggregate neoplasm,
                                        final NeoplasmSummary summary,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<GradeUriInfoVisitor, GradeCodeInfoStore> grade
            = new DefaultAttribute<>( "grade",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      GradeUriInfoVisitor::new,
                                      GradeCodeInfoStore::new,
                                      Collections.emptyMap() );
      attributes.add( grade.toNeoplasmAttribute() );
   }

   static private void addStage( final ConceptAggregate neoplasm,
                                 final NeoplasmSummary summary,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<StageUriInfoVisitor, StageCodeInfoStore> stage
            = new DefaultAttribute<>( "stage",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      StageUriInfoVisitor::new,
                                      StageCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( stage.getBestUri().isEmpty() || stage.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( stage.toNeoplasmAttribute() );
   }

   static private void addTnmT( final ConceptAggregate neoplasm,
                                 final NeoplasmSummary summary,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                 final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<T_UriInfoVisitor, TnmCodeInfoStore> t
            = new DefaultAttribute<>( "t",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      T_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( t.getBestUri().isEmpty() || t.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( t.toNeoplasmAttribute() );
   }

   static private void addTnmN( final ConceptAggregate neoplasm,
                                final NeoplasmSummary summary,
                                final List<NeoplasmAttribute> attributes,
                                final Collection<ConceptAggregate> allConcepts,
                                final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<N_UriInfoVisitor, TnmCodeInfoStore> n
            = new DefaultAttribute<>( "n",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      N_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( n.getBestUri().isEmpty() || n.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( n.toNeoplasmAttribute() );
   }

   static private void addTnmM( final ConceptAggregate neoplasm,
                                final NeoplasmSummary summary,
                                final List<NeoplasmAttribute> attributes,
                                final Collection<ConceptAggregate> allConcepts,
                                final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<M_UriInfoVisitor, TnmCodeInfoStore> m
            = new DefaultAttribute<>( "m",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      M_UriInfoVisitor::new,
                                      TnmCodeInfoStore::new,
                                      Collections.emptyMap() );
      if ( m.getBestUri().isEmpty() || m.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( m.toNeoplasmAttribute() );
   }


//   static private String getT( final ConceptAggregate summary ) {
//      final Collection<String> ts = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_T, HAS_PATHOLOGIC_T ) );
//      return String.join( ";", getTnmValue( ts, 't', true ) );
//   }
//
//   static private String getN( final ConceptAggregate summary ) {
//      final Collection<String> ns = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_N, HAS_PATHOLOGIC_N ) );
//      return String.join( ";", getTnmValue( ns, 'n', true ) );
//   }
//
//   static private String getM( final ConceptAggregate summary ) {
//      final Collection<String> ms = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_M, HAS_PATHOLOGIC_M ) );
//      return String.join( ";", getTnmValue( ms, 'm', false ) );
//   }
//
//   static private String getTnmValue( final Collection<String> tnms, final char type, final boolean allowX ) {
//      final Collection<String> values = new HashSet<>();
//      for ( String tnm : tnms ) {
//         final String lower = tnm.toLowerCase().replace( "_stage", "" );
//         final int typeIndex = lower.indexOf( type );
//         if ( typeIndex < 0 || typeIndex >= tnm.length() - 1 ) {
//            continue;
//         }
//         final String value = lower.substring( typeIndex + 1 );
//         values.add( value );
//      }
//      values.remove( "x_category" );
//      if ( !allowX ) {
//         values.remove( "x" );
//         values.remove( "X" );
//      }
//      return String.join( ";", values );
//   }


   // TODO ER_amount, ER_procedure, PR_amount, PR_procedure, HER2_amount, HER2_procedure (has_method)
   static private final Collection<String> BIOMARKERS = Arrays.asList(
         "ER_", "PR_", "HER2", "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
         "PDL1", "MSI", "KRAS", "PSA", "PSA_EL" );

   static private void addBiomarkers( final ConceptAggregate neoplasm,
                                     final NeoplasmSummary summary,
                                     final List<NeoplasmAttribute> attributes,
                                     final Collection<ConceptAggregate> allConcepts,
                                     final Collection<ConceptAggregate> patientNeoplasms ) {
       BIOMARKERS.forEach( b -> addBiomarker( b, neoplasm, summary, attributes, allConcepts, patientNeoplasms ) );
   }

   static private void addBiomarker( final String biomarkerName,
                                       final ConceptAggregate neoplasm,
                                        final NeoplasmSummary summary,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts,
                                        final Collection<ConceptAggregate> patientNeoplasms ) {
      final Biomarker biomarker  = new Biomarker( biomarkerName,
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms );
      if ( biomarker.getBestUri().isEmpty() || biomarker.getBestCode().isEmpty() ) {
         return;
      }
      attributes.add( biomarker.toNeoplasmAttribute() );
   }


   static private String copyWithUriAsValue( final List<NeoplasmAttribute> attributes,
                                             final String sourceName,
                                             final String targetName ) {
      final NeoplasmAttribute sourceAttribute =
            attributes.stream().filter( a -> a.getName().equals( sourceName ) ).findFirst().orElse( null );
      if ( sourceAttribute == null ) {
         return "";
      }
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      final String uri = sourceAttribute.getClassUri();
      attribute.setName( targetName );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( uri );
      attribute.setConfidence( sourceAttribute.getConfidence() );
      attribute.setConfidenceFeatures( sourceAttribute.getConfidenceFeatures() );
      attribute.setDirectEvidence( sourceAttribute.getDirectEvidence() );
      attribute.setIndirectEvidence( sourceAttribute.getIndirectEvidence() );
      attribute.setNotEvidence( sourceAttribute.getNotEvidence() );
      attributes.add( attribute );
      return uri;
   }

   static private String createWithValue( final String name, final String uri, final String value,
                                          final List<NeoplasmAttribute> attributes ) {
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      attribute.setName( name );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setClassUri( uri );
      attribute.setValue( value );
      attribute.setConfidence( 10 );
      attribute.setConfidenceFeatures( Collections.emptyList() );
      attribute.setDirectEvidence( Collections.emptyList() );
      attribute.setIndirectEvidence( Collections.emptyList() );
      attribute.setNotEvidence( Collections.emptyList() );
      attributes.add( attribute );
      return uri;
   }


}
