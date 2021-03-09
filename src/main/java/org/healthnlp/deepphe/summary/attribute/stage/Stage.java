package org.healthnlp.deepphe.summary.attribute.stage;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.attribute.infostore.AttributeInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.util.*;

import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addBooleanFeatures;

final public class Stage implements SpecificAttribute {

   static private final Logger LOGGER = Logger.getLogger( "Stage" );

   final private NeoplasmAttribute _stage;
   private String _bestStageUri;
   private String _bestStage;
   private String _bestStageCode;

   public Stage( final ConceptAggregate neoplasm,
                 final Collection<ConceptAggregate> allConcepts,
                 final Collection<ConceptAggregate> patientNeoplasms ) {
      _stage = createStageAttribute( neoplasm, allConcepts, patientNeoplasms );
   }

   private NeoplasmAttribute createStageAttribute( final ConceptAggregate neoplasm,
                                                   final Collection<ConceptAggregate> allConcepts,
                                                   final Collection<ConceptAggregate> patientNeoplasms ) {
      final StageUriInfoVisitor uriInfoVisitor = new StageUriInfoVisitor();
      final StageInfoStore patientStore = new StageInfoStore( patientNeoplasms, uriInfoVisitor );

      final StageInfoStore neoplasmStore = new StageInfoStore( neoplasm, uriInfoVisitor );

      final StageInfoStore allConceptsStore = new StageInfoStore( allConcepts, uriInfoVisitor );


      _bestStage = neoplasmStore._mainUriStore._bestUri;
      _bestStageCode = neoplasmStore.getBestCode();

      final List<Integer> features = createFeatures( neoplasm,
                                                     allConcepts,
                                                     neoplasmStore,
                                                     patientStore,
                                                     allConceptsStore );

      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( Collections.singletonList( neoplasm ),
                                             patientStore._concepts,
                                             allConceptsStore._concepts );

      return SpecificAttribute.createAttribute( "stage",
                                                neoplasmStore.getBestCode(),
                                                evidence,
                                                features );
   }

   public String getBestStage() {
      return _bestStage;
   }

   public String getBestStageCode() {
      return _bestStageCode;
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _stage;
   }

   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                         final Collection<ConceptAggregate> allConcepts,
                                         final AttributeInfoStore neoplasmStore,
                                         final AttributeInfoStore patientStore,
                                         final AttributeInfoStore allConceptStore ) {
      final List<Integer> features = new ArrayList<>();

      neoplasmStore.addGeneralFeatures( features );
      patientStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, patientStore );

      allConceptStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, allConceptStore );

      final boolean noStage = neoplasmStore._mainUriStore._bestUri.isEmpty();
      addBooleanFeatures( features,
                          noStage,
                          noStage && !patientStore._mainUriStore._bestUri.isEmpty(),
                          noStage && !allConceptStore._mainUriStore._bestUri.isEmpty(),
                          neoplasmStore._mainUriStore._bestUri.equals( patientStore._mainUriStore._bestUri ),
                          neoplasmStore._mainUriStore._bestUri.equals( allConceptStore._mainUriStore._bestUri ) );

      addBooleanFeatures( features,
                          neoplasm.isNegated(),
                          neoplasm.isUncertain(),
                          neoplasm.isGeneric(),
                          neoplasm.isConditional() );
      return features;
   }


}
