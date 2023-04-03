package org.healthnlp.deepphe.summary.attribute.cr.newInfoStore;

import org.healthnlp.deepphe.neo4j.node.Mention;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public interface EvidenceGrouper {

   enum EvidenceLevel {
      DIRECT_EVIDENCE,
      INDIRECT_EVIDENCE,
      NOT_EVIDENCE;
   }

   void init( ConfidenceGrouper confidenceGrouper );

   Collection<Mention> getDirectEvidence();

   Collection<Mention> getIndirectEvidence();

   Collection<Mention> getNotEvidence();

}
