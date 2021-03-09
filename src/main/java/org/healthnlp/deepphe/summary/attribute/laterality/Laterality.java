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

      final LateralityInfoStore allConceptsStore = new LateralityInfoStore( allConcepts, uriInfoVisitor );


      _bestLaterality = neoplasmStore._mainUriStore._bestUri;
      _bestLateralityCode = neoplasmStore.getBestCode();

      final List<Integer> features = createFeatures( neoplasm,
                                                     allConcepts,
                                                     neoplasmStore,
                                                     patientStore,
                                                     allConceptsStore );

      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( Collections.singletonList( neoplasm ),
                                             patientStore._concepts,
                                             allConceptsStore._concepts );

      return SpecificAttribute.createAttribute( "laterality",
                                                neoplasmStore.getBestCode(),
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
                                         final AttributeInfoStore patientStore,
                                         final AttributeInfoStore allConceptStore ) {
      final List<Integer> features = new ArrayList<>();

      neoplasmStore.addGeneralFeatures( features );
      patientStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, patientStore );

      allConceptStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, allConceptStore );

      final boolean noLaterality = neoplasmStore._mainUriStore._bestUri.isEmpty();
      addBooleanFeatures( features,
                          noLaterality,
                          noLaterality && !patientStore._mainUriStore._bestUri.isEmpty(),
                          noLaterality && !allConceptStore._mainUriStore._bestUri.isEmpty(),
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
