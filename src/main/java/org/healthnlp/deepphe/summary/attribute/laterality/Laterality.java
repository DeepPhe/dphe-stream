package org.healthnlp.deepphe.summary.attribute.laterality;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;

import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addBooleanFeatures;

final public class Laterality implements SpecificAttribute {

   static private final Logger LOGGER = Logger.getLogger( "Laterality" );

   final private NeoplasmAttribute _laterality;
   private String _bestLaterality;
   private String _bestLateralityCode;

   public Laterality( final ConceptAggregate neoplasm,
                      final Collection<ConceptAggregate> allConcepts,
                      final Collection<ConceptAggregate> patientNeoplasms ) {
      _laterality = createLateralityAttribute( neoplasm, allConcepts, patientNeoplasms );
   }

   private NeoplasmAttribute createLateralityAttribute( final ConceptAggregate neoplasm,
                                                   final Collection<ConceptAggregate> allConcepts,
                                                   final Collection<ConceptAggregate> patientNeoplasms ) {
      final LateralUriInfoVisitor uriInfoVisitor = new LateralUriInfoVisitor();
      final LateralityInfoStore patientStore = new LateralityInfoStore( patientNeoplasms, uriInfoVisitor );

      final LateralityInfoStore neoplasmStore = new LateralityInfoStore( neoplasm, uriInfoVisitor );

      patientStore._codeInfoStore.init( patientNeoplasms, patientStore._mainUriStore );
      neoplasmStore._codeInfoStore.init( Collections.singletonList( neoplasm ), neoplasmStore._mainUriStore );

      _bestLaterality = neoplasmStore._mainUriStore._bestUri;
      _bestLateralityCode = neoplasmStore._codeInfoStore._bestLateralityCode;

      final List<Integer> features = createFeatures( neoplasm,
                                                     allConcepts,
                                                     neoplasmStore,
                                                     patientStore );

      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( Collections.singletonList( neoplasm ),
                                             neoplasmStore._concepts,
                                             patientStore._concepts );

      return SpecificAttribute.createAttribute( "laterality",
                                                neoplasmStore._codeInfoStore._bestLateralityCode,
                                                evidence,
                                                features );
   }

   public String getBestLaterality() {
      return _bestLaterality;
   }

   public String getBestLateralityCode() {
      return _bestLateralityCode;
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _laterality;
   }

   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                         final Collection<ConceptAggregate> allConcepts,
                                         final AttributeInfoStore neoplasmStore,
                                         final AttributeInfoStore patientStore ) {
      final List<Integer> features = new ArrayList<>();

      neoplasmStore.addGeneralFeatures( features );
      patientStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, patientStore );

      // Todo addLateralityFeatures ... boolean isDefault.
//      addMorphStoreFeatures( features, neoplasmStore );
//      addMorphStoreFeatures( features, patientStore );

//      neoplasmStore.addMorphRatioFeatures( features, patientStore );


      addBooleanFeatures( features,
                          neoplasm.isNegated(),
                          neoplasm.isUncertain(),
                          neoplasm.isGeneric(),
                          neoplasm.isConditional() );

      LOGGER.info( "Features: " + features.size() );
      return features;
   }


}
