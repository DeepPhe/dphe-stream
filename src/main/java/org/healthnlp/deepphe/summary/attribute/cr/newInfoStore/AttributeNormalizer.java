package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;


import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * This takes the place of [Cr]CodeInfoStore.
 *
 * @author SPF , chip-nlp
 * @since {3/22/2023}
 */
public interface AttributeNormalizer {

   void init( AttributeInfoCollector infoCollector, Map<String,String> dependencies );

   String getBestCode();

   default String getBestCode( final AttributeInfoCollector infoCollector ) {
      return getBestCode( infoCollector.getBestAggregates() );
   }

   String getBestCode( final Collection<CrConceptAggregate> aggregates );

   default String getCode( final CrConceptAggregate aggregate ) {
      return getCode( aggregate.getUri() );
   }

   String getCode( final String uri );

   default boolean hasBestCode( final CrConceptAggregate aggregate, final String bestCode ) {
      return getCode( aggregate ).equals( bestCode );
   }

   /**
       *
       * @return any absolute offset caused by loss in confidence.  e.g. only 1 of 3 possible normal values is used.
       */
   default double getConfidenceDeficit() {
      return 0;
   }

   /**
    *
    * @return any multiplier caused by loss in confidence.  e.g. only 1 of 3 possible normal values is used.
    */
   default double getConfidenceMultiplier() {
      return 1.0;
   }

   default Collection<Mention> getDirectEvidence() {
      return Collections.emptyList();
   }

   default Collection<Mention> getIndirectEvidence() {
      return Collections.emptyList();
   }

   default Collection<Mention> getNotEvidence() {
      return Collections.emptyList();
   }

}
