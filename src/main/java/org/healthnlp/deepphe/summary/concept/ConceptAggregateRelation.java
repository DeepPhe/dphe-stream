package org.healthnlp.deepphe.summary.concept;

import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {3/15/2023}
 */
final public class ConceptAggregateRelation implements ConfidenceOwner {

   private final String _type;
   private final CrConceptAggregate _target;
   private final Collection<MentionRelation> _mentionRelations;
   private final double _confidence;

   public ConceptAggregateRelation( final String type,
                                    final CrConceptAggregate target,
                                    final Collection<MentionRelation> mentionRelations ) {
      _type = type;
      _target = target;
      _mentionRelations = mentionRelations;
      NeoplasmSummaryCreator.addDebug( "Relation " + _type + " " + _target.getUri() +"\n");
      _confidence = ConfidenceCalculator.calculateAggregateRelation( mentionRelations );
   }

   public String getType() {
      return _type;
   }

   public CrConceptAggregate getTarget() {
      return _target;
   }

   public Collection<MentionRelation> getMentionRelations() {
      return _mentionRelations;
   }

   /**
    * Use (c1 + c2 + c3 + cN) / (N + 2 * sqrt(N))
    * (1)100 ~ (2)90 ~ (3)80 ~ (4)70 ~ (7)60     (>=33.3)   --> To pass 100 requires 3, 4, 5, 8
    * (2)100 ~ (3)90 ~ (5)80 ~ (9)70 ~ (20)60    (>=41.41)  --> To pass 100 requires 4, 6, 10, 21
    * @return Adjusted average as 'confidence' between 0 and 1
    */
   public double getConfidence() {
      return _confidence;
   }


}
