package org.healthnlp.deepphe.summary.engine;


import org.apache.log4j.Logger;
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
import org.healthnlp.deepphe.summary.attribute.topography.Topography;
import org.healthnlp.deepphe.summary.attribute.topography.minor.TopoMinorCodeInfoStore;
import org.healthnlp.deepphe.summary.attribute.topography.minor.TopoMinorUriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;



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
                                                        final Collection<ConceptAggregate> allConcepts ) {
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
      final String lateralityCode = addLaterality( neoplasm, summary, attributes, allConcepts, patientNeoplasms, topoCode );
      addTopoMinor( neoplasm, summary, attributes, allConcepts, patientNeoplasms, topoCode, lateralityCode );
      addMorphology( neoplasm, summary, attributes, allConcepts, patientNeoplasms, topoCode );
      addBehavior( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addGrade( neoplasm, summary, attributes, allConcepts, patientNeoplasms );

      summary.setPathologic_t( getT( neoplasm ) );
      summary.setPathologic_n( getN( neoplasm ) );
      summary.setPathologic_m( getM( neoplasm ) );

      addBiomarkers( neoplasm, summary, attributes, allConcepts, patientNeoplasms );

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

      // TODO as NeoplasmAttribute

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
      dependencies.put( "laterality", lateralityCode );
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

   static private void addMorphology( final ConceptAggregate neoplasm,
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
                                      final Collection<ConceptAggregate> patientNeoplasms ) {
      final DefaultAttribute<BehaviorUriInfoVisitor, BehaviorCodeInfoStore> behavior
            = new DefaultAttribute<>( "behavior",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      BehaviorUriInfoVisitor::new,
                                      BehaviorCodeInfoStore::new,
                                      Collections.emptyMap() );
      attributes.add( behavior.toNeoplasmAttribute() );
   }


   static private String addLaterality( final ConceptAggregate neoplasm,
                                 final NeoplasmSummary summary,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                      final Collection<ConceptAggregate> patientNeoplasms,
                                        final String topographyMajor ) {
      final Map<String,String> dependencies = new HashMap<>( 1 );
      dependencies.put( "topography_major", topographyMajor );
      final DefaultAttribute<LateralUriInfoVisitor, LateralityCodeInfoStore> laterality
            = new DefaultAttribute<>( "laterality",
                                      neoplasm,
                                      allConcepts,
                                      patientNeoplasms,
                                      LateralUriInfoVisitor::new,
                                      LateralityCodeInfoStore::new,
                                      dependencies );
      attributes.add( laterality.toNeoplasmAttribute() );
      return laterality.getBestCode();
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




   static private String getT( final ConceptAggregate summary ) {
      final Collection<String> ts = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_T, HAS_PATHOLOGIC_T ) );
      return String.join( ";", getTnmValue( ts, 't', true ) );
   }

   static private String getN( final ConceptAggregate summary ) {
      final Collection<String> ns = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_N, HAS_PATHOLOGIC_N ) );
      return String.join( ";", getTnmValue( ns, 'n', true ) );
   }

   static private String getM( final ConceptAggregate summary ) {
      final Collection<String> ms = new HashSet<>( summary.getRelatedUris( HAS_CLINICAL_M, HAS_PATHOLOGIC_M ) );
      return String.join( ";", getTnmValue( ms, 'm', false ) );
   }

   static private String getTnmValue( final Collection<String> tnms, final char type, final boolean allowX ) {
      final Collection<String> values = new HashSet<>();
      for ( String tnm : tnms ) {
         final String lower = tnm.toLowerCase().replace( "_stage", "" );
         final int typeIndex = lower.indexOf( type );
         if ( typeIndex < 0 || typeIndex >= tnm.length() - 1 ) {
            continue;
         }
         final String value = lower.substring( typeIndex + 1 );
         values.add( value );
      }
      values.remove( "x_category" );
      if ( !allowX ) {
         values.remove( "x" );
         values.remove( "X" );
      }
      return String.join( ";", values );
   }


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
      attributes.add( biomarker.toNeoplasmAttribute() );
   }




}
