package org.healthnlp.deepphe.summary.attribute.cr.grade;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;


/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class GradeInfoCollector extends AbstractAttributeInfoCollector {

   static private final int GRADE_WINDOW = 25;

   public Collection<String> getRelationTypes() {
      return Arrays.asList( RelationConstants.HAS_GRADE, RelationConstants.HAS_GLEASON_SCORE );
   }

   // Is this necessary?
   public Collection<CrConceptAggregate> getBestAggregates() {
      final Collection<CrConceptAggregate> statedGrades = new HashSet<>();
      for ( CrConceptAggregate aggregate : super.getBestAggregates() ) {
         for ( Mention mention : aggregate.getMentions() ) {
            final int mentionBegin = mention.getBegin();
            if ( mentionBegin <= GRADE_WINDOW ) {
               continue;
            }
            final Note note = NoteNodeStore.getInstance()
                                           .get( mention.getNoteId() );
            if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
               continue;
            }
            final String preText = note.getText()
                                       .substring( mentionBegin - GRADE_WINDOW, mentionBegin )
                                       .toLowerCase();
            if ( preText.contains( "histologic grade:" ) ) {
               NeoplasmSummaryCreator.addDebug( "Trimming to grade candidate "
                                                + aggregate.getCoveredText() + "\n" );
               statedGrades.add( aggregate );
               break;
            }
         }
      }
      if ( !statedGrades.isEmpty() ) {
         return statedGrades;
      }
      return super.getBestAggregates();
   }


}
