package org.healthnlp.deepphe.summary.attribute.cr.stage;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class StageInfoCollector extends AbstractAttributeInfoCollector {

   static private final int STAGE_WINDOW = 16;

   public Collection<String> getRelationTypes() {
      return Collections.singletonList( RelationConstants.HAS_STAGE );
   }

   // Is this necessary?
   public Collection<CrConceptAggregate> getBestAggregates() {
      final Collection<CrConceptAggregate> statedStages = new HashSet<>();
      for ( CrConceptAggregate aggregate : super.getBestAggregates() ) {
         for ( Mention mention : aggregate.getMentions() ) {
            final int mentionBegin = mention.getBegin();
            if ( mentionBegin <= STAGE_WINDOW ) {
               continue;
            }
            final Note note = NoteNodeStore.getInstance()
                                           .get( mention.getNoteId() );
            if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
               continue;
            }
            final String preText = note.getText()
                                       .substring( mentionBegin - STAGE_WINDOW, mentionBegin )
                                       .toLowerCase();
            if ( preText.contains( "figo stage:" ) ) {
               NeoplasmSummaryCreator.addDebug( "Trimming to stage candidate "
                                                + aggregate.getCoveredText() + "\n" );
               statedStages.add( aggregate );
               break;
            }
         }
      }
      if ( !statedStages.isEmpty() ) {
         return statedStages;
      }
      return super.getBestAggregates();
   }


}
