package org.healthnlp.deepphe.summary.attribute.cr;

import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeNormalizer;
import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This takes the place of [Cr]DefaultAttribute
 *
 * @author SPF , chip-nlp
 * @since {3/22/2023}
 */
public class CrDefaultAttributeNew<C extends AttributeInfoCollector,
      N extends AttributeNormalizer> implements CrSpecificAttribute {

   private final C _attributeInfoCollector;
   private final N _attributeNormalizer;

   final private NeoplasmAttribute _neoplasmAttribute;

   public CrDefaultAttributeNew( final String name,
                                 final CrConceptAggregate neoplasm,
                                 final Supplier<C> attributeInfoCollector,
                                 final Supplier<N> attributeNormalizer,
                                 final Map<String,String> dependencies ) {
      this( name, neoplasm, attributeInfoCollector.get(), attributeNormalizer.get(), dependencies );
   }

   public CrDefaultAttributeNew( final String name,
                                 final CrConceptAggregate neoplasm,
                                 final C attributeInfoCollector,
                                 final N attributeNormalizer,
                                 final Map<String,String> dependencies ) {
      _attributeInfoCollector = attributeInfoCollector;
      _attributeInfoCollector.init( neoplasm );
      _attributeNormalizer = attributeNormalizer;
      _attributeNormalizer.init( _attributeInfoCollector, dependencies );

      _neoplasmAttribute = createNeoplasmAttribute( name );
   }

   protected NeoplasmAttribute createNeoplasmAttribute( final String name ) {
      NeoplasmSummaryCreator.addDebug( "#####  " + name + "  #####\n" );
      // TODO - Next URI and Next Code
      return CrSpecificAttribute.createAttribute( name,
                                                  getBestCode(),
                                                  getBestUri(),
                                                  getConfidence(),
                                                  _attributeNormalizer.getDirectEvidence(),
                                                  _attributeNormalizer.getIndirectEvidence(),
                                                  _attributeNormalizer.getNotEvidence(),
                                                  Collections.emptyList() );
//                                                  _featureCreator.getFeatures() );
   }

   public Collection<String> getBestUris() {
      return new ConfidenceGroup<>( _attributeInfoCollector.getAllAggregates() ).getBest()
                                    .stream()
                               .map( CrConceptAggregate::getUri )
                               .collect( Collectors.toSet() );
   }

   public String getBestUri() {
      return String.join( "_", getBestUris() );
   }

   public String getBestCode() {
      return _attributeNormalizer.getBestCode();
   }

   public double getConfidence() {
      return _attributeNormalizer.getConfidence();
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _neoplasmAttribute;
   }


}
