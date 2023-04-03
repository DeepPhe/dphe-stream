package org.healthnlp.deepphe.summary.engine;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.summary.attribute.cr.CrDefaultAttributeNew;
import org.healthnlp.deepphe.summary.attribute.cr.behavior.BehaviorInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.behavior.BehaviorNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.biomarker.BiomarkerInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.biomarker.BiomarkerNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.grade.GradeInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.grade.GradeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.histology.HistologyInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.histology.HistologyNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.laterality.LateralityInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.laterality.LateralityNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.stage.StageInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.stage.StageNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.tnm.M_InfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.tnm.N_InfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.tnm.T_InfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.tnm.TnmNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.topo_major.TopoMajorInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.topo_major.TopoMajorNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.topo_minor.TopoMinorTypeSelector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

import java.util.*;
import java.util.function.Supplier;



/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
final public class CrNeoplasmSummaryCreator {

   static private final Logger LOGGER = Logger.getLogger( "CrNeoplasmSummaryCreator" );

   static private final StringBuilder DEBUG_SB = new StringBuilder();


   static private final boolean _debug = false;

   private CrNeoplasmSummaryCreator() {}


   static public void addDebug( final String text ) {
      if ( _debug ) {
         DEBUG_SB.append( text );
      }
   }

   static public String getDebug() {
      return DEBUG_SB.toString();
   }

   static public void resetDebug() {
      DEBUG_SB.setLength( 0 );
   }


   static public NeoplasmSummary createCrNeoplasmSummary( final CrConceptAggregate neoplasm ) {
      addDebug( "=======================================================================\n" +
                neoplasm.getPatientId() +  "\n" );
      final NeoplasmSummary summary = new NeoplasmSummary();
      summary.setId( neoplasm.getUri() + "_" + System.currentTimeMillis() );
      summary.setClassUri( neoplasm.getUri() );
      final List<NeoplasmAttribute> attributes = new ArrayList<>();
      final Map<String,String> dependencies = new HashMap<>();

      attributes.add( getAttribute( "topography_major", neoplasm,
                                    TopoMajorInfoCollector::new, TopoMajorNormalizer::new, dependencies ) );
      attributes.add( getAttribute( "laterality", neoplasm,
                                    LateralityInfoCollector::new, LateralityNormalizer::new, dependencies ) );
      final Supplier<AttributeInfoCollector> infoCollector =
            TopoMinorTypeSelector.getAttributeInfoCollector( dependencies );
      final Supplier<AttributeNormalizer> infoNormalizer =
            TopoMinorTypeSelector.getAttributeNormalizer( dependencies );
      attributes.add( getAttribute( "topography_minor", neoplasm,
                                    infoCollector, infoNormalizer, dependencies ) );
      attributes.add( getAttribute( "histology", neoplasm,
                                    HistologyInfoCollector::new, HistologyNormalizer::new, dependencies ) );
      attributes.add( getAttribute( "behavior", neoplasm,
                                    BehaviorInfoCollector::new, BehaviorNormalizer::new, dependencies ) );
      attributes.add( getAttribute( "grade", neoplasm,
                                    GradeInfoCollector::new, GradeNormalizer::new, dependencies ) );
      attributes.add( getAttribute( "stage", neoplasm,
                                    StageInfoCollector::new, StageNormalizer::new, dependencies ) );
      attributes.add( getAttribute( "t", neoplasm, T_InfoCollector::new, TnmNormalizer::new, dependencies ) );
      attributes.add( getAttribute( "n", neoplasm, N_InfoCollector::new, TnmNormalizer::new, dependencies ) );
      attributes.add( getAttribute( "m", neoplasm, M_InfoCollector::new, TnmNormalizer::new, dependencies ) );

      summary.setAttributes( attributes );
      return summary;
   }

   static private <C extends AttributeInfoCollector,
         N extends AttributeNormalizer> NeoplasmAttribute getAttribute( final String name,
                                                                        final CrConceptAggregate neoplasm,
                                                                        final Supplier<C> attributeInfoCollector,
                                                                        final Supplier<N> attributeNormalizer,
                                                                        final Map<String,String> dependencies ) {
      final CrDefaultAttributeNew<C, N> attribute
            = new CrDefaultAttributeNew<>( name, neoplasm,
                                           attributeInfoCollector, attributeNormalizer, dependencies );
      dependencies.put( name, attribute.getBestCode() );
      return attribute.toNeoplasmAttribute();
   }




   // TODO ER_amount, ER_procedure, PR_amount, PR_procedure, HER2_amount, HER2_procedure (has_method)
   static private final Collection<String> BIOMARKERS = Arrays.asList(
         "ER_", "PR_", "HER2", "KI67", "BRCA1", "BRCA2", "ALK", "EGFR", "BRAF", "ROS1",
         "PDL1", "MSI", "KRAS", "PSA", "PSA_EL" );

   static private NeoplasmAttribute getBiomarker( final String name,
                                                  final CrConceptAggregate neoplasm,
                                                  final Map<String,String> dependencies ) {
      final BiomarkerInfoCollector infoCollector = new BiomarkerInfoCollector();
      infoCollector.setWantedUris( name );
      final CrDefaultAttributeNew<BiomarkerInfoCollector, BiomarkerNormalizer> attribute
            = new CrDefaultAttributeNew<>( name, neoplasm,
                                           infoCollector, new BiomarkerNormalizer(), dependencies );
      dependencies.put( name, attribute.getBestCode() );
      return attribute.toNeoplasmAttribute();
   }


}
