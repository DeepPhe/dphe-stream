package org.healthnlp.deepphe.summary.attribute.cr.histology;

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
 * @since {3/24/2023}
 */
public class HistologyInfoCollector extends AbstractAttributeInfoCollector {

   static private final int HISTOLOGY_WINDOW = 30;


   public Collection<String> getRelationTypes() {
      return Arrays.asList( RelationConstants.HAS_DIAGNOSIS, RelationConstants.HAS_TUMOR_EXTENT );
   }

   // Is this necessary? --> move to normalizer and adjust confidence per aggregate?
   public Collection<CrConceptAggregate> getBestAggregates() {
      final Collection<CrConceptAggregate> histologies = new HashSet<>( super.getBestAggregates() );
      histologies.add( getNeoplasm() );
      final Collection<CrConceptAggregate> statedHistologies = new HashSet<>();
      for ( CrConceptAggregate aggregate : histologies ) {
         for ( Mention mention : aggregate.getMentions() ) {
            final int mentionBegin = mention.getBegin();
            if ( mentionBegin <= HISTOLOGY_WINDOW ) {
               continue;
            }
            final Note note = NoteNodeStore.getInstance()
                                           .get( mention.getNoteId() );
            if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
               continue;
            }
            final String preText = note.getText()
                                       .substring( mentionBegin - HISTOLOGY_WINDOW, mentionBegin )
                                       .toLowerCase();
            if ( preText.contains( "histologic type:" )
                 || preText.contains( "diagnosis:" )
                 || preText.contains( "consistent with" ) ) {
               NeoplasmSummaryCreator.addDebug( "Trimming to histology candidate "
                                                + aggregate.getCoveredText() + "\n" );
               statedHistologies.add( aggregate );
               break;
            }
         }
      }
      if ( !statedHistologies.isEmpty() ) {
         return statedHistologies;
      }
      return histologies;
   }


}
