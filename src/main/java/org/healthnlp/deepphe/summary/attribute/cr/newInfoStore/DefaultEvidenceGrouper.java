package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.summary.concept.ConfidenceOwner;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;

import java.util.Collection;
import java.util.EnumMap;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class DefaultEvidenceGrouper implements EvidenceGrouper {

   private EnumMap<EvidenceLevel, Collection<Mention>> _evidenceMap;

   public void init( final ConfidenceGrouper confidenceGrouper ) {
      _evidenceMap = new EnumMap<>( EvidenceLevel.class );
      _evidenceMap.put( EvidenceLevel.DIRECT_EVIDENCE,
                        getMentions( confidenceGrouper.getConfidenceGroup().getBest() ) );
      _evidenceMap.put( EvidenceLevel.INDIRECT_EVIDENCE,
                        getMentions( confidenceGrouper.getConfidenceGroup().getNext() ) );
      _evidenceMap.put( EvidenceLevel.NOT_EVIDENCE,
                        getMentions( confidenceGrouper.getConfidenceGroup().getOther() ) );
   }

   private  Collection<Mention> getMentions( final Collection<ConfidenceOwner> aggregates ) {
      return aggregates.stream()
                               .filter( c -> c instanceof CrConceptAggregate )
                               .map( c -> (CrConceptAggregate)c  )
                               .map( CrConceptAggregate::getMentions )
                               .flatMap( Collection::stream )
                               .collect( Collectors.toSet() );
   }

   public Collection<Mention> getDirectEvidence() {
      return _evidenceMap.get( EvidenceLevel.DIRECT_EVIDENCE );
   }

   public Collection<Mention> getIndirectEvidence() {
      return _evidenceMap.get( EvidenceLevel.INDIRECT_EVIDENCE );
   }

   public Collection<Mention> getNotEvidence() {
      return _evidenceMap.get( EvidenceLevel.NOT_EVIDENCE );
   }

}
